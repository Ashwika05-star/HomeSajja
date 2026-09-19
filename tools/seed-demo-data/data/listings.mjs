// A pool of realistic second-hand Indian furniture. Each city gets nine of them, from its own sellers, with small price differences.
// t(title, category, material, condition, ageYears, price, [length, width, height] cm, variant, description, options)
const t = (title, category, material, condition, ageYears, price, dims, variant, description, options = {}) =>
  ({ title, category, material, condition, ageYears, price, dims, variant, description, ...options });

export const POOL = [
  t('Teak wood 3-seater sofa with cushions', 'SOFA', 'WOOD', 'GOOD', 6, 18500, [198, 82, 88], 0, 'Solid teak frame with thick foam cushions in maroon fabric. Comfortable, no sagging. Selling because we are moving to a smaller flat.'),
  t('Sheesham 6-seater dining table with 6 chairs', 'TABLE', 'WOOD', 'GOOD', 4, 24000, [180, 90, 76], 1, 'Heavy sheesham dining set with six cushioned chairs. Minor water ring on one corner, otherwise in great condition.'),
  t('King size bed with hydraulic storage', 'BED', 'WOOD', 'LIKE_NEW', 2, 16500, [210, 190, 95], 2, 'Engineered wood king bed with hydraulic storage box. Mattress not included. Assembled once, moved carefully.'),
  t('Ergonomic office chair, mesh back', 'CHAIR', 'PLASTIC', 'GOOD', 2, 4200, [62, 62, 110], 0, 'Adjustable height and lumbar support. Used for work from home for two years. Wheels smooth, gas lift working.'),
  t('Steel almirah, 3 door with locker', 'WARDROBE', 'METAL', 'GOOD', 8, 7800, [90, 50, 190], 0, 'Godrej-type steel almirah with internal locker and hanging rod. A little scratched at the base, no rust.'),
  t('Study table with drawers and shelf', 'DESK', 'WOOD', 'GOOD', 3, 3200, [120, 60, 76], 1, 'Sturdy study desk with two drawers and a small bookshelf on top. Perfect for school or college students.'),
  t('Five-shelf wooden bookshelf', 'BOOKSHELF', 'WOOD', 'FAIR', 7, 2400, [80, 30, 180], 2, 'Tall open bookshelf, holds a lot. Some old marks on the sides. Great value for the size.'),
  t('Cane rocking chair', 'CHAIR', 'WOOD', 'GOOD', 5, 3600, [70, 90, 100], 1, 'Traditional cane and wood rocking chair. Cane seat re-woven last year. Very relaxing on a balcony.'),
  t('TV unit, walnut finish, 5 ft', 'TV_UNIT', 'WOOD', 'LIKE_NEW', 1, 5400, [150, 40, 50], 2, 'Modern TV unit with cable management and three storage cabinets. Bought last year, barely used.'),
  t('Single-seater recliner sofa', 'RECLINER', 'LEATHER', 'GOOD', 3, 9500, [90, 95, 100], 0, 'Manual recliner in brown faux leather. Smooth recline, no tears. Comfortable for long evenings.'),
  t('Marble-top centre table', 'COFFEE_TABLE', 'GLASS', 'GOOD', 5, 6500, [100, 60, 45], 1, 'Round marble-top centre table on a carved wooden base. Heavy and elegant. Small chip on the underside.'),
  t('Wrought iron shoe rack, 4 tier', 'STORAGE', 'METAL', 'GOOD', 2, 1200, [70, 30, 95], 0, 'Slim wrought iron shoe rack that holds about 12 pairs. Fits behind the door.'),
  t('L-shaped fabric sofa, 5 seater', 'SOFA', 'FABRIC', 'LIKE_NEW', 2, 28000, [270, 170, 85], 2, 'Grey L-shaped sofa in washable fabric. Two years old, no stains. Original price was ₹52,000.'),
  t('Wooden swing (jhula) with chains', 'OTHER', 'WOOD', 'GOOD', 9, 12000, [180, 60, 60], 0, 'Traditional teak jhula with brass chains, ideal for a balcony or living room. Buyer collects.'),
  t('Solid wood single bed with box storage', 'BED', 'WOOD', 'GOOD', 4, 8500, [200, 95, 45], 1, 'Single bed with a large storage box below. Sturdy, no creaks. Mattress included on request.'),
  t('Glass-top dining table, 4 seater', 'TABLE', 'GLASS', 'LIKE_NEW', 1, 9800, [120, 75, 76], 2, 'Tempered glass top with a powder-coated black base and four chairs. Very light and easy to clean.'),
  t('Bar cabinet with wine rack', 'STORAGE', 'WOOD', 'GOOD', 4, 11500, [90, 40, 130], 2, 'Compact bar cabinet with glass holders and a wine rack. Dark polish, looks premium in a living room.'),
  t('Kids study table and chair set', 'DESK', 'PLASTIC', 'GOOD', 2, 2200, [90, 55, 60], 0, 'Colourful study set for children aged 5 to 10. Height adjustable. Cleaned and sanitised.'),
  t('Double door wooden wardrobe with mirror', 'WARDROBE', 'WOOD', 'GOOD', 6, 13500, [120, 55, 200], 1, 'Large wardrobe with full-length mirror and a drawer. Solid wood, heavy. Two people needed to move.'),
  t('Wooden dressing table with mirror', 'OTHER', 'WOOD', 'GOOD', 5, 4800, [90, 45, 150], 2, 'Dressing table with a large mirror and two side drawers. Polish still shiny.'),
  t('Cushioned bench with shoe storage', 'STORAGE', 'FABRIC', 'LIKE_NEW', 1, 3500, [100, 40, 45], 0, 'Entryway bench with hidden shoe storage and a comfortable seat. Neutral beige.'),
  t('Steel frame bunk bed', 'BED', 'METAL', 'FAIR', 6, 9000, [200, 95, 165], 0, 'Sturdy bunk bed in black powder coat. Ladder included. A few paint chips, structure is solid.'),
  t('Wall-mounted floating bookshelf set (3)', 'BOOKSHELF', 'WOOD', 'NEW', 0, 1900, [80, 20, 20], 2, 'Set of three floating shelves, brand new in the box. Bought extra by mistake.'),
  t('Vintage teak writing desk', 'DESK', 'WOOD', 'GOOD', 25, 15500, [130, 65, 78], 0, 'Old teak writing desk with brass handles and a leather inlay. A lovely piece with character.'),
  t('Two-seater sofa cum bed', 'SOFA', 'FABRIC', 'GOOD', 3, 11000, [180, 90, 80], 1, 'Converts into a bed in seconds. Blue fabric cover, recently dry cleaned. Ideal for guests.'),
  t('Plastic outdoor chairs, set of 4 with table', 'TABLE', 'PLASTIC', 'GOOD', 2, 2800, [80, 80, 72], 0, 'Weatherproof balcony set. Light and stackable. Colours slightly faded.'),
  t('Carved wooden temple (mandir) unit', 'OTHER', 'WOOD', 'GOOD', 7, 5200, [60, 35, 110], 1, 'Wall-mount wooden mandir with a small drawer and a bell holder. Clean and well kept.'),
  t('Cot with cane weaving (charpai style)', 'BED', 'WOOD', 'FAIR', 8, 2600, [190, 90, 42], 0, 'Traditional charpai-style cot with fresh cane weaving. Lightweight and comfortable in summer.'),
  t('Metal and wood shoe cabinet', 'STORAGE', 'WOOD', 'GOOD', 3, 2900, [80, 30, 100], 1, 'Two-door shoe cabinet with adjustable shelves. Fits about 20 pairs.'),
  t('Bean bag chair, XXL', 'RECLINER', 'FABRIC', 'GOOD', 1, 1500, [90, 90, 70], 2, 'Extra-large bean bag with fresh beans. Cover is washable.'),
];

// Which pool items each city lists, and how much dearer or cheaper each city is.
export const CITY_PRICE_FACTOR = { Mumbai: 1.12, Pune: 0.97, Bengaluru: 1.05, Delhi: 1.0, Hyderabad: 0.95 };
export const CITY_PICKS = {
  Mumbai: [0, 1, 3, 4, 8, 12, 16, 22, 26],
  Pune: [2, 5, 7, 9, 13, 17, 19, 24, 28],
  Bengaluru: [3, 6, 10, 12, 15, 18, 20, 25, 29],
  Delhi: [1, 4, 8, 11, 14, 18, 21, 23, 27],
  Hyderabad: [0, 2, 5, 9, 13, 16, 19, 24, 26],
};
// Indexes (within a city's nine) that are offered for exchange instead of sale.
export const EXCHANGE_SLOTS = new Set([2, 6]);

export const MATERIAL_REQUESTS = [
  { vendor: 'dadarwood', city: 'Mumbai', materialType: 'WOOD', title: 'Old teak wood planks for restoration', quantity: '40 kg', budgetMin: 1500, budgetMax: 4000, description: 'Looking for seasoned teak planks or broken teak furniture we can reuse in repairs. Any thickness above 1 inch.' },
  { vendor: 'greenloopmum', city: 'Mumbai', materialType: 'METAL', title: 'Steel and iron furniture frames', quantity: '100 kg', budgetMin: 800, budgetMax: 2500, description: 'Buying scrap steel almirah bodies, bed frames and iron racks. Fair weighed price, we collect.' },
  { vendor: 'koregaoncarp', city: 'Pune', materialType: 'WOOD', title: 'Sheesham offcuts and old doors', quantity: '10 pieces', budgetMin: 1000, budgetMax: 3000, description: 'Need solid sheesham offcuts or old doors for making small tables and stools.' },
  { vendor: 'banerrefurb', city: 'Pune', materialType: 'FABRIC', title: 'Sofa fabric and upholstery leftovers', quantity: '25 metres', budgetMin: 500, budgetMax: 2000, description: 'Clean upholstery fabric in plain colours for our refurbished cushions.' },
  { vendor: 'whitefieldwood', city: 'Bengaluru', materialType: 'WOOD', title: 'Rosewood or teak table legs', quantity: '12 legs', budgetMin: 600, budgetMax: 2400, description: 'Turned legs in good condition for restoring dining tables and consoles.' },
  { vendor: 'lajpatrepairs', city: 'Delhi', materialType: 'GLASS', title: 'Glass table tops (tempered)', quantity: '5 pieces', budgetMin: 700, budgetMax: 3000, description: 'Round or rectangular tempered glass tops, any size, no cracks.' },
  { vendor: 'kukatpallycarp', city: 'Hyderabad', materialType: 'WOOD', title: 'Mango and neem wood', quantity: '60 kg', budgetMin: 900, budgetMax: 2600, description: 'Seasoned mango or neem wood for making stools and swings.' },
];

// Finished jobs that give vendors real-looking ratings. `who` is the customer, `with` the vendor, `stars` and `comment` the review.
export const COMPLETED_JOBS = [
  { type: 'repair', who: 'aarav', with: 'dadarwood', item: 'Teak dining chair', category: 'CHAIR', problem: 'LOOSE_JOINTS', text: 'Two legs wobble and the seat rattles when I sit.', stars: 5, comment: 'Prakash bhai fixed the chair in two days and it is as good as new. Very fair price too.' },
  { type: 'repair', who: 'priya', with: 'dadarwood', item: 'Sheesham coffee table', category: 'COFFEE_TABLE', problem: 'SCRATCHES', text: 'Deep scratches on the top from the kids and a faded polish.', stars: 4, comment: 'Good polish work. Took a day longer than promised but worth the wait.' },
  { type: 'recycle', who: 'rohan', with: 'greenloopmum', material: 'METAL', condition: 'BEYOND_REPAIR', stars: 5, comment: 'They picked up my old rusted almirah on time and the team was very polite.' },
  { type: 'purchase', who: 'priya', with: 'andherimart', stars: 4, comment: 'Sofa was exactly as described. Delivery was quick and the price was fair.' },
  { type: 'repair', who: 'sneha', with: 'koregaoncarp', item: 'Study desk', category: 'DESK', problem: 'BROKEN_LEG', text: 'One leg cracked while moving houses.', stars: 5, comment: 'Sanjay replaced the leg and matched the wood perfectly. Excellent carpenter.' },
  { type: 'repair', who: 'vikram', with: 'koregaoncarp', item: 'Bookshelf', category: 'BOOKSHELF', problem: 'LOOSE_JOINTS', text: 'Shelf brackets are loose and the frame leans.', stars: 4, comment: 'Solid fix, tidy work. Would call again.' },
  { type: 'recycle', who: 'isha', with: 'puneecoscrap', material: 'WOOD', condition: 'BEYOND_REPAIR', stars: 5, comment: 'Hassle-free pickup of an old wardrobe. Glad it will be recycled and not dumped.' },
  { type: 'repair', who: 'ananya', with: 'whitefieldwood', item: 'Dining table', category: 'TABLE', problem: 'POLISHING', text: 'The top has lost its shine and has water marks.', stars: 5, comment: 'Beautiful French polish. The table looks brand new. Highly recommended.' },
  { type: 'repair', who: 'karthik', with: 'whitefieldwood', item: 'Wardrobe door', category: 'WARDROBE', problem: 'LOOSE_JOINTS', text: 'The door hinge has come loose and the door sags.', stars: 3, comment: 'Fixed properly but needed a second visit. Communication could be better.' },
  { type: 'purchase', who: 'meghna', with: 'indiranagarhub', stars: 5, comment: 'Great little shop. The bookshelf was in perfect shape and the owner was very helpful.' },
  { type: 'recycle', who: 'ananya', with: 'econestblr', material: 'MIXED', condition: 'BEYOND_REPAIR', stars: 4, comment: 'Dropped off an old sofa. Quick and easy. Wish they had a pickup slot on weekends.' },
  { type: 'repair', who: 'rahul', with: 'lajpatrepairs', item: 'Wooden bed', category: 'BED', problem: 'STRUCTURAL_DAMAGE', text: 'The bed frame cracked near the headboard.', stars: 4, comment: 'Strengthened the frame and it no longer creaks. Reasonable rates.' },
  { type: 'purchase', who: 'simran', with: 'karolbaghbazaar', stars: 5, comment: 'Got a dining set at a very good price. Owner even helped load the tempo.' },
  { type: 'recycle', who: 'aditya', with: 'delhigreenscrap', material: 'METAL', condition: 'BEYOND_REPAIR', stars: 5, comment: 'They even paid me for the iron frames. Very professional.' },
  { type: 'repair', who: 'saiteja', with: 'kukatpallycarp', item: 'Rocking chair', category: 'CHAIR', problem: 'BROKEN_LEG', text: 'A rocker snapped on my grandmother\'s chair.', stars: 5, comment: 'They saved a chair that means a lot to us. Wonderful craftsmanship.' },
  { type: 'purchase', who: 'lakshmi', with: 'banjarainteriors', stars: 4, comment: 'Nice pieces and honest description. Delivery took a couple of days.' },
  { type: 'recycle', who: 'farhan', with: 'charminarrecycle', material: 'WOOD', condition: 'REPAIRABLE', stars: 4, comment: 'Picked up an old cot the next day. Friendly team.' },
];
