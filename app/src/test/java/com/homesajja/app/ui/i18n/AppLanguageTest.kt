package com.homesajja.app.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun appLanguage_fromStored_handlesVariousInputs() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("unknown"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("ENGLISH"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("English"))

        assertEquals(AppLanguage.HINDI, AppLanguage.fromStored("hi"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromStored("HINDI"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromStored("Hindi"))
    }

    @Test
    fun appLanguage_isHindi_flag() {
        assertTrue(AppLanguage.HINDI.isHindi)
        assertFalse(AppLanguage.ENGLISH.isHindi)
    }

    @Test
    fun vendorStrings_navigationTranslations_areDistinctAndNonEmpty() {
        assertNotEquals(VendorStrings.dashboard(false), VendorStrings.dashboard(true))
        assertEquals("डैशबोर्ड", VendorStrings.dashboard(true))

        assertNotEquals(VendorStrings.listings(false), VendorStrings.listings(true))
        assertEquals("लिस्टिंग्स", VendorStrings.listings(true))

        assertNotEquals(VendorStrings.requests(false), VendorStrings.requests(true))
        assertEquals("अनुरोध", VendorStrings.requests(true))

        assertNotEquals(VendorStrings.explore(false), VendorStrings.explore(true))
        assertEquals("मार्केट", VendorStrings.explore(true))

        assertNotEquals(VendorStrings.profile(false), VendorStrings.profile(true))
        assertEquals("प्रोफाइल", VendorStrings.profile(true))

        assertNotEquals(VendorStrings.logOut(false), VendorStrings.logOut(true))
        assertEquals("लॉग आउट", VendorStrings.logOut(true))
    }

    @Test
    fun vendorStrings_dashboardCards_areLocalized() {
        assertEquals("कुल कमाई", VendorStrings.totalEarned(true))
        assertEquals("इस महीने", VendorStrings.thisMonth(true))
        assertEquals("आपका कार्य", VendorStrings.yourWork(true))
        assertEquals("पूरा हुआ", VendorStrings.completed(true))
        assertEquals("लंबित", VendorStrings.pending(true))
        assertEquals("बिक्री", VendorStrings.sales(true))
        assertEquals("मरम्मत", VendorStrings.repairs(true))
        assertEquals("एक्सचेंज", VendorStrings.exchanges(true))
        assertEquals("रीसाइक्लिंग", VendorStrings.recycling(true))
        assertEquals("होम सज्जा में आपका स्वागत है", VendorStrings.welcomeTitle(true))
    }

    @Test
    fun vendorStrings_businessTypes_areLocalizedInHindi() {
        assertEquals("मरम्मत विशेषज्ञ", VendorStrings.businessType("REPAIR", true))
        assertEquals("मरम्मत विशेषज्ञ", VendorStrings.businessType("REPAIR_PROFESSIONAL", true))
        assertEquals("रीसाइक्लिंग विशेषज्ञ", VendorStrings.businessType("RECYCLER", true))
        assertEquals("कारीगर / अपसाइक्लर", VendorStrings.businessType("ARTISAN", true))
        assertEquals("फर्नीचर विक्रेता", VendorStrings.businessType("VENDOR", true))
        assertEquals("विक्रेता", VendorStrings.businessType("OTHER", true))
        assertEquals("विक्रेता", VendorStrings.businessType(null, true))

        assertEquals("Recycler", VendorStrings.businessType("RECYCLER", false))
        assertEquals("Vendor", VendorStrings.businessType(null, false))
    }

    @Test
    fun vendorStrings_requestsAndPickups_areLocalized() {
        assertEquals("खरीद", VendorStrings.purchases(true))
        assertEquals("एक्सचेंज", VendorStrings.exchangesTab(true))
        assertEquals("मरम्मत", VendorStrings.repairsTab(true))
        assertEquals("रीसाइक्लिंग", VendorStrings.recyclingTab(true))

        assertEquals("उपलब्ध पिकअप", VendorStrings.openPickups(true))
        assertEquals("मेरे पिकअप", VendorStrings.myPickups(true))
        assertEquals("पिकअप लें (निःशुल्क)", VendorStrings.claimFree(true))
        assertEquals("कोटेशन भेजकर लें", VendorStrings.claimWithQuote(true))
        assertEquals("ग्राहक: रमेश", VendorStrings.fromCustomer("रमेश", true))
        assertEquals("लकड़ी फर्नीचर", VendorStrings.furnitureItem("लकड़ी", true))
    }

    @Test
    fun vendorStrings_listingsAndActions_areLocalized() {
        assertEquals("सक्रिय (3)", VendorStrings.activeTab(3, true))
        assertEquals("बिक चुके / बंद (2)", VendorStrings.soldAndClosedTab(2, true))
        assertEquals("कोई सक्रिय लिस्टिंग नहीं है", VendorStrings.noActiveListings(true))
        assertEquals("अभी तक कुछ नहीं बिका", VendorStrings.nothingSoldYet(true))
        assertEquals("बदलें", VendorStrings.edit(true))
        assertEquals("हटाएं", VendorStrings.delete(true))
        assertEquals("बिक गया", VendorStrings.markAsSold(true))
        assertEquals("अनुपलब्ध करें", VendorStrings.markUnavailable(true))
        assertEquals("उपलब्ध करें", VendorStrings.markAvailable(true))
    }

    @Test
    fun vendorStrings_monthNames_areLocalized() {
        assertEquals("जन", VendorStrings.monthName("Jan", true))
        assertEquals("फ़र", VendorStrings.monthName("Feb", true))
        assertEquals("मार्च", VendorStrings.monthName("Mar", true))
        assertEquals("अप्रै", VendorStrings.monthName("Apr", true))
        assertEquals("मई", VendorStrings.monthName("May", true))
        assertEquals("जून", VendorStrings.monthName("Jun", true))
        assertEquals("जुला", VendorStrings.monthName("Jul", true))
        assertEquals("अग", VendorStrings.monthName("Aug", true))
        assertEquals("सित", VendorStrings.monthName("Sep", true))
        assertEquals("अक्टू", VendorStrings.monthName("Oct", true))
        assertEquals("नव", VendorStrings.monthName("Nov", true))
        assertEquals("दिस", VendorStrings.monthName("Dec", true))

        assertEquals("Jan", VendorStrings.monthName("Jan", false))
    }
}
