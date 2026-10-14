// Talks to Firebase over its public REST APIs, signed in as the demo accounts themselves, so every write goes through
// the app's real security rules (nothing here bypasses them).
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');

export function loadConfig({ emulator, dryRun = false }) {
  const services = JSON.parse(readFileSync(join(root, 'app', 'google-services.json'), 'utf8'));
  const strings = readFileSync(join(root, 'app', 'src', 'main', 'res', 'values', 'strings.xml'), 'utf8');
  const stringValue = (name) => new RegExp(`name="${name}"[^>]*>([^<]+)<`).exec(strings)[1].trim();
  const projectId = services.project_info.project_id;
  return {
    emulator,
    dryRun,
    projectId,
    apiKey: services.client[0].api_key[0].current_key,
    cloudName: stringValue('cloudinary_cloud_name'),
    uploadPreset: stringValue('cloudinary_upload_preset'),
    authBase: emulator ? 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1' : 'https://identitytoolkit.googleapis.com/v1',
    dbBase: emulator
      ? `http://127.0.0.1:8080/v1/projects/${projectId}/databases/(default)/documents`
      : `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`,
  };
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function authCall(config, method, body) {
  const response = await fetch(`${config.authBase}/${method}?key=${config.apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const data = await response.json();
  if (!response.ok) throw Object.assign(new Error(data.error?.message ?? `auth ${method} failed`), { code: data.error?.message });
  return data;
}

/** Signs the demo account in, creating it first if it doesn't exist (unless dryRun is set). Returns { uid, token }. */
export async function ensureAccount(config, { email, password, name }) {
  let session;
  if (config.dryRun) {
    session = await authCall(config, 'accounts:signInWithPassword', { email, password, returnSecureToken: true });
  } else {
    try {
      session = await authCall(config, 'accounts:signUp', { email, password, displayName: name, returnSecureToken: true });
    } catch (e) {
      if (!String(e.code).startsWith('EMAIL_EXISTS')) throw e;
      session = await authCall(config, 'accounts:signInWithPassword', { email, password, returnSecureToken: true });
    }
  }
  await sleep(config.emulator ? 0 : 150);
  return { uid: session.localId, token: session.idToken };
}

export async function deleteAccount(config, token) {
  if (config.dryRun) {
    console.log('  [dry-run] would delete auth account');
    return;
  }
  await authCall(config, 'accounts:delete', { idToken: token });
}

// ---- Firestore value encoding ----
export function encode(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (typeof value === 'string') return { stringValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encode) } };
  return { mapValue: { fields: Object.fromEntries(Object.entries(value).map(([k, v]) => [k, encode(v)])) } };
}

const fieldsOf = (object) => Object.fromEntries(Object.entries(object).map(([k, v]) => [k, encode(v)]));

async function dbCall(config, token, method, path, body) {
  const response = await fetch(`${config.dbBase}/${path}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (response.status === 404 && method === 'GET') return null;
  if (response.status === 403 && method === 'GET') return { _forbidden: true };
  const text = await response.text();
  if (!response.ok) throw new Error(`${method} ${path} -> ${response.status} ${text.slice(0, 300)}`);
  return text ? JSON.parse(text) : {};
}

export const docExists = async (config, token, path) => (await dbCall(config, token, 'GET', path)) !== null;

/** The document's `status` text, or null if it doesn't exist. */
export async function docStatus(config, token, path) {
  const doc = await dbCall(config, token, 'GET', path);
  if (!doc) return null;
  if (doc._forbidden) return 'FORBIDDEN';
  return doc.fields?.status?.stringValue ?? '';
}

function summarizeUrls(arr) {
  if (!Array.isArray(arr) || arr.length === 0) return '';
  return ` [${arr.map((u) => String(u).replace(/^https:\/\/res\.cloudinary\.com\/[^/]+\/image\/upload\//, 'cloudinary://')).join(', ')}]`;
}

/** Creates or replaces the document (or logs what would change when `config.dryRun` is true). */
export async function setDoc(config, token, path, data) {
  if (config.dryRun) {
    const existing = await dbCall(config, token, 'GET', path);
    const action = existing ? 'OVERWRITE' : 'CREATE';
    const mediaInfo = summarizeUrls(data.images ?? data.brochureImages);
    console.log(`  [dry-run] ${action} ${path}${data.title ? ` ("${data.title}")` : ''}${mediaInfo}`);
    return {};
  }
  return dbCall(config, token, 'PATCH', path, { fields: fieldsOf(data) });
}

/** Changes only the given fields (or logs what would change when `config.dryRun` is true). */
export async function updateDoc(config, token, path, changes) {
  if (config.dryRun) {
    const keys = Object.keys(changes).join(', ');
    const mediaInfo = summarizeUrls(changes.images ?? changes.brochureImages);
    console.log(`  [dry-run] UPDATE ${path} {${keys}}${mediaInfo}`);
    return {};
  }
  const mask = Object.keys(changes).map((k) => `updateMask.fieldPaths=${encodeURIComponent(k)}`).join('&');
  return dbCall(config, token, 'PATCH', `${path}?${mask}`, { fields: fieldsOf(changes) });
}

export async function deleteDoc(config, token, path) {
  if (config.dryRun) {
    console.log(`  [dry-run] DELETE ${path}`);
    return {};
  }
  return dbCall(config, token, 'DELETE', path);
}

/** Structured query: documents of `collection` where `field == value`. Returns the document ids. */
export async function idsWhere(config, token, collection, field, value) {
  const response = await fetch(`${config.dbBase}:runQuery`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: collection }],
        where: { fieldFilter: { field: { fieldPath: field }, op: 'EQUAL', value: encode(value) } },
      },
    }),
  });
  const rows = await response.json();
  if (!response.ok) throw new Error(`query ${collection}.${field} failed: ${JSON.stringify(rows).slice(0, 200)}`);
  return rows.filter((r) => r.document).map((r) => r.document.name.split('/').pop());
}
