package io.chthonic.gamebigbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.chthonic.bigbox3d.compose.BigBox3DProgress
import io.chthonic.bigbox3d.compose.BoxRawImages

// Controls whether a pool slot's BigBox3DProgress is visible (spinning) or paused.
// False in parking spots; true when assigned to a loading item. The movableContentOf
// content reads this from whichever CompositionLocalProvider it currently lives in,
// so visibility flips automatically as the slot moves between locations.
private val LocalPoolSlotVisible = compositionLocalOf { false }

/**
 * Holds the state for a pool of [BigBox3DProgress] spinners that are shared across
 * loading items. Obtain an instance via [rememberBigBox3DProgressPool].
 */
class BigBox3DProgressPool internal constructor(
    internal val pool: List<@Composable () -> Unit>?,
    internal val loadingSet: Set<Int>,
    internal val assignments: Map<Int, Int>,
    private val setLoading: (Int, Boolean) -> Unit,
) {
    /** Returns the [BigBox3D.onLoadingChange] callback for the item at [idx]. */
    fun onLoadingChange(idx: Int): (Boolean) -> Unit = { loading ->
        setLoading(idx, loading)
    }
}

/**
 * Creates and remembers a [BigBox3DProgressPool] backed by [textures].
 *
 * The pool keeps [poolSize] [BigBox3DProgress] instances permanently in composition
 * (atlas always warm) and assigns them to whichever items are currently loading.
 * Call [BigBox3DProgressPool.ParkingSpots] once above your list to give idle slots
 * a stable home, and [BigBox3DProgressPool.LoadingOverlay] inside each list item.
 */
@Composable
fun rememberBigBox3DProgressPool(
    textures: BoxRawImages?,
    poolSize: Int = 3,
): BigBox3DProgressPool {
    val pool = remember(textures) {
        val t = textures ?: return@remember null
        List(poolSize) {
            movableContentOf {
                BigBox3DProgress(
                    textures = t,
                    visible = LocalPoolSlotVisible.current,
                    size = 140.dp,
                )
            }
        }
    }
    var loadingSet by remember { mutableStateOf(emptySet<Int>()) }
    val assignments = if (pool != null)
        loadingSet.sorted().take(pool.size).mapIndexed { i, idx -> idx to i }.toMap()
    else emptyMap()

    return BigBox3DProgressPool(
        pool = pool,
        loadingSet = loadingSet,
        assignments = assignments,
        setLoading = { idx, loading ->
            loadingSet = if (loading) loadingSet + idx else loadingSet - idx
        },
    )
}

/**
 * Renders invisible parking spots for idle pool slots.
 *
 * Must be placed in the **same composition scope** as the call to
 * [rememberBigBox3DProgressPool] — not inside a `SubcomposeLayout` (e.g. LazyColumn).
 * This ensures the move from parking → item overlay is atomic within one frame.
 */
@Composable
fun BigBox3DProgressPool.ParkingSpots() {
    val p = pool ?: return
    CompositionLocalProvider(LocalPoolSlotVisible provides false) {
        for (slot in p.indices) {
            // Each slot lives here when idle (visible=false, 0dp, render loop paused).
            Box(Modifier.size(0.dp)) {
                if (slot !in assignments.values) p[slot]()
            }
        }
    }
}

/**
 * Shows the loading overlay for the item at [idx]: a pool slot if one is available,
 * otherwise a plain [CircularProgressIndicator]. Shows nothing when not loading.
 *
 * Call this as a sibling to [BigBox3D] inside a `Box` so it appears as an overlay.
 */
@Composable
fun BigBox3DProgressPool.LoadingOverlay(idx: Int) {
    if (idx !in loadingSet) return
    val poolSlot = assignments[idx]
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (poolSlot != null && pool != null) {
            CompositionLocalProvider(LocalPoolSlotVisible provides true) {
                pool[poolSlot]()
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
