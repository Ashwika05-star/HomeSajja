package com.homesajja.app.ui.i18n

import com.homesajja.app.data.model.VendorBusinessType

/**
 * Idiomatic Hindi translations for the vendor experience in HomeSajja.
 */
object VendorStrings {

    // --- Navigation & Core Tabs
    fun dashboard(isHindi: Boolean) = if (isHindi) "डैशबोर्ड" else "Dashboard"
    fun listings(isHindi: Boolean) = if (isHindi) "लिस्टिंग्स" else "Listings"
    fun requests(isHindi: Boolean) = if (isHindi) "अनुरोध" else "Requests"
    fun explore(isHindi: Boolean) = if (isHindi) "मार्केट" else "Explore"
    fun profile(isHindi: Boolean) = if (isHindi) "प्रोफाइल" else "Profile"
    fun addListing(isHindi: Boolean) = if (isHindi) "+ लिस्टिंग जोड़ें" else "+ Add Listing"
    fun addListingFab(isHindi: Boolean) = if (isHindi) "लिस्टिंग जोड़ें" else "Add listing"
    fun logOut(isHindi: Boolean) = if (isHindi) "लॉग आउट" else "Log out"

    // --- Dashboard
    fun totalEarned(isHindi: Boolean) = if (isHindi) "कुल कमाई" else "Total earned"
    fun thisMonth(isHindi: Boolean) = if (isHindi) "इस महीने" else "This month"
    fun earnedNote(isPartial: Boolean, isHindi: Boolean) = if (isHindi) {
        if (isPartial) "हाल ही में प्राप्त हुए भुगतानों की गणना।" else "केवल वही भुगतान शामिल हैं जो आपको प्राप्त हुए हैं।"
    } else {
        if (isPartial) "Counts your most recent confirmed payments." else "Counts only payments you confirmed as received."
    }
    fun earnedEachMonth(isHindi: Boolean) = if (isHindi) "मासिक कमाई" else "Earned each month"
    fun noPaymentsYet(isHindi: Boolean) = if (isHindi) {
        "पिछले 6 महीनों में कोई पुष्ट भुगतान नहीं है। जब कोई भुगतान करेगा और आप \"भुगतान मिला\" दर्ज करेंगे, तो यह यहाँ दिखाई देगा।"
    } else {
        "No confirmed payments in the last 6 months yet. When someone pays you and you tap \"Payment received\", it appears here."
    }
    fun noReviewsYet(isHindi: Boolean) = if (isHindi) "अभी कोई समीक्षा नहीं" else "No reviews yet"
    fun reviewsCount(count: Int, isHindi: Boolean) = if (isHindi) "$count समीक्षाएं" else "$count reviews"

    fun yourWork(isHindi: Boolean) = if (isHindi) "आपका कार्य" else "Your work"
    fun completed(isHindi: Boolean) = if (isHindi) "पूरा हुआ" else "Completed"
    fun pending(isHindi: Boolean) = if (isHindi) "लंबित" else "Pending"
    fun sales(isHindi: Boolean) = if (isHindi) "बिक्री" else "Sales"
    fun repairs(isHindi: Boolean) = if (isHindi) "मरम्मत" else "Repairs"
    fun exchanges(isHindi: Boolean) = if (isHindi) "एक्सचेंज" else "Exchanges"
    fun recycling(isHindi: Boolean) = if (isHindi) "रीसाइक्लिंग" else "Recycling"

    fun goTo(isHindi: Boolean) = if (isHindi) "यहाँ जाएं" else "Go to"
    fun myListings(isHindi: Boolean) = if (isHindi) "मेरी लिस्टिंग्स" else "My listings"
    fun onSaleNow(count: Int, isHindi: Boolean) = if (isHindi) "$count अभी बिक्री के लिए" else "$count on sale now"
    fun incomingRequests(isHindi: Boolean) = if (isHindi) "आने वाले अनुरोध" else "Incoming requests"
    fun pendingText(pending: Int, isHindi: Boolean) = if (isHindi) {
        if (pending == 0) "कोई लंबित अनुरोध नहीं" else "$pending अनुरोध प्रतीक्षारत"
    } else {
        if (pending == 0) "Nothing waiting" else "$pending waiting for you"
    }
    fun exploreMarketplace(isHindi: Boolean) = if (isHindi) "मार्केटप्लेस देखें" else "Explore marketplace"
    fun browseAllListings(isHindi: Boolean) = if (isHindi) "अपने शहर में सभी फर्नीचर लिस्टिंग देखें" else "Browse all furniture listings in your city"
    fun monthlyReport(isHindi: Boolean) = if (isHindi) "मासिक रिपोर्ट" else "Monthly report"
    fun exportReportDesc(isHindi: Boolean) = if (isHindi) "महीने की रिपोर्ट PDF या CSV में निकालें" else "Export a month as PDF or CSV"

    fun finishProfileTitle(isHindi: Boolean) = if (isHindi) "दुकान की प्रोफाइल पूरी करें" else "Finish your shop profile"
    fun finishProfileDesc(isHindi: Boolean) = if (isHindi) "विवरण और दुकान का स्थान जोड़ें ताकि ग्राहक आपसे आसानी से जुड़ सकें।" else "Add a description and your shop location so customers can find you."
    fun editProfile(isHindi: Boolean) = if (isHindi) "प्रोफाइल बदलें" else "Edit profile"

    fun welcomeTitle(isHindi: Boolean) = if (isHindi) "होम सज्जा में आपका स्वागत है" else "Welcome to HomeSajja"
    fun welcomeDesc(hasListings: Boolean, isHindi: Boolean) = if (isHindi) {
        if (hasListings) "आपकी लिस्टिंग सक्रिय हैं। जब ग्राहक अनुरोध भेजेंगे और भुगतान करेंगे, तब आपकी कमाई और कार्य यहाँ दिखाई देंगे।"
        else "अभी यहाँ कुछ नहीं है। जब आप सामान लिस्ट करेंगे, अनुरोध स्वीकार करेंगे और भुगतान प्राप्त करेंगे, तब आपकी कमाई और कार्य यहाँ दिखाई देंगे।"
    } else {
        if (hasListings) "Your listings are live. Once customers send requests and pay you, your earnings, finished work and rating show up on this page."
        else "Nothing here yet. Once you list something, answer requests and customers pay you, your earnings, finished work and rating show up on this page."
    }
    fun addFirstListing(hasListings: Boolean, isHindi: Boolean) = if (isHindi) {
        if (hasListings) "एक और लिस्टिंग जोड़ें" else "अपनी पहली लिस्टिंग जोड़ें"
    } else {
        if (hasListings) "Add another listing" else "Add your first listing"
    }
    fun notVerifiedYet(isHindi: Boolean) = if (isHindi) "सत्यापित नहीं" else "Not verified yet"
    fun verified(isHindi: Boolean) = if (isHindi) "सत्यापित" else "Verified"

    fun businessType(type: String?, isHindi: Boolean): String = if (isHindi) when (type?.uppercase()) {
        "REPAIR", "REPAIR_PROFESSIONAL" -> "मरम्मत विशेषज्ञ"
        "RECYCLER" -> "रीसाइक्लिंग विशेषज्ञ"
        "ARTISAN", "ARTISAN_UPCYCLER" -> "कारीगर / अपसाइक्लर"
        "FURNITURE_VENDOR", "VENDOR" -> "फर्नीचर विक्रेता"
        else -> "विक्रेता"
    } else (type?.let { VendorBusinessType.fromNameOrNull(it)?.displayName } ?: "Vendor")

    fun monthName(monthStr: String, isHindi: Boolean): String = if (isHindi) when (monthStr.trim().lowercase().take(3)) {
        "jan" -> "जन"
        "feb" -> "फ़र"
        "mar" -> "मार्च"
        "apr" -> "अप्रै"
        "may" -> "मई"
        "jun" -> "जून"
        "jul" -> "जुला"
        "aug" -> "अग"
        "sep" -> "सित"
        "oct" -> "अक्टू"
        "nov" -> "नव"
        "dec" -> "दिस"
        else -> monthStr
    } else monthStr

    // --- Requests & Sub-tabs
    fun purchases(isHindi: Boolean) = if (isHindi) "खरीद" else "Purchases"
    fun exchangesTab(isHindi: Boolean) = if (isHindi) "एक्सचेंज" else "Exchanges"
    fun repairsTab(isHindi: Boolean) = if (isHindi) "मरम्मत" else "Repairs"
    fun recyclingTab(isHindi: Boolean) = if (isHindi) "रीसाइक्लिंग" else "Recycling"

    fun incoming(isHindi: Boolean) = if (isHindi) "आने वाले" else "Incoming"
    fun outgoing(isHindi: Boolean) = if (isHindi) "भेजे गए" else "Outgoing"
    fun sent(isHindi: Boolean) = if (isHindi) "भेजे गए" else "Sent"
    fun received(isHindi: Boolean) = if (isHindi) "प्राप्त हुए" else "Received"

    fun openPickups(isHindi: Boolean) = if (isHindi) "उपलब्ध पिकअप" else "Open pickups"
    fun myPickups(isHindi: Boolean) = if (isHindi) "मेरे पिकअप" else "My pickups"
    fun history(isHindi: Boolean) = if (isHindi) "इतिहास" else "History"

    fun claimFree(isHindi: Boolean) = if (isHindi) "पिकअप लें (निःशुल्क)" else "Claim (free)"
    fun claimWithQuote(isHindi: Boolean) = if (isHindi) "कोटेशन भेजकर लें" else "Claim & quote"
    fun fromCustomer(customer: String, isHindi: Boolean) = if (isHindi) "ग्राहक: $customer" else "From $customer"
    fun furnitureItem(material: String, isHindi: Boolean) = if (isHindi) "$material फर्नीचर" else "$material furniture"

    fun noOpenPickupsInCity(city: String, isHindi: Boolean) =
        if (isHindi) "$city में कोई नया पिकअप उपलब्ध नहीं है" else "No open pickups in $city"
    fun noOpenPickupsSubtitle(isHindi: Boolean) =
        if (isHindi) "ऐसे पिकअप अनुरोध जिन्हें अभी किसी ने नहीं लिया है, वे यहाँ दिखाई देंगे।"
        else "Pickup requests nobody has claimed yet will show up here."

    // --- Listings
    fun activeTab(count: Int, isHindi: Boolean) = if (isHindi) "सक्रिय ($count)" else "Active ($count)"
    fun inactiveTab(count: Int, isHindi: Boolean) = if (isHindi) "बिक चुके / निष्क्रिय ($count)" else "Sold / Inactive ($count)"
    fun noActiveListings(isHindi: Boolean) = if (isHindi) "कोई सक्रिय लिस्टिंग नहीं है" else "No active listings"
    fun noActiveListingsSub(isHindi: Boolean) = if (isHindi) "अपनी दुकान का फर्नीचर लिस्ट करने के लिए नीचे बटन दबाएं।" else "List furniture from your shop to make it visible to local buyers."
    fun noInactiveListings(isHindi: Boolean) = if (isHindi) "कोई पुरानी लिस्टिंग नहीं है" else "No closed listings"

    // --- Profile & Settings
    fun blockedPeople(isHindi: Boolean) = if (isHindi) "ब्लॉक किए गए लोग" else "Blocked people"
    fun language(isHindi: Boolean) = if (isHindi) "भाषा" else "Language"
    fun appearance(isHindi: Boolean) = if (isHindi) "थीम" else "Appearance"
    fun about(isHindi: Boolean) = if (isHindi) "दुकान के बारे में" else "About"
    fun location(isHindi: Boolean) = if (isHindi) "स्थान व पता" else "Location"
    fun reviews(isHindi: Boolean) = if (isHindi) "ग्राहक समीक्षाएं" else "Reviews"
    fun catalogue(count: Int, isHindi: Boolean) = if (isHindi) "कैटलॉग ($count)" else "Catalogue ($count)"
    fun noCatalogueItems(isHindi: Boolean) = if (isHindi) "अभी कोई वस्तु लिस्ट नहीं की गई है।" else "No items listed right now."
    fun noDescriptionYet(isHindi: Boolean) = if (isHindi) "इस विक्रेता ने अभी तक कोई विवरण नहीं जोड़ा है।" else "This vendor hasn't added a description yet."
    fun noLocationYet(isHindi: Boolean) = if (isHindi) "दुकान का स्थान अभी नहीं जोड़ा गया।" else "Shop location not added yet."

    // --- Reports
    fun exportPdf(isHindi: Boolean) = if (isHindi) "PDF डाउनलोड करें" else "Export PDF"
    fun exportCsv(isHindi: Boolean) = if (isHindi) "CSV डाउनलोड करें" else "Export CSV"
    fun preparing(isHindi: Boolean) = if (isHindi) "तैयार किया जा रहा है…" else "Preparing…"

    // --- Edit Profile
    fun shopProfileTitle(isHindi: Boolean) = if (isHindi) "दुकान का प्रोफाइल" else "Shop profile"
    fun businessNameLabel(isHindi: Boolean) = if (isHindi) "दुकान / व्यापार का नाम" else "Business name"
    fun businessTypeLabel(isHindi: Boolean) = if (isHindi) "व्यापार का प्रकार" else "Business type"
    fun aboutBusinessLabel(isHindi: Boolean) = if (isHindi) "दुकान / व्यापार के बारे में" else "About your business"
    fun phoneLabel(isHindi: Boolean) = if (isHindi) "फोन नंबर" else "Phone number"
    fun cityLabel(isHindi: Boolean) = if (isHindi) "शहर" else "City"
    fun addressLabel(isHindi: Boolean) = if (isHindi) "दुकान का पता (वैकल्पिक)" else "Shop address (optional)"
    fun addressPlaceholder(isHindi: Boolean) = if (isHindi) "उदा. 12 एमजी रोड, मुंबई" else "e.g. 12 MG Road, Andheri East"
    fun descriptionLabel(isHindi: Boolean) = if (isHindi) "दुकान का विवरण" else "Description"
    fun paymentsTitle(isHindi: Boolean) = if (isHindi) "भुगतान" else "Payments"
    fun upiExplanation(isHindi: Boolean) = if (isHindi) {
        "आपकी UPI ID आपके कोटेशन और स्वीकार किए गए ऑफ़र के साथ भेजी जाती है ताकि ग्राहक QR कोड या UPI से भुगतान कर सकें। नकद के लिए खाली छोड़ें।"
    } else {
        "Your UPI ID goes with your quotes and accepted offers, so customers can pay you by UPI (with a QR code too). Leave it empty to be paid in cash."
    }
    fun upiIdLabel(isHindi: Boolean) = if (isHindi) "UPI ID (वैकल्पिक)" else "UPI ID (optional)"
    fun upiInvalidError(isHindi: Boolean) = if (isHindi) "यह वैध UPI ID नहीं लग रही है, उदा. name@okhdfcbank" else "That doesn't look like a UPI ID, e.g. name@okhdfcbank"
    fun customersPayTo(isHindi: Boolean) = if (isHindi) "ग्राहक इस पर भुगतान करेंगे" else "Customers pay to"
    fun shopLocationTitle(isHindi: Boolean) = if (isHindi) "दुकान का स्थान" else "Shop location"
    fun shopLocationDesc(city: String, isHindi: Boolean) = if (isHindi) {
        "मानचित्र को ऐसे घुमाएं कि पिन आपकी दुकान पर हो, फिर \"पिन पर स्थान सेट करें\" दबाएं। $city के ग्राहकों को आपकी प्रोफाइल पर यह स्थान दिखेगा।"
    } else {
        "Move the map so the pin sits on your shop, then tap \"Set location\". Customers in $city see this pin on your profile."
    }
    fun setLocationToPin(isHindi: Boolean) = if (isHindi) "पिन पर स्थान सेट करें" else "Set location to the pin"
    fun shopLocationSet(lat: Double, lng: Double, isHindi: Boolean) = if (isHindi) {
        "दुकान का स्थान सेट है (%.5f, %.5f)".format(lat, lng)
    } else {
        "Shop location set (%.5f, %.5f)".format(lat, lng)
    }
    fun noShopLocationSet(isHindi: Boolean) = if (isHindi) "दुकान का स्थान अभी सेट नहीं किया गया।" else "No shop location set yet."
    fun brochurePhotosTitle(isHindi: Boolean) = if (isHindi) "दुकान व कैटलॉग के फोटो" else "Brochure & catalogue photos"
    fun saveProfile(isHindi: Boolean) = if (isHindi) "प्रोफाइल सहेजें" else "Save profile"
    fun saving(isHindi: Boolean) = if (isHindi) "सहेजा जा रहा है…" else "Saving"
    fun couldntSave(isHindi: Boolean) = if (isHindi) "सहेज नहीं सके" else "Couldn't save"
    fun tryAgain(isHindi: Boolean) = if (isHindi) "पुनः प्रयास करें" else "Try again"
    fun close(isHindi: Boolean) = if (isHindi) "बंद करें" else "Close"
    fun cancel(isHindi: Boolean) = if (isHindi) "रद्द करें" else "Cancel"
    fun edit(isHindi: Boolean) = if (isHindi) "बदलें" else "Edit"
    fun delete(isHindi: Boolean) = if (isHindi) "हटाएं" else "Delete"
    fun save(isHindi: Boolean) = if (isHindi) "सहेजें" else "Save"

    // --- Action buttons & Listing actions
    fun markAsSold(isHindi: Boolean) = if (isHindi) "बिक गया" else "Mark as sold"
    fun markUnavailable(isHindi: Boolean) = if (isHindi) "अनुपलब्ध करें" else "Mark unavailable"
    fun markAvailable(isHindi: Boolean) = if (isHindi) "उपलब्ध करें" else "Mark available"
    fun deleteListingTitle(isHindi: Boolean) = if (isHindi) "लिस्टिंग हटाएं?" else "Delete listing?"
    fun deleteListingConfirm(title: String, isHindi: Boolean) = if (isHindi) {
        "\"$title\" और इसके सभी फोटो हमेशा के लिए हटा दिए जाएंगे।"
    } else {
        "\"$title\" and its photos will be removed permanently."
    }
    fun soldAndClosedTab(count: Int, isHindi: Boolean) = if (isHindi) "बिक चुके / बंद ($count)" else "Sold & closed ($count)"
    fun nothingSoldYet(isHindi: Boolean) = if (isHindi) "अभी तक कुछ नहीं बिका" else "Nothing sold yet"
    fun nothingSoldYetSub(isHindi: Boolean) = if (isHindi) "बिक चुके और बंद सामान यहाँ दिखाई देंगे।" else "Sold and closed listings will show up here."
    fun itemsForSaleOrExchangeSub(isHindi: Boolean) = if (isHindi) {
        "बिक्री या एक्सचेंज के लिए रखी गई वस्तुएं यहाँ दिखाई देंगी।"
    } else {
        "Items you put up for sale or exchange appear here."
    }

    // --- Common filters
    fun filterAll(isHindi: Boolean) = if (isHindi) "सभी" else "All"
    fun filterActive(isHindi: Boolean) = if (isHindi) "सक्रिय" else "Active"
    fun filterClosed(isHindi: Boolean) = if (isHindi) "समाप्त" else "Closed"
    fun showAll(isHindi: Boolean) = if (isHindi) "सभी दिखाएं" else "Show all"
    fun tryDifferentFilter(isHindi: Boolean) = if (isHindi) "दूसरा फ़िल्टर चुनकर देखें।" else "Try a different filter."

    // --- Exchanges & Purchases Details
    fun noIncomingRequests(isHindi: Boolean) = if (isHindi) "कोई नया अनुरोध नहीं आया है" else "No incoming requests"
    fun noIncomingRequestsSub(isHindi: Boolean) = if (isHindi) {
        "जब कोई आपके सामान के लिए अनुरोध या ऑफ़र भेजेगा, तो वह यहाँ दिखाई देगा।"
    } else {
        "When someone offers a swap for one of your exchange items, it shows up here."
    }
    fun noOutgoingRequests(isHindi: Boolean) = if (isHindi) "कोई अनुरोध नहीं भेजा गया" else "No requests sent"
    fun noOutgoingRequestsSub(isHindi: Boolean) = if (isHindi) {
        "एक्सचेंज का प्रस्ताव भेजें और यहाँ उसकी स्थिति देखें।"
    } else {
        "Propose an exchange and you can follow it here."
    }
    fun newExchange(isHindi: Boolean) = if (isHindi) "नया एक्सचेंज" else "New exchange"
    fun noReceivedPurchases(isHindi: Boolean) = if (isHindi) "कोई खरीद अनुरोध नहीं आया" else "No requests received"
    fun noReceivedPurchasesSub(isHindi: Boolean) = if (isHindi) {
        "आपकी लिस्टिंग पर आने वाले खरीद अनुरोध और ऑफ़र यहाँ दिखाई देंगे।"
    } else {
        "Buy requests and offers on your listings show up here."
    }
    fun noSentPurchases(isHindi: Boolean) = if (isHindi) "कोई अनुरोध नहीं भेजा गया" else "No requests sent"
    fun noSentPurchasesSub(isHindi: Boolean) = if (isHindi) {
        "जब आप किसी सामान पर खरीदने या ऑफ़र देने का अनुरोध करेंगे, तो वह यहाँ दिखाई देगा।"
    } else {
        "When you tap Buy or Make offer on a listing, it shows up here."
    }

    // --- Recycling & Repairs
    fun noRecyclingRequestsYet(isHindi: Boolean) = if (isHindi) "अभी कोई रीसाइक्लिंग अनुरोध नहीं है" else "No recycling requests yet"
    fun noRecyclingRequestsVendorSub(isHindi: Boolean) = if (isHindi) {
        "आपको भेजे गए रीसाइक्लिंग अनुरोध यहाँ दिखाई देंगे।"
    } else {
        "Recycling requests addressed to you will show up here."
    }
    fun noRepairRequestsYet(isHindi: Boolean) = if (isHindi) "अभी कोई मरम्मत अनुरोध नहीं है" else "No repair requests yet"
    fun noRepairRequestsVendorSub(isHindi: Boolean) = if (isHindi) {
        "ग्राहकों द्वारा भेजे गए मरम्मत अनुरोध यहाँ दिखाई देंगे।"
    } else {
        "Repair requests from customers will show up here."
    }
    fun earnedThisMonth(isHindi: Boolean) = if (isHindi) "इस महीने की कमाई" else "earned this month"
    fun paymentsReceivedTitle(isHindi: Boolean) = if (isHindi) "प्राप्त हुए भुगतान" else "Payments received"
    fun expenditure(isHindi: Boolean) = if (isHindi) "खर्च" else "Expenditure"
    fun profit(isHindi: Boolean) = if (isHindi) "लाभ" else "Profit"
    fun itemsSoldTitle(isHindi: Boolean) = if (isHindi) "बेची गई वस्तुएं (बिक्री)" else "Items sold"
    fun itemsBoughtTitle(isHindi: Boolean) = if (isHindi) "खरीदी गई वस्तुएं (खर्च)" else "Items bought"
    fun reportNotice(isHindi: Boolean) = if (isHindi) {
        "केवल वही भुगतान गिने जाते हैं जिनकी पुष्टि हुई है। PDF और CSV इसी फोन पर तैयार होते हैं।"
    } else {
        "Only payments confirmed as received are counted. The PDF and CSV are made on this phone; you choose where to send them."
    }
    fun noPaymentsInMonth(month: String, isHindi: Boolean) = if (isHindi) {
        "$month में कोई पुष्ट भुगतान प्राप्त नहीं हुआ।"
    } else {
        "No payments were confirmed as received in $month."
    }
    fun noExpendituresInMonth(month: String, isHindi: Boolean) = if (isHindi) {
        "$month में कोई खरीदारी दर्ज नहीं हुई।"
    } else {
        "No purchases were recorded in $month."
    }
    fun noActivityInMonth(month: String, isHindi: Boolean) = if (isHindi) {
        "$month में कोई बिक्री या खरीदारी दर्ज नहीं हुई।"
    } else {
        "No sales or purchases were recorded in $month."
    }
}

