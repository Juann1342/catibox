package com.chifuzgames.catibox.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.chifuzgames.catibox.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private val BANNER_ID = BuildConfig.BANNER_ID
    private val INTERSTITIAL_ID = BuildConfig.INTERSTITIAL_ID
    private val REWARDED_ID = BuildConfig.REWARDED_ID


    private var bannerView: AdView? = null

    /** Mostrar banner de manera asíncrona */
    fun showBanner(context: Context, container: LinearLayout) {
        try {
            if (bannerView == null) {
                bannerView = AdView(context).apply {
                    adUnitId = BANNER_ID
                    setAdSize(AdSize.BANNER)
                }

                bannerView?.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // no hacer toast para no molestar al usuario
                    }
                }

                bannerView?.loadAd(AdRequest.Builder().build())
            }

            bannerView?.let { banner ->
                if (banner.parent != null) (banner.parent as ViewGroup).removeView(banner)
                container.addView(banner)
            }
        } catch (_: Exception) {
            // Ignorar cualquier error silenciosamente
        }
    }

    fun destroyBanner() {
        try {
            bannerView?.destroy()
            bannerView = null
        } catch (_: Exception) {}
    }

    /** Mostrar interstitial de manera segura y asíncrona */
    fun showInterstitial(
        activity: Activity,
        onAdClosed: () -> Unit,
        onAdUnavailable: (() -> Unit)? = null
    ) {
        try {
            InterstitialAd.load(activity, INTERSTITIAL_ID, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() { onAdClosed() }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) { onAdClosed() }
                        }
                        try { ad.show(activity) } catch (_: Exception) { onAdClosed() }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onAdUnavailable?.invoke()
                    }
                })
        } catch (_: Exception) {
            onAdUnavailable?.invoke()
        }
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdUnavailable: (() -> Unit)? = null
    ) {
        try {
            RewardedAd.load(activity, REWARDED_ID, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() { /* nada */ }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) { /* nada */ }
                        }
                        try { ad.show(activity) { _: RewardItem -> onRewardEarned() } } catch (_: Exception) {}
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onAdUnavailable?.invoke()
                    }
                })
        } catch (_: Exception) {
            onAdUnavailable?.invoke()
        }
    }

}
