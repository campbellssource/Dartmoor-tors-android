package com.dartmoortors.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.dartmoortors.ui.theme.Green
import com.dartmoortors.ui.theme.Orange
import com.dartmoortors.ui.theme.Teal
import android.graphics.Color as AndroidColor

/**
 * Pre-rendered, cached marker icons for tors (T2-04).
 *
 * Rendering ~930 tors as individual Compose `MarkerComposable`s is expensive (each
 * composes a UI tree and rasterises it), which is why clustering used to be required.
 * Instead we render each distinct pin appearance to a [BitmapDescriptor] exactly once
 * and reuse it across all native `Marker`s — cheap enough that clustering is unnecessary.
 */
class TorMarkerIcons(
    val visited: BitmapDescriptor,
    val accessible: BitmapDescriptor,
    val notAccessible: BitmapDescriptor,
    val notInCollection: BitmapDescriptor,
    val selectedOutOfCollection: BitmapDescriptor,
) {
    /** Pick the icon for a tor based on its state (mirrors the previous colour logic). */
    fun iconFor(item: TorMapItem): BitmapDescriptor = when {
        !item.isInActiveCollection -> notInCollection
        item.isVisited -> visited
        item.isAccessible -> accessible
        else -> notAccessible
    }
}

/**
 * Build and remember the tor marker icon set once per composition. The bitmaps are
 * density-correct and depend only on fixed theme colours, so they never need rebuilding.
 */
@Composable
fun rememberTorMarkerIcons(): TorMarkerIcons {
    val context = LocalContext.current
    return remember {
        TorMarkerIcons(
            visited = circleDescriptor(context, Green.toArgb(), showCheck = true),
            accessible = circleDescriptor(context, Teal.toArgb(), showCheck = false),
            notAccessible = circleDescriptor(context, Orange.toArgb(), showCheck = false),
            notInCollection = circleDescriptor(context, AndroidColor.GRAY, showCheck = false),
            selectedOutOfCollection = circleDescriptor(
                context, AndroidColor.GRAY, showCheck = false, diameterDp = 22f, borderDp = 2f
            ),
        )
    }
}

private fun circleDescriptor(
    context: Context,
    fillColor: Int,
    showCheck: Boolean,
    diameterDp: Float = 18f,
    borderDp: Float = 1.5f,
): BitmapDescriptor =
    BitmapDescriptorFactory.fromBitmap(
        createCircleMarkerBitmap(context, fillColor, showCheck, diameterDp, borderDp)
    )

/**
 * Draw a filled circle with a white border (and optional white check) to a bitmap,
 * reproducing the look of the previous Compose marker.
 */
private fun createCircleMarkerBitmap(
    context: Context,
    fillColor: Int,
    showCheck: Boolean,
    diameterDp: Float,
    borderDp: Float,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (diameterDp * density).toInt().coerceAtLeast(1)
    val border = borderDp * density
    val radius = size / 2f

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(radius, radius, radius - border / 2f, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = border
    }
    canvas.drawCircle(radius, radius, radius - border / 2f, borderPaint)

    if (showCheck) {
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(size * 0.30f, size * 0.52f)
            lineTo(size * 0.44f, size * 0.66f)
            lineTo(size * 0.70f, size * 0.36f)
        }
        canvas.drawPath(path, checkPaint)
    }

    return bitmap
}
