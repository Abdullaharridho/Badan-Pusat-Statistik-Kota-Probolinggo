package com.example.bpskota.tracking

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.tracking.ActivityTracker

class BpsKotaApplication : Application() {

    private lateinit var activityTracker: ActivityTracker

    override fun onCreate() {
        super.onCreate()

        activityTracker = ActivityTracker(
            this,
            BpskpRetrofitClient.api
        )

        ProcessLifecycleOwner
            .get()
            .lifecycle
            .addObserver(
                object : DefaultLifecycleObserver {

                    override fun onStart(owner: LifecycleOwner) {
                        activityTracker.trackAppOpen()
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        activityTracker.trackAppClose()
                    }
                }
            )
    }
}