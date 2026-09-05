package com.narmeshnigam.a1remote.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The one [KeyMap] the process uses.
 *
 * The service, the view model and Key Lab all have to agree on what a key means at the same
 * instant, and the service outlives any screen, so the map is process-wide rather than owned by
 * a view model.
 */
object KeyMaps {
    @Volatile
    private var instance: KeyMap? = null

    fun get(context: Context): KeyMap = instance ?: synchronized(this) {
        instance ?: KeyMap(
            store = DataStoreKeyMapStore(context.applicationContext),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ).also { instance = it }
    }
}
