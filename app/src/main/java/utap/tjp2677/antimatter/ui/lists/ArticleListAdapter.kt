package utap.tjp2677.antimatter.ui.lists

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.databinding.FeedItemBinding
import utap.tjp2677.antimatter.utils.toDp
import utap.tjp2677.antimatter.utils.toPx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class ArticleListAdapter(private val viewModel: MainViewModel, val onClickCallback: (Article) -> Unit)
    : ListAdapter<Article, ArticleListAdapter.ViewHolder>(Diff()) {

    private var glideOptions = RequestOptions()
        .fitCenter()
        .transform(
            // Todo:  Find a way to do this in XML styles. Inconsistent between devices.
            RoundedCorners (20.toDp.toInt())
        )

    inner class ViewHolder(val articleBinding: FeedItemBinding) :
        RecyclerView.ViewHolder(articleBinding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowBinding = FeedItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ViewHolder(rowBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.articleBinding

        binding.author.text = item.author//.name
        binding.publication.text = item.publicationName
        binding.title.text = item.title

        item.image?.let {
            Glide.with(binding.root.context)
                .asBitmap() // Try to display animated Gifs and video still
                .load(it)
                .apply(glideOptions)
                .into(binding.heroImage)
        }

        binding.root.setOnClickListener {
            val article = viewModel.getArticleAt(position)
            onClickCallback(article)
        }
    }

    class Diff : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.title == newItem.title
        }
    }
}


class ArticleItemTouchHelper(context: Context, private val adapter: ArticleListAdapter) : ItemTouchHelper.Callback() {

    /* Resources:
        - https://stackoverflow.com/questions/44965278/recyclerview-itemtouchhelper-buttons-on-swipe
        - https://developer.android.com/reference/kotlin/androidx/recyclerview/widget/ItemTouchHelper.Callback#onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder,int)
        - https://www.digitalocean.com/community/tutorials/android-recyclerview-swipe-to-delete-undo
        - https://developer.android.com/reference/androidx/constraintlayout/motion/widget/OnSwipe#getAutoCompleteMode()
     */

    private var hapticsTriggered = false
    private var reachedMax = false
    private val buttonWidth = 80.toPx // itemView.width * 1/5f
    private val buttonBaseColor = com.google.android.material.R.attr.colorPrimarySurface
    private val buttonLeftColor = Color.HSVToColor(floatArrayOf(94f, 0.4f, 95f))

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
    ): Int {
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(0, swipeFlags)
    }

    override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        return (buttonWidth/viewHolder.itemView.width).toFloat()
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return -1f // Disable (for now)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val view = viewHolder.itemView
        val swipeThreshold = this.getSwipeThreshold(viewHolder)
        val maxDX = view.width * swipeThreshold

        if ((!isCurrentlyActive || hapticsTriggered) && abs(dX) < maxDX) {
            hapticsTriggered = false
        }

        if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            val xD = when(dX >= 0) {
                true -> min(dX, maxDX)
                false -> max(dX, -maxDX)
            }

            Log.d("Displacement Magnitude", "${abs(dX)} (max: $maxDX)")

            if (abs(dX) >= maxDX && view.isHapticFeedbackEnabled && !hapticsTriggered) {
                // TODO: Try/catch
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                hapticsTriggered = true
                reachedMax = true
            }

            when {
                dX < 0 -> {
                    Log.d("SWIPE TO LEFT", dX.toString())
                    drawIndicatorRight(c, viewHolder)
                }
                dX > 0 -> {
                    Log.d("SWIPE TO RIGHT", dX.toString())
                    drawIndicatorLeft(c, viewHolder)
                }
                else -> c.restore()
            }

            super.onChildDraw(c, recyclerView, viewHolder, xD, dY, actionState, isCurrentlyActive)
        }

    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        // https://codeburst.io/android-swipe-menu-with-recyclerview-8f28a235ff28
        if (reachedMax) {
            reachedMax = false
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    private fun drawIndicatorLeft(c: Canvas, viewHolder: ViewHolder) {
        val itemView = viewHolder.itemView

        val indicator = RectF(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            (itemView.left + buttonWidth).toFloat(),
            itemView.bottom.toFloat()
        )
        val indicatorPaint = Paint()
        indicatorPaint.color = buttonBaseColor
        c.drawRect(indicator, indicatorPaint)

        // Create icon
        val iconSave = ResourcesCompat.getDrawable(itemView.context.resources, R.drawable.ic_outline_bookmarks_24, null)
        iconSave?.let {
            val buttonHeight = itemView.bottom - itemView.top
            val iconHeight = it.intrinsicHeight*1.05
            val iconWidth = it.intrinsicWidth*1.05

            val iconLeft = (itemView.left + (buttonWidth/2 - iconWidth/2)).toInt()
            val iconRight = (iconLeft + iconWidth).toInt()
            val iconTop = (itemView.top + (buttonHeight/2 - iconHeight/2)).toInt()
            val iconBottom = (iconTop + iconHeight).toInt()

            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            it.draw(c)
        }
    }

    private fun drawIndicatorRight(c: Canvas, viewHolder: ViewHolder) {
        val itemView = viewHolder.itemView

        val p = Paint()
        p.color = buttonBaseColor

        // Create colored container
        val indicator = RectF(
            (itemView.right - buttonWidth).toFloat(),
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        val indicatorPaint = Paint()
        indicatorPaint.color = buttonBaseColor
        c.drawRect(indicator, indicatorPaint)

        // Create icon
        val iconSave = ResourcesCompat.getDrawable(itemView.context.resources, R.drawable.ic_outline_done_24, null)
        iconSave?.let {
            val buttonHeight = itemView.bottom - itemView.top
            val iconHeight = it.intrinsicHeight*1.05
            val iconWidth = it.intrinsicWidth*1.05

            val iconTop = (itemView.top + (buttonHeight/2 - iconHeight/2)).toInt()
            val iconBottom = (iconTop + iconHeight).toInt()
            val iconRight = (itemView.right - (buttonWidth/2 - iconWidth/2)).toInt()
            val iconLeft = (iconRight - iconWidth).toInt()

            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            it.draw(c)
        }
    }
}