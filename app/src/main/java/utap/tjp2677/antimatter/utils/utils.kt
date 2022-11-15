package utap.tjp2677.antimatter.utils

import android.content.res.Resources
import android.util.TypedValue

val Number.toPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics)

val Number.toDp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_PX,
    this.toFloat(),
    Resources.getSystem().displayMetrics)