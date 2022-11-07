package utap.tjp2677.antimatter.utils

import android.content.res.Resources
import android.util.TypedValue

class utils {

    val Number.toPx get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics)

}