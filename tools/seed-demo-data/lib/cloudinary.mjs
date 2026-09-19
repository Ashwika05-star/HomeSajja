import { CATEGORIES, furnitureSvg } from './images.mjs';

/** Uploads one SVG through the app's unsigned preset and returns a PNG URL for it (Cloudinary converts on delivery). */
async function upload(config, publicId, svg) {
  const form = new FormData();
  form.set('upload_preset', config.uploadPreset);
  form.set('public_id', publicId);
  form.set('file', new Blob([svg], { type: 'image/svg+xml' }), `${publicId}.svg`);
  const response = await fetch(`https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload`, { method: 'POST', body: form });
  const data = await response.json();
  if (!response.ok) throw new Error(`Cloudinary upload of ${publicId} failed: ${data.error?.message ?? response.status}`);
  // …/upload/v123/<public id>.svg  ->  …/upload/v123/<public id>.png
  return data.secure_url.replace(/\.svg$/, '.png');
}

/** Uploads every category illustration in three colourways (once; re-running reuses what is already there). */
export async function uploadIllustrations(config, log) {
  const urls = {};
  for (const category of CATEGORIES) {
    for (let variant = 0; variant < 3; variant++) {
      const id = `homesajja-demo/${category.toLowerCase()}-${variant}`;
      urls[`${category}:${variant}`] = await upload(config, id, furnitureSvg(category, variant));
    }
    log(`  illustrations: ${category}`);
  }
  return urls;
}
