package com.gardiyan.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * İnce çizgili arayüz simgeleri. Çizimler Lucide simge setinden (ISC lisansı) alınıp
 * Compose vektörüne çevrildi; Material dolu simgelerinden daha sakin bir görünüm verir.
 */
object LimitraIcons {

    private fun lineIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    val Home: ImageVector by lazy {
        lineIcon(
            "home",
            "M15 21 V13 A1 1 0 0 0 14 12 H10 A1 1 0 0 0 9 13 V21",
            "M3 10 A2 2 0 0 1 3.709 8.472 L10.709 2.473 A2 2 0 0 1 13.291 2.473 L20.291 8.472 A2 2 0 0 1 21 10 V19 A2 2 0 0 1 19 21 H5 A2 2 0 0 1 3 19 Z"
        )
    }

    val Shield: ImageVector by lazy {
        lineIcon(
            "shield",
            "M20 13 C20 18 16.5 20.5 12.34 21.95 A1 1 0 0 1 11.67 21.94 C7.5 20.5 4 18 4 13 V6 A1 1 0 0 1 5 5 C7 5 9.5 3.8 11.24 2.28 A1.17 1.17 0 0 1 12.76 2.28 C14.51 3.81 17 5 19 5 A1 1 0 0 1 20 6 Z"
        )
    }

    val Award: ImageVector by lazy {
        lineIcon(
            "award",
            "M15.477 12.89 L16.992 21.416 A0.5 0.5 0 0 1 16.182 21.886 L12.602 19.199 A1 1 0 0 0 11.405 19.199 L7.819 21.885 A0.5 0.5 0 0 1 7.009 21.416 L8.523 12.89",
            "M18 8 A6 6 0 1 1 6 8 A6 6 0 1 1 18 8 Z"
        )
    }

    val Trophy: ImageVector by lazy {
        lineIcon(
            "trophy",
            "M6 9 H4.5 A2.5 2.5 0 0 1 4.5 4 H6",
            "M18 9 H19.5 A2.5 2.5 0 0 0 19.5 4 H18",
            "M4 22 H20",
            "M10 14.66 V17 C10 17.55 9.53 17.98 9.03 18.21 C7.85 18.75 7 20.24 7 22",
            "M14 14.66 V17 C14 17.55 14.47 17.98 14.97 18.21 C16.15 18.75 17 20.24 17 22",
            "M18 2 H6 V9 A6 6 0 0 0 18 9 V2 Z"
        )
    }

    val Flame: ImageVector by lazy {
        lineIcon(
            "flame",
            "M8.5 14.5 A2.5 2.5 0 0 0 11 12 C11 10.62 10.5 10 10 9 C8.928 6.857 9.776 4.946 12 3 C12.5 5.5 14 7.9 16 9.5 C18 11.1 19 13 19 15 A7 7 0 1 1 5 15 C5 13.847 5.433 12.706 6 12 A2.5 2.5 0 0 0 8.5 14.5 Z"
        )
    }

    val Plus: ImageVector by lazy {
        lineIcon("plus", "M5 12 H19", "M12 5 V19")
    }

    val ChevronRight: ImageVector by lazy {
        lineIcon("chevronRight", "M9 18 L15 12 L9 6")
    }

    val Key: ImageVector by lazy {
        lineIcon(
            "key",
            "M2.586 17.414 A2 2 0 0 0 2 18.828 V21 A1 1 0 0 0 3 22 H6 A1 1 0 0 0 7 21 V20 A1 1 0 0 1 8 19 H9 A1 1 0 0 0 10 18 V17 A1 1 0 0 1 11 16 H11.172 A2 2 0 0 0 12.586 15.414 L13.4 14.6 A6.5 6.5 0 1 0 9.4 10.6 Z",
            "M17 7.5 A0.5 0.5 0 1 1 16 7.5 A0.5 0.5 0 1 1 17 7.5 Z"
        )
    }

    val Hourglass: ImageVector by lazy {
        lineIcon(
            "hourglass",
            "M5 22 H19",
            "M5 2 H19",
            "M17 22 V17.828 A2 2 0 0 0 16.414 16.414 L12 12 L7.586 16.414 A2 2 0 0 0 7 17.828 V22",
            "M7 2 V6.172 A2 2 0 0 0 7.586 7.586 L12 12 L16.414 7.586 A2 2 0 0 0 17 6.172 V2"
        )
    }

    val Search: ImageVector by lazy {
        lineIcon("search", "M19 11 A8 8 0 1 1 3 11 A8 8 0 1 1 19 11 Z", "M21 21 L16.65 16.65")
    }
}
