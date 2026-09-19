// Demo people. Every account shares one password (see README) and an @homesajja.demo email, so they are easy to spot and remove.
export const DEMO_PASSWORD = 'HomeSajja#Demo1';

const user = (key, name, city, phone) => ({ key, kind: 'user', name, city, phone, email: `demo.${key}@homesajja.demo` });

export const USERS = [
  user('aarav', 'Aarav Mehta', 'Mumbai', '+91 98200 11001'),
  user('priya', 'Priya Nair', 'Mumbai', '+91 98200 11002'),
  user('rohan', 'Rohan Deshmukh', 'Mumbai', '+91 98200 11003'),
  user('sneha', 'Sneha Kulkarni', 'Pune', '+91 98220 22001'),
  user('vikram', 'Vikram Joshi', 'Pune', '+91 98220 22002'),
  user('isha', 'Isha Patil', 'Pune', '+91 98220 22003'),
  user('ananya', 'Ananya Rao', 'Bengaluru', '+91 98860 33001'),
  user('karthik', 'Karthik Iyer', 'Bengaluru', '+91 98860 33002'),
  user('meghna', 'Meghna Reddy', 'Bengaluru', '+91 98860 33003'),
  user('rahul', 'Rahul Sharma', 'Delhi', '+91 98110 44001'),
  user('simran', 'Simran Kaur', 'Delhi', '+91 98110 44002'),
  user('aditya', 'Aditya Verma', 'Delhi', '+91 98110 44003'),
  user('saiteja', 'Sai Teja', 'Hyderabad', '+91 98490 55001'),
  user('lakshmi', 'Lakshmi Prasanna', 'Hyderabad', '+91 98490 55002'),
  user('farhan', 'Farhan Ali', 'Hyderabad', '+91 98490 55003'),
];

const vendor = (key, name, businessName, businessType, city, area, lat, lng, description, extra = {}) => ({
  key, kind: 'vendor', name, businessName, businessType, city, area, lat, lng, description,
  email: `demo.${key}@homesajja.demo`, phone: '+91 90000 ' + String(10000 + Math.abs(key.split('').reduce((a, c) => a * 31 + c.charCodeAt(0), 7)) % 89999),
  ...extra,
});

const repairs = { repairServices: ['BROKEN_LEG', 'LOOSE_JOINTS', 'POLISHING', 'SCRATCHES', 'DAMAGED_WOOD'], repairCostMin: 400, repairCostMax: 6000 };

export const VENDORS = [
  vendor('andherimart', 'Manish Shah', 'Andheri Furniture Mart', 'SHOP', 'Mumbai', 'Andheri East', 19.1136, 72.8697,
    'Pre-owned and lightly used furniture from Mumbai homes, checked and priced fairly. Delivery across the western suburbs.'),
  vendor('dadarwood', 'Prakash Naik', 'Dadar Woodcraft Repairs', 'REPAIR_PROFESSIONAL', 'Mumbai', 'Dadar West', 19.0178, 72.8478,
    'Three generations of carpentry: polishing, joint repair, cane weaving and upholstery fixes. Free pickup within Dadar.', { ...repairs, repairCostMax: 7500 }),
  vendor('greenloopmum', 'Fatima Sheikh', 'GreenLoop Recyclers Mumbai', 'RECYCLER', 'Mumbai', 'Goregaon East', 19.1663, 72.8526,
    'We collect old furniture and reclaim wood, metal and fabric. Nothing goes to landfill if we can help it.'),
  vendor('revivemum', 'Nikhil Rane', 'Revive Refurb Studio', 'REFURBISHER', 'Mumbai', 'Vile Parle', 19.1075, 72.8440,
    'We restore old teak and sheesham pieces to their best and sell them with a 6-month workmanship promise.'),

  vendor('koregaoncarp', 'Sanjay Bhosale', 'Koregaon Carpentry Works', 'CARPENTER', 'Pune', 'Koregaon Park', 18.5362, 73.8940,
    'Custom furniture and quick repairs by experienced carpenters. Home visits in Pune east.', repairs),
  vendor('banerrefurb', 'Deepa Kale', 'Baner Refurb Studio', 'REFURBISHER', 'Pune', 'Baner', 18.5590, 73.7868,
    'Second-life furniture, repainted and reupholstered. Ready to move into your home.'),
  vendor('puneecoscrap', 'Amit Jadhav', 'Pune EcoScrap & Recycling', 'RECYCLER', 'Pune', 'Kothrud', 18.5074, 73.8077,
    'Old sofas, beds and wardrobes taken away and responsibly recycled. Same-week pickup.'),

  vendor('indiranagarhub', 'Ramesh Gowda', 'Indiranagar Furniture Hub', 'SHOP', 'Bengaluru', 'Indiranagar', 12.9784, 77.6408,
    'Curated second-hand furniture for compact Bengaluru apartments. Walk in or order through HomeSajja.'),
  vendor('whitefieldwood', 'Suresh Babu', 'Whitefield Wood Doctor', 'REPAIR_PROFESSIONAL', 'Bengaluru', 'Whitefield', 12.9698, 77.7500,
    'Wood repair, French polish and modular furniture fixes for Whitefield and Marathahalli.', repairs),
  vendor('econestblr', 'Lavanya Krishna', 'EcoNest Recycling', 'RECYCLER', 'Bengaluru', 'Jayanagar', 12.9308, 77.5838,
    'Drop off or schedule a pickup. We recycle wood, metal frames and fabric from old furniture.'),

  vendor('karolbaghbazaar', 'Harpreet Singh', 'Karol Bagh Furniture Bazaar', 'SHOP', 'Delhi', 'Karol Bagh', 28.6519, 77.1909,
    'Big showroom of pre-loved sofas, beds, wardrobes and dining sets. Bargains every week.'),
  vendor('lajpatrepairs', 'Mohit Arora', 'Lajpat Wood Repairs', 'REPAIR_PROFESSIONAL', 'Delhi', 'Lajpat Nagar', 28.5677, 77.2433,
    'Repairs and polishing for wooden furniture. Estimates in one day.', repairs),
  vendor('delhigreenscrap', 'Zoya Khan', 'Delhi Green Scrap Co.', 'RECYCLER', 'Delhi', 'Okhla', 28.5355, 77.2731,
    'Furniture scrap and reuse. We pay for good metal and hardwood, and pick up across South Delhi.'),
  vendor('capitalcarp', 'Baldev Chauhan', 'Capital Carpenters', 'CARPENTER', 'Delhi', 'Janakpuri', 28.6219, 77.0878,
    'Repairs, alterations and made-to-order furniture. Fast and tidy work.', repairs),

  vendor('banjarainteriors', 'Srinivas Rao', 'Banjara Interiors Outlet', 'SHOP', 'Hyderabad', 'Banjara Hills', 17.4126, 78.4482,
    'Quality used furniture and home décor from Banjara Hills homes. Delivery within Hyderabad.'),
  vendor('kukatpallycarp', 'Yadagiri Goud', 'Kukatpally Carpenters', 'CARPENTER', 'Hyderabad', 'Kukatpally', 17.4948, 78.3996,
    'Reliable carpenters for repair, polishing and custom work in Kukatpally and Miyapur.', repairs),
  vendor('charminarrecycle', 'Imran Pasha', 'Charminar Recyclers', 'RECYCLER', 'Hyderabad', 'Charminar', 17.3616, 78.4747,
    'Furniture recycling and scrap collection. Wood, metal and cloth all find a second life.'),
];

export const ALL_PEOPLE = [...USERS, ...VENDORS];
