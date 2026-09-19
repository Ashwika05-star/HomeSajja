// Simple flat furniture illustrations, drawn as SVG, so demo listings have pictures without using anyone's photos.
// Cloudinary turns each SVG into a PNG when asked for one (see cloudinary.mjs).

const WOOD = [
  { wood: '#8A5A3B', dark: '#5E3B25', light: '#B98356' }, // teak
  { wood: '#A8724A', dark: '#734A2E', light: '#D19B6F' }, // sheesham
  { wood: '#6B4A3A', dark: '#452F25', light: '#946B55' }, // walnut
];
const FABRIC = ['#8C3B46', '#6C7A5B', '#3F5A6B'];

const room = (inner) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600">
  <rect width="800" height="600" fill="#F2E4D8"/>
  <rect y="430" width="800" height="170" fill="#E2CDBD"/>
  <ellipse cx="400" cy="470" rx="290" ry="26" fill="#000" opacity="0.08"/>
  ${inner}</svg>`;

const rect = (x, y, w, h, fill, r = 8) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}"/>`;

const shapes = {
  SOFA: (c, f) => rect(140, 250, 520, 170, f) + rect(110, 290, 70, 130, f, 22) + rect(620, 290, 70, 130, f, 22) +
    rect(190, 300, 200, 90, c.light, 14) + rect(410, 300, 200, 90, c.light, 14) + rect(170, 420, 24, 30, c.dark) + rect(606, 420, 24, 30, c.dark),
  BED: (c, f) => rect(130, 190, 540, 150, c.dark) + rect(150, 320, 500, 90, f) + rect(170, 290, 190, 46, '#FBF3EA', 18) + rect(440, 290, 190, 46, '#FBF3EA', 18) +
    rect(130, 400, 30, 50, c.dark) + rect(640, 400, 30, 50, c.dark),
  TABLE: (c) => rect(150, 290, 500, 34, c.wood) + rect(190, 324, 26, 130, c.dark) + rect(584, 324, 26, 130, c.dark) + rect(270, 324, 260, 16, c.dark),
  CHAIR: (c, f) => rect(300, 190, 200, 130, c.wood, 14) + rect(290, 320, 220, 44, f, 12) + rect(305, 364, 22, 92, c.dark) + rect(473, 364, 22, 92, c.dark),
  DESK: (c) => rect(130, 280, 540, 30, c.wood) + rect(150, 310, 26, 140, c.dark) + rect(624, 310, 26, 140, c.dark) + rect(440, 310, 170, 100, c.light, 6) + rect(480, 350, 90, 12, c.dark, 4),
  WARDROBE: (c) => rect(230, 120, 340, 330, c.wood, 10) + rect(240, 130, 150, 310, c.light, 6) + rect(410, 130, 150, 310, c.light, 6) + rect(368, 260, 10, 60, c.dark, 4) + rect(422, 260, 10, 60, c.dark, 4),
  STORAGE: (c) => rect(200, 220, 400, 230, c.wood, 8) + rect(215, 235, 370, 95, c.light, 6) + rect(215, 340, 370, 95, c.light, 6) + rect(385, 275, 30, 10, c.dark, 4) + rect(385, 380, 30, 10, c.dark, 4),
  BOOKSHELF: (c, f) => rect(230, 110, 340, 340, c.wood, 6) + rect(244, 124, 312, 96, '#F2E4D8', 4) + rect(244, 232, 312, 96, '#F2E4D8', 4) + rect(244, 340, 312, 96, '#F2E4D8', 4) +
    rect(262, 150, 26, 70, f, 3) + rect(292, 138, 22, 82, c.light, 3) + rect(318, 156, 30, 64, c.dark, 3) + rect(270, 262, 30, 66, c.dark, 3) + rect(306, 250, 24, 78, f, 3) + rect(480, 370, 50, 66, c.light, 3),
  TV_UNIT: (c) => rect(130, 330, 540, 110, c.wood, 8) + rect(145, 345, 160, 80, c.light, 6) + rect(320, 345, 160, 80, c.light, 6) + rect(495, 345, 160, 80, c.light, 6) +
    rect(250, 190, 300, 130, '#2B211E', 8) + rect(260, 200, 280, 110, '#3D3530', 4),
  COFFEE_TABLE: (c) => rect(230, 320, 340, 26, c.light, 12) + rect(250, 346, 20, 108, c.dark) + rect(530, 346, 20, 108, c.dark) + rect(300, 300, 60, 20, '#8C3B46', 6) + rect(400, 296, 40, 24, '#6C7A5B', 8),
  RECLINER: (c, f) => rect(240, 200, 240, 200, f, 34) + rect(210, 300, 90, 150, f, 26) + rect(420, 300, 120, 90, c.light, 22) + rect(230, 440, 26, 20, c.dark) + rect(440, 440, 26, 20, c.dark),
  OTHER: (c, f) => rect(260, 250, 280, 190, c.wood, 12) + rect(280, 270, 240, 60, c.light, 8) + rect(280, 350, 240, 70, c.light, 8) + rect(380, 296, 40, 8, c.dark, 4),
};

export const CATEGORIES = Object.keys(shapes);

/** The SVG for a category; `variant` 0..2 changes the wood and fabric colours. */
export function furnitureSvg(category, variant = 0) {
  const c = WOOD[variant % WOOD.length];
  return room(shapes[category](c, FABRIC[variant % FABRIC.length]));
}
