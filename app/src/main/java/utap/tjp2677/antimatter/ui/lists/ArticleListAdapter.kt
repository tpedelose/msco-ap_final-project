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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.FeedItemBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions


class ArticleListAdapter(private val mainViewModel: MainViewModel)
    : ListAdapter<Article, ArticleListAdapter.ViewHolder>(Diff()) {

    private var glideOptions = RequestOptions ()
        .transform(RoundedCorners (20))

    inner class ViewHolder(val articleBinding: FeedItemBinding) :
        RecyclerView.ViewHolder(articleBinding.root) {
            init {
                articleBinding.root.setOnClickListener {
                    val article = mainViewModel.getArticleAt(adapterPosition)
                    mainViewModel.openArticle(it.context, article)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowBinding = FeedItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ViewHolder(rowBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.articleBinding

        binding.author.text = item.author.name
        binding.publication.text = item.publication.name
        binding.title.text = item.title

        item.image?.let {
            Glide.with(binding.root.context)
                .asBitmap() // Try to display animated Gifs and video still
                .load(it)
                .apply(glideOptions)
                .into(binding.heroImage)
        }

    }

    class Diff : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.title == newItem.title
//                    && oldItem.rating == newItem.rating
        }
    }
}


class ArticleItemTouchHelper(context: Context, private val adapter: ArticleListAdapter) : ItemTouchHelper.Callback() {

    private var hapticsTriggered = false
    private var reachedMax = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(0, swipeFlags)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return (1/5f)
    }

    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                          direction: Int) {
        // Code for horizontal swipe.
        Log.d(javaClass.simpleName, "Swipe dir $direction")
        val position = viewHolder.adapterPosition
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        Log.d("THIS", abs(dX).toString())

        val view = viewHolder.itemView
        var xD = dX

        if (!isCurrentlyActive) {
//            recyclerView.setBackgroundColor(Color.TRANSPARENT)
            hapticsTriggered = false

            if (reachedMax) {
                view.x = 0f
            }

        } else if (dX != 0f) {

            val swipeThreshold = this.getSwipeThreshold(viewHolder)
            val maxDX = view.width * swipeThreshold
            xD = when(dX >= 0) {
                true -> min(dX, maxDX)
                false -> max(dX, -maxDX)
            }

            Log.d("HERE", "${abs(dX)} == $maxDX")
            if (abs(dX) >= maxDX) {
                if (view.isHapticFeedbackEnabled && !hapticsTriggered) {
                    // TODO: Try/catch
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    hapticsTriggered = true
                    reachedMax = true
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, xD, dY, actionState, isCurrentlyActive)

        when (isCurrentlyActive) {
            true -> drawIndicators(c, viewHolder)
            else -> c.restore()
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

    private fun drawIndicators(c: Canvas, viewHolder: ViewHolder) {
        val itemView = viewHolder.itemView
        val buttonWidth = itemView.width * 1/5f

        val p = Paint()
        p.color = Color.GREEN

        // Left icon / swipe right -> Save to Read Later
        val left = itemView.left.toFloat()
        val top = itemView.top.toFloat()
        val right = (itemView.left + buttonWidth)
        val bottom = itemView.bottom.toFloat()
        val leftButton = RectF(left, top, right, bottom)
        c.drawRoundRect(leftButton, 32f, 32f, p)
        val iconSave = ResourcesCompat.getDrawable(itemView.context.resources, R.drawable.ic_outline_bookmarks_24, null)
        iconSave?.let {
            val iconLeft = ((buttonWidth - it.intrinsicWidth)/2).toInt()
            val iconRight = (iconLeft + it.intrinsicWidth)
            val iconTop = ((itemView.y + it.intrinsicHeight)/2).toInt()
            val iconBottom = (iconTop + it.intrinsicHeight)

            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            it.draw(c)
        }



        // Right icon / swipe left -> Mark as read
        val iconMarkAsRead = ResourcesCompat.getDrawable(itemView.context.resources, R.drawable.ic_outline_bookmarks_24, null)
        val rightButton = RectF(
            (itemView.right - buttonWidth).toFloat(),
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        c.drawRoundRect(rightButton, 32f, 32f, p)

//        itemView.elevation = 5f
    }
}