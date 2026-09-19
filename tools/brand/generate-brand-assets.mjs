#!/usr/bin/env node
// Draws the HomeSajja icon and the Play Store graphics as SVG, has Cloudinary turn them into PNGs (no image tools needed),
// and saves them where the app and play-store/ expect them.   Run: node tools/brand/generate-brand-assets.mjs
import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { loadConfig } from '../seed-demo-data/lib/firebase.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const config = loadConfig({ emulator: false });
const CABERNET = '#6D1F2B';
const CREAM = '#F3DCE0';

// The mark, drawn on a 108 x 108 canvas (the same numbers as res/drawable/ic_launcher_foreground.xml):
// a house with an armchair inside it.
const mark = (houseColor, chairColor) => `
  <path fill="${houseColor}" d="M54,24 L86,50 L79,50 L79,84 L29,84 L29,50 L22,50 Z"/>
  <rect x="45" y="54" width="18" height="12" rx="4" fill="${chairColor}"/>
  <rect x="41" y="65" width="26" height="8" rx="3" fill="${chairColor}"/>
  <rect x="44" y="72" width="4" height="8" fill="${chairColor}"/>
  <rect x="60" y="72" width="4" height="8" fill="${chairColor}"/>`;

const iconSvg = (size, shape) => `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 108 108">
  <rect width="108" height="108" rx="${shape === 'round' ? 54 : 0}" fill="${CABERNET}"/>${mark(CREAM, CABERNET)}</svg>`;

const featureSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
  <rect width="1024" height="500" fill="${CABERNET}"/>
  <circle cx="900" cy="80" r="220" fill="#7C2C39"/>
  <circle cx="110" cy="470" r="180" fill="#7C2C39"/>
  <g transform="translate(60,80) scale(3.4)">${mark(CREAM, CABERNET)}</g>
  <text x="470" y="235" font-family="Georgia, 'Times New Roman', serif" font-size="96" font-weight="bold" fill="${CREAM}">HomeSajja</text>
  <text x="474" y="300" font-family="Arial, Helvetica, sans-serif" font-size="34" fill="#E8D4C8">Give furniture a second life</text>
  <text x="474" y="360" font-family="Arial, Helvetica, sans-serif" font-size="28" fill="#E29AA8">Buy · Sell · Exchange · Repair · Recycle</text>
</svg>`;

async function render(publicId, svg, transformation) {
  const form = new FormData();
  form.set('upload_preset', config.uploadPreset);
  form.set('public_id', `homesajja-brand/${publicId}`);
  form.set('file', new Blob([svg], { type: 'image/svg+xml' }), `${publicId}.svg`);
  const response = await fetch(`https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload`, { method: 'POST', body: form });
  const data = await response.json();
  if (!response.ok) throw new Error(`upload ${publicId}: ${data.error?.message}`);
  const url = data.secure_url.replace('/upload/', `/upload/${transformation}/`).replace(/\.svg$/, '.png');
  const png = await fetch(url);
  if (!png.ok) throw new Error(`download ${publicId}: ${png.status}`);
  return Buffer.from(await png.arrayBuffer());
}

const save = (path, buffer) => { mkdirSync(dirname(path), { recursive: true }); writeFileSync(path, buffer); console.log('wrote', path.replace(root + '/', '')); };

save(join(root, 'play-store', 'icon-512.png'), await render('icon-square', iconSvg(512, 'square'), 'w_512,h_512,c_fill'));
save(join(root, 'play-store', 'feature-graphic-1024x500.png'), await render('feature', featureSvg, 'w_1024,h_500,c_fill'));

// Legacy launcher icons for Android 7.x (API 24-25); newer phones use the adaptive icon in res/mipmap-anydpi-v26.
const densities = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [density, px] of Object.entries(densities)) {
  const dir = join(root, 'app', 'src', 'main', 'res', `mipmap-${density}`);
  save(join(dir, 'ic_launcher.png'), await render('icon-square', iconSvg(512, 'square'), `w_${px},h_${px},c_fill`));
  save(join(dir, 'ic_launcher_round.png'), await render('icon-round', iconSvg(512, 'round'), `w_${px},h_${px},c_fill`));
}
