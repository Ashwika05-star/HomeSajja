// Curated high-resolution artisanal furniture photographs from the Google Stitch design screens.
// Each remote URL is uploaded to Cloudinary through the unsigned preset (`homesajja_listings`) so Firestore only stores Cloudinary URLs.
const STITCH_REMOTE_URLS = {
  teakSofa: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCbyEs6TGFMslZpjEQLER_jYflr8FOs7320ovaJsRR9ra4gSz7KdZ0oqbScdODpxIORvdmfC4lkUoandv2O7jFqOVb-nZKJsp_qkyjnwfI8fwI-Lt51sTGqkgcgmZ1yS0pW9BUdv79XEvksumwiu_n7L4tc3So_xm6IgmUazEm7az_GNj6VXuL9dzS5Jc611cAkQbF5TY03eKjuJiqvIeVBv2XtxcmWY7xV1vaFm_P1z5_Z9trRV_YM',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDv642rR-pUfZbgnxOLGcnVgJ--zuZ8jPhBFsA_Ilw4C9G6uA1cr7wv9pYsBeJZIxDkHdToaL1YIN8JtI-o27chyizXHqmwbeiN0W3z3GV7N6kLEZ2HTM9jMm7Krv-DmxXDtWW_C2ECH-hIvexhADu9lYeieDc_gR4CH_pWQpCE8JNN7ByQDDtJF349XA7HR0V-uwUnjlP3zDCE0-fO_YdBc3s_DG1l2fjOs2n3_MiQOoEXSaQ_Adxm',
  ],
  lShapedSofa: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDv642rR-pUfZbgnxOLGcnVgJ--zuZ8jPhBFsA_Ilw4C9G6uA1cr7wv9pYsBeJZIxDkHdToaL1YIN8JtI-o27chyizXHqmwbeiN0W3z3GV7N6kLEZ2HTM9jMm7Krv-DmxXDtWW_C2ECH-hIvexhADu9lYeieDc_gR4CH_pWQpCE8JNN7ByQDDtJF349XA7HR0V-uwUnjlP3zDCE0-fO_YdBc3s_DG1l2fjOs2n3_MiQOoEXSaQ_Adxm',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDpSC-O3HoLMDMdF9zZoWMgLzKQnFtPWn4vgqS2B-zvZl3U6IUZciJY_WAuMDauk0_KEh1FytxTqVuDA_MlH1m5Fx_-JFrR9CQF7as-dFcwvaxGlWrN2MSZ-zKDn1WyUNNuSpMRLJliNB82sMWP28oNVzNovGOEmONh9BW-pi1Rk3j6LEpcgqOxZfMvD17UzMZC2TPRm_NPXAg1_O_sfp4iu3K0RC5cuLLX2NDxz-3TCICMcvLCTBnD',
  ],
  sheeshamDining: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuB-w94xEY9nT0vduTjR95n0zUH_D58WxBokj9tzZy1m9SqJQX2pyMC9ZYe-gSNeFUJ3ChdZJSDgPLH56DUmefUQVu5a0uwA53mVjyAlM7SWvbQ94cnANP1nrGloUUR-kWzVFn66YbwkebfUSnv_8xL1bq1eYnsbIJH4z_YMXjHd-x4EMm5r79d_iAtwCje_PLYNqky9GYHLmSDPjQqtTgmUgy1lac5eHPWyk240wfr1roFvupHKOkc9',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBZwp3lDLCtMdfInIS3J9d56PSY8o2Pra1aDEJdBBa2aK836fHO6C7ttK0WkPyQ8laSH-zKz-IjjEuEaTpFkDhZuwEuc2eOdXLQNAYj0HGCRHaQ8QBm2hg066o4mLq56N0YRG_VQ3x8ZxNavVUdOV9cshodwBSPS3PPEUnmeCDKi34P42emFhT6V3o-d9hJX68DzBgYpaYfI2FCPJ_8B_w1kiTbEsxd1Tcq2TYVMbLX34lxFmvaZ4a1',
  ],
  pegStool: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuB0OPD4-rdeItpFu4p8u8YSiSk-79UFETnqLvFtPbUZ6N5D-mv--ImS0Osrf-RxzY5uFrWV6nsP7nz5SWbSKaA_mhuiIPwhqThfBNiT_PfjwVmX7fPPcgLze-pbFAkydhQTx-bh42NW8F_fLdTkSySR8cB3NYTlbpwzqxI2HonreU7hPty59EzO8VUhGs4AxFsiCv3QPlrpKZ_jmrmVnsNbNVTF6G0Ett6A61rUo06dmunvVQzoR-k9',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDWCss1KBiEkvO4JveJvfrlvPQy50DT7J9QgVhtaMfR0B03TGvnHQXkoyIZkF7Cmi-Cwjx_uo8OBXuAXq26HZvPTsT0jGjzunQqlJRla7RJICJpUYycdAV7yRUy8UrH_Uy1-YmOuo53636oFZw8jfdlg4Ej7Kys_DXcY7VFwjQBj5N_4UbZxYwpkrpq6-M6FGkmzLmfcBBckz7404_t3b2XerAQGVufS5nApzf4Ijjsvnom1Prpz0vb',
  ],
  caneArmchair: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAeSTa3SxCFHDTdNHE9yiHIgzJMIw5ioL9lQT-XX6mZixd8YdBc86w-92fgedDQzFNk17pXgS-8YT3oNg6jkY-rD60Z4vGZxtp3tYTFXKz7hOnDHjmBNh3-1Mnbxox7JhF3soGf_esn9TDg783w5Fu3quNZxrafevQRQ2nhx234pD8eYhcmk3CaAv_eAJh374gPHLUqMwhKKirvocMnLWOvIjLOCq5jrD2Lqsgd6rVZ4--utOtLes4z',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuACzGKrTNQqgM0jX7jCgT1X12ojQvq3s2MZFy8KUNsdedSxbxS7HxVpn5NgYjo0wdOW3H6cZW1ynKA45fuDURxAJSitIWMZ-SM7ZaaL7K-Pctw6BMpXmfUX4o7Cd_dNblw6Zu29nfbZUmrBve33kQdxzRh3SJ9kdaRvFZDjZEJ71aPJJDMNQqUbvj5THOBOkD5IQZv_VZnNrnyhFAKkSTDwA5MPDcOTn3whR36S7kI0qIhrCn7EKgsD',
  ],
  studyStool: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDWCss1KBiEkvO4JveJvfrlvPQy50DT7J9QgVhtaMfR0B03TGvnHQXkoyIZkF7Cmi-Cwjx_uo8OBXuAXq26HZvPTsT0jGjzunQqlJRla7RJICJpUYycdAV7yRUy8UrH_Uy1-YmOuo53636oFZw8jfdlg4Ej7Kys_DXcY7VFwjQBj5N_4UbZxYwpkrpq6-M6FGkmzLmfcBBckz7404_t3b2XerAQGVufS5nApzf4Ijjsvnom1Prpz0vb',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAgEqpm7cFgheqvodG8hfMU29GG878f5Ub0aWB8v3f8x-m2gX68s-6BrNo2hhlRSHsYi0cKZTNFvE5SNxZEOKVYCRlHAvkfbwxgnMlbduqmovtJoo48XPtGl3JNx5yxCaSiVrTgN_DQYHEX0tpHNbUKjtQ5ZNXEvlyEMPPzA5hf9m793zJXNwCY9D7hyBltHj9lUzd_Fuv6ObsKk4yudWZn3aW-lW6gi3qd7bz_7B7hceqQoF3c16E-',
  ],
  sideTable: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCb6uSe42MRaoVZRS1a14t5HX8KntMyKPvPmqzhXIXWsJfkJqV3ncuKWFVcx2qlpa69IJUsMUhQuQeCQ2V8iBoAPgI4UQig1bRAKSV7Jv6x6CTBdwqkSo7fu6uSZ29wOYt2UwBI15ai54xl1imhI9xCvh4S94LlnPY4nfIDr58VKJjNh2u3LoKQ-foG5c9QhwgTQ_gwLc3EaD3kgiJeg6iwsM3TxMxVLl1nhffJTSRlo4iBB6g5ocJJ',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuA0kjlwn4M1vZ3oWUy9T5aZbrbNXf63ZUPCyYWwnJ36L7168tBFTNZMUXPRb4hH98be175-bbW82V1wxZpdRtjcvi7q5Du_tqtPPISiM6QhyDyiIAv8-hzzLamOqTZuIpE6QFqrPlqRf7x1Y3PuT2kcS9Xp84P4QypnQBpa09CLrJ3RjRqihmjS-YAnvFg3Mcen63QFz2r3iQ91KuZixItG8xNQoapywdGS1hxsvAO1tZoyXRf8sQb7',
  ],
  officeChair: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDuixcqpD23H-BbwSLZZpogInxEBhBTg2MSA5QE6ROHCrkhz58lA1jhZrQDQCUSAIjuMTLzREbAw96jJ2O8aprv9fYHK0E78OgAh67-kJK5SdAwmWzVwUwDObRe5GJhuwpuJ9uMzt0AvKUiXmjAZrStN1zQgy_2PiN9jDyiPHguMvE8M_szmjLsRe6gaJLWS21mDtSqrf0QDdLro3F1eBjuxueTCAGly5yYWfxYnBRYH0uigZIhGig5',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCt2pTtNZ0Epm58W1z89CRUMZliRptFHcjVEduPSJkKB_5_4chQeFp0fFWUJllefY3qnJXVqzIQwZjiE8GRoO99TbUst26VledED0luwotO1ISJyqlrElfHgCvZU19V9p_T_2ssSoNYFOEGKUWC08EEkt2v0sAm72jmAdK7uWdt41F4Tq6Og67-gxIwhwMbx6Bnv_sL5hKPYX0Zv-SzDAJxKA3bwXQuyL0m30GreYbK4BDCitQgDaTR',
  ],
  rosewoodBedside: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBmT61OfDZzeyqrCByN_wHvOfoQWHX-pGjXRn6O5fw7ehFxJiu0A7Ww1xMzvMvY4k83B_nFnl-cgPqyNj38ctzoRRhoWqHw1YntuMJ-LikqfACydhH_MUcIjqt_T4wwC9X2p1uIRN0mxwZs8RwIwYrC7ozfoRzDm466yhILqcB3Q-Gq95ciZYyrqsNntcqtnZD_UrOVs0wcdGBQepsG-dlTXx8fnU7XREl_Bg5YTpVJYBI05paw9LNa',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAFGHngIFwAWwCnHh6atp6TCeM3_3QK5P6v0PJgWRwBr6C5HkBAkmBzKYRY0lHKwvY7eqAIo4htSFRQDmCg7jtSYpEUtdtBgQZuEe8Q5jtHgfavvTnlM5cG-1ze1iO2GxhYkUUfCSd2Suhi9WX_TCYtX_KHp2O8yOaOPNsL-5gL5KV_wlTqxK0YA3qomYPISb4iSfPvfVXUIxFKoU9s8MOnVkI4lT0Rnw3xYyaDb-cQ75eq1uaKsHH1',
  ],
  carvedShelf: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDaMzTmqrHl3oGSwcnW_VgJjAxw1POCyDwHgpGuosql1u0hDaPkOHOk2E0K76mjBLvdPmy38_8VkshYVLKGlXuts8abZGX4SwVYnjRmXIXk4R0aLWVdwmIumviAC9ba66DD-kUJbm8WWmUO4Soa2YZJFp50r3aWCl9Ph_KXg-UVtbPvYVogkkTxx9hnXEjXM__0I8RB1sOjefKU2ONoP7mncktgX69PMAWHsqdZoROJEWuWg6bMlIg5',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuD7D1rUSu2bUYFNLzXLh721dTHT6J_fY2s5SPLuqRWmqPQ4NWZGILpjFU-qJrig7sAmE__KPmzRbs0-hQWXY0vqjT7YUfAcKASnOT6C6_OiJec0xLHvAR9r9pGRypB2xw0CHmqDa5ISXg0Usgxj-5t8xYz3k1OdSn7ZNZnPlWc2R9Hrwj3s3AUHbfwkCn7AyoYxIrC-p8z_4UXs41ds09cQm3r2JbY7Dskbk4eYwnPV7N9rIZYP-Mkd',
  ],
  studyDesk: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBkPC6Do3BHge3yZ5Zcu6nGgPqHp_0Bg4O67Yn8sdaKd_Weni6VmjHJOZc07NlO8zyy0lTbyvYqyQyadYh8B1BHFsxEnhxPeEIcjrpNPE238KhHRJz5G0CsChR9o2hXd-zeufeo60W5z1tVJgQlfXBHXWZaN_dV3hQaD-testOcq8pCNdWkr6uDsWKhb0hIkhqQDmAzBO7xmwBMykInFRHYO1dHhUzGFDSNRyCJM4wi0IGGlu0F9WDe',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuB0V7RvtKRgG_OD5EpQv9Xo32NcTey_y9qpx8NXMYGaXLRHxjzxyvjeLXHBVg1Ovj1JARRzAZTLbmWDkBIyj-1-o_Sp85GWEkcEQMrkqCGOs-AxUE_CXQ7hsDItg5Ow5CwzoIpqAOoyKPvdokpGkjl1N493U8DM5sbh_VJlOvvfhtt_UxSAs5T3PIGwj9tsc5EqzFRloGeN0F141WAXIZ0sgp5aSJvQ1_ero3dB0A3YF58CmJV5YGml',
  ],
  wardrobe: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBwPkTauC4vjc-fG8eC1wxv9FtaWMZWJtb-X0xf0y0w3PH0g7Z4pkmHGqodAa_J5wzTqkC0Gw530vlh126hmYroRFtmx-f7Grv6tnJfluMYT1S1D5kXold7t1AAJzITmIivZBxGhtLHVDYNPv5Nqpr9WFRxM_Gk-kaKixm02TnHnNAMQRRQ1Q8m0xwC5cewxPznVYfJs3nvyt_fL_aTgmlfGDW2r2-N17LbtqzOZN5cC2ul2ahoyoFG',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCG8ph9Zm3CgRM1mDayFU8qww511U--W6tPxWba37luzrYONGQppHw1N-Me6rE7VOsmeiPLAp45BE_gNQol_qizm0YQGTGZHSjDzA7kZAU-vDh-oQCaNIyDfm6OZeycb1FvsJUKq40_EezacBukCxCC5iZATBxxsVZBv8yzZHWRrAW7oxIX6lnCOgzelNYI5J_pmsF_W46e7mYV13anV6CgHL3ZJY9FsY9FL7gNFT5JfJXctLB09WX9',
  ],
  coffeeTable: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAiyiy8CvMaORCPvjhta4UBYmeE3popKhYVaIFqUokbN6qkuGLevWCzF8sFklaF9v1Snom5Xb6EeiJUbqxZxMk_ju_H9_8zPpEyJ6kV6L8EwoT4J7f-8WuufKBOSrtbGBUoPI7yBXaTL_Lg2NQ2AqfrboKyPc7cb_ioILByy3CKYZGYsuqXM_Baqi25XXHvk7IgugNxzNjp9YPs6J1NRfmAKU4SnYP_N-WoD4LsbLe0D5aUbMGKiWJJ',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCgKSZPk9EMpBonP0mt-N1SHOEZlGaJh2O0feEpDuXpsvG0sReRXmwe1DNotzvI6DxHz_ezERpbcF729rEL2MwQ3F7241wKIEiqq7fbYOE96LZBI0vWLqvgyqIzEzwtDB0uUm4DnS2IDst8LOpy_cJ2rJCbeNooAvDNzVEwywbBEZtYOzeUc4yP9kddP4dYQzvEA--_-Go4rohcWh_F0V6opzVGcgFzLiAvaCBX5wnkzroHDTOL8yOc',
  ],
  caneRockingChair: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDZ043F4UJ5RXqhu4rylguFE8cLB2lBjXrBfMaAnC8air9cmlVCA3nUdkcXl0pquJXTudFren2HTY_s-ZzJAK3ap1WBQ39U7nu6FjrXG_QnHwY43XHUOY7CXzZiuqRUELdWGCkXYYWohXkE5CPMJdiKKd3EPm--52-QSI4k9MowAZ7adGGSE9uLaZ5VNvC1DQM2w0o7eTPIGn0tFhMd1JAzPBB0AOgvY1gMgELAt3ZTYW0Lo81V8pt6',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAk_IwrZYnsUqFbn6_q3TEG1WVEj-tlq-zCRPaelGeTrLYnQ0ROjMwazv9yNUgOF6kd-vtnykgqGWOwayVssBnhpjZTbtppsScZByP8ZFkJ7bmXRZO3VlUWJE5gM69vbolfnSkjUv_mpshE_3Y3OLS_Dtt6fp3GE3OgCC0flTcgqORVPHpStdUnwTNGeDRu4oE4jIN9p-9LvQ7ILBxobB_WZ26o78zARtG8Pa1FDxoxs57qdlmAph2C',
  ],
  workshopBrochure: [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBZZPQ2mMfCPxfWfXVsPeCzZt9C7xUDHrA2utGQERegCpbjlA_9a7karlmWlBOhZJi85uFJAAMOAA7c2AWLjyj96X-Py9RTeNxZCk1-gKJB2mp2_pf_0r_y5R9wU80ESbmTP6M6N8hZMHElK8L2KlfsuT3LtaTJxu1d46fWZ4XGlAFj2oR7Txb6E9EivEtDLccKMd6s6Gbm3KUwO8_vr6JtG_2hDFyxV_6oRIflMHBeSny1r6jWkBLj',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuB4YF5iRr7yLb_en0nhG7hw2trXvwt-u4wwaoBCFWy-OCX8HfUNglBp_lL9NujimlpzFzm9h6JfD6elGX4K9snOfhAJSmmwFWF74vCJMloZqiNO3H0QbwnK9DqU3uYEBa0bgfiaOnEz723QFtNrsOBT9qzPFfu5S9cow53i6uLQl3_QUzSv2QgWfuOjtkzmEFAeQFuNu7SECMc2xBdlfTbHDV3YFu1tZeU2KjBuEaSOB1zi8Z6L6VAJ',
  ],
};

// Populated in-place with Cloudinary `https://res.cloudinary.com/...` URLs when `uploadIllustrations` runs.
export const STITCH_PHOTOS = Object.fromEntries(
  Object.entries(STITCH_REMOTE_URLS).map(([key, list]) => [key, [...list]]),
);

/** Uploads a remote URL through the app's unsigned preset (`homesajja_listings`) and returns the Cloudinary `secure_url`. */
async function uploadRemoteUrl(config, publicId, remoteUrl) {
  const form = new FormData();
  form.set('upload_preset', config.uploadPreset);
  form.set('public_id', publicId);
  form.set('file', remoteUrl);
  const response = await fetch(`https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload`, {
    method: 'POST',
    body: form,
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(`Cloudinary upload of ${publicId} failed: ${data.error?.message ?? response.status}`);
  }
  return data.secure_url;
}

/** Uploads every curated Stitch photograph to Cloudinary (unsigned preset `homesajja_listings`) by passing the remote URL. */
export async function uploadIllustrations(config, log) {
  const uploadedByRemote = new Map();
  for (const [key, remoteList] of Object.entries(STITCH_REMOTE_URLS)) {
    for (let i = 0; i < remoteList.length; i++) {
      const remoteUrl = remoteList[i];
      let secureUrl = uploadedByRemote.get(remoteUrl);
      if (!secureUrl) {
        const publicId = `homesajja-stitch/${key}-${i}`;
        secureUrl = await uploadRemoteUrl(config, publicId, remoteUrl);
        uploadedByRemote.set(remoteUrl, secureUrl);
        log(`  cloudinary: ${publicId} -> ${secureUrl}`);
      }
      STITCH_PHOTOS[key][i] = secureUrl;
    }
  }

  const CATEGORY_PHOTOS = {
    SOFA: [...STITCH_PHOTOS.teakSofa, ...STITCH_PHOTOS.lShapedSofa],
    TABLE: [...STITCH_PHOTOS.sheeshamDining, ...STITCH_PHOTOS.rosewoodBedside],
    CHAIR: [...STITCH_PHOTOS.caneArmchair, ...STITCH_PHOTOS.pegStool, ...STITCH_PHOTOS.caneRockingChair],
    DESK: STITCH_PHOTOS.studyDesk,
    WARDROBE: STITCH_PHOTOS.wardrobe,
    STORAGE: [...STITCH_PHOTOS.rosewoodBedside, ...STITCH_PHOTOS.wardrobe],
    BOOKSHELF: STITCH_PHOTOS.carvedShelf,
    TV_UNIT: [...STITCH_PHOTOS.studyDesk, ...STITCH_PHOTOS.coffeeTable],
    COFFEE_TABLE: [...STITCH_PHOTOS.coffeeTable, ...STITCH_PHOTOS.sideTable],
    RECLINER: [...STITCH_PHOTOS.caneArmchair, ...STITCH_PHOTOS.caneRockingChair],
    BED: [...STITCH_PHOTOS.sheeshamDining, ...STITCH_PHOTOS.teakSofa],
    OTHER: [...STITCH_PHOTOS.pegStool, ...STITCH_PHOTOS.sideTable],
  };

  const urls = {};
  for (const [category, list] of Object.entries(CATEGORY_PHOTOS)) {
    for (let variant = 0; variant < 3; variant++) {
      urls[`${category}:${variant}`] = list[variant % list.length];
    }
  }
  return urls;
}
