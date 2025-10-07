package com.chifuzgames.catibox.ads

import android.app.Application
import com.google.android.gms.ads.MobileAds

class CatiboxAddapp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        AdManager.initialize(this)
    }
}