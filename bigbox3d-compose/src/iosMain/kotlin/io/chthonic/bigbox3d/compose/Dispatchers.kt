package io.chthonic.bigbox3d.compose

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Dispatchers.IO does not exist on Kotlin/Native — use Default (backed by a thread pool)
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
