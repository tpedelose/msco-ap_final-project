package utap.tjp2677.antimatter.ui.article

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.color.MaterialColors
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.databinding.FragmentArticleBinding
import utap.tjp2677.antimatter.utils.toDp
import kotlin.math.max


//https://www.google.com/search?q=how+fast+does+the+average+person+read
const val wordsPerMinute = 200

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()

    private var highlightColor: Int = -1
    private var textColor: Int = -1


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMenuProvider()
        initColors(binding.root.context)

        // Interactions
        binding.content.movementMethod = LinkMovementMethod.getInstance()
        binding.content.customSelectionActionModeCallback = SelectionCallback(binding.content) {
            textView, actionMode, menuItem -> Boolean
                return@SelectionCallback when (menuItem?.itemId) {
                    R.id.highlight -> {
                        createAnnotation(textView, textView.selectionStart, textView.selectionEnd)
                        true
                    }
                    else -> false
                }
        }

        // Observers
        viewModel.observeOpenedArticle().observe(viewLifecycleOwner) {
            viewModel.fetchAnnotations()
            loadArticle(it)
        }

        viewModel.observeIsPlayingStatus().observe(viewLifecycleOwner) { isPlaying ->
            when (isPlaying && viewModel.openArticleIsLoadedToPlayer()) {
                true -> {
                    binding.header.playArticle.text = "Pause"
                    binding.header.playArticle.setIconResource(R.drawable.ic_outline_pause_24)
                }
                false -> {
                    binding.header.playArticle.text = "Listen"
                    binding.header.playArticle.setIconResource(R.drawable.ic_outline_play_arrow_24)
                }
            }
        }

        viewModel.observeOpenAnnotations().observe(viewLifecycleOwner) { annotations ->
            Log.d("Annotations", annotations.toString())
            if (annotations == null) { return@observe }

            val spanned = (binding.content.text as Spannable)
            annotations.forEach {
                spanned.setSpan(AnnotationSpan(highlightColor), it.start, it.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            binding.content.text = spanned
        }

        // Listeners
        binding.bottomAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.header.playArticle.setOnClickListener {
            if (viewModel.getIsPlayingStatus() && viewModel.openArticleIsLoadedToPlayer()) {
                viewModel.stopPlaying()
            } else {
                viewModel.getOpenedArticle()?.let {
                    viewModel.setNowPlaying(it)
                    viewModel.playArticle()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadArticle(article: Article) {
        // Fetch images
        article.publicationIconLink?.let { logo: String ->
            glideFetch(logo, binding.header.publicationImage)
        }

        article.image?.let {
            glideFetch(it, binding.heroImage)
        } ?: binding.heroImage.setImageDrawable(null)

        // Set text
        binding.header.title.text = article.title
        binding.header.authorName.text = article.author
        binding.header.publicationName.text = article.publicationName

        val articleContent = Html.fromHtml(article.content, Html.FROM_HTML_MODE_LEGACY)
        binding.content.text = articleContent

        // Todo: Precompute?
        val wordCount = articleContent.count { str -> str in " "}
        binding.header.wordCount.text = "$wordCount words"
        val readTime = max(1, (wordCount / wordsPerMinute.toFloat()).toInt())
        binding.header.readTime.text = "$readTime min"
    }

    private fun initMenuProvider () {
        binding.bottomAppBar.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                    menuInflater.inflate(R.menu.bottom_app_bar_article, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.add -> {true}  // TODO: Handle favorite icon press
                        R.id.more -> {true}  // TODO: Handle more item (inside overflow menu) press
                        R.id.open_in_browser -> {
                            // Handle open in browser
                            val article = viewModel.getOpenedArticle()
                            Log.d("URL", "${article?.link}")
                            article?.link?.let { url: String -> openWebPage(url) }
                            true
                        }
                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initColors (context: Context) {
        highlightColor = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorPrimaryContainer, Color.YELLOW)
        // WARN: Text color can look weird w/ dynamic colors
        textColor = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK)
    }

    private fun glideFetch(url: String?, imageView: ImageView) {
        val glideOptions = RequestOptions()
            .fitCenter()
            .transform(
                RoundedCorners(20.toDp.toInt())
            )

        Glide.with(imageView.context)
            .asBitmap() // Try to display animated Gifs and video still
            .load(url)
            .apply(glideOptions)
            .into(imageView)
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (activity?.let { intent.resolveActivity(it.packageManager) } != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this.context, "No browsers installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAnnotation(textView: TextView, start: Int, end: Int) {
        if (start == end) { return }  // reject

        val spanner = AnnotationSpan(highlightColor)
        textView.text = (textView.text as Spannable).apply {
            this.setSpan(spanner, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        viewModel.getOpenedArticle()?.firestoreID?.let {
            viewModel.addAnnotation(it, start, end)
        }

    }

}

class SelectionCallback(private val textView: TextView, val clickHandler: (TextView, ActionMode?, MenuItem?) -> Boolean)
    : ActionMode.Callback {
    // https://stackoverflow.com/questions/12995439/custom-cut-copy-action-bar-for-edittext-that-shows-text-selection-handles/13004720#13004720

    override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
        p0?.menuInflater?.inflate(R.menu.article_text_selection, p1)
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
        // Remove select all?
        p1?.removeItem(android.R.id.selectAll)
        return true
    }

    override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
        Log.d("TextSelectionCallback", "onActionItemClicked item=${p1.toString()}/${p1?.itemId}")
        return clickHandler(textView, p0, p1)
    }

    override fun onDestroyActionMode(p0: ActionMode?) {
    }
}


class AnnotationSpan(private val backgroundColor: Int, private val textColor: Int? = null)
    : ClickableSpan()  {

    val TAG = "Annotation Span"

    override fun onClick(p0: View) {
        // https://stackoverflow.com/questions/11905486/how-get-coordinate-of-a-clickablespan-inside-a-textview
//            TODO("Not yet implemented")
        Log.d(TAG, "Clicky!")

        if (p0 is TextView) {
            (p0.text as Spanned).let {
                val start = it.getSpanStart(this)
                val end = it.getSpanEnd(this)
                Log.d(TAG, "$start, $end")

                val xMin: Float = p0.layout.getPrimaryHorizontal(start)
                val xMax: Float = p0.layout.getPrimaryHorizontal(end)
                Log.d(TAG, "$xMin, $xMax")

                val lineStartOffset = p0.layout.getLineForOffset(start)
                val lineEndOffset = p0.layout.getLineForOffset(end)

                val f = AnnotationClickCallback()
                f.startActionMode(p0, contentLeft = xMin.toInt(), contentRight = xMax.toInt())

            }
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = backgroundColor
        textColor?.let {
            ds.color = textColor
        }
    }
}

interface OnActionItemClickListener {
    fun onActionItemClick(item: MenuItem)
}

class AnnotationClickCallback : ActionMode.Callback2() {
    // https://medium.com/over-engineering/using-androids-actionmode-e903181f2ee3

    var onActionItemClickListener: OnActionItemClickListener? = null

    private var mode: ActionMode? = null
    @MenuRes private var menuResId: Int = 0
    private var contentLeft: Int = 0
    private var contentTop: Int = 0
    private var contentRight: Int = 0
    private var contentBottom: Int = 0

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater?.inflate(R.menu.article_annotation_click, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.annotation_delete -> {}
            R.id.annotation_share -> {}
        }
//        mode.finish()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
    }

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
        outRect.set(contentLeft, contentTop, contentRight, contentBottom)
    }

    fun startActionMode(
        view: View,
//                        @MenuRes menuResId: Int,
        contentLeft: Int = 0,
        contentTop: Int = 0,
        contentRight: Int = view.width,
        contentBottom: Int = view.height,
    ) {
//        this.menuResId = menuResId
        this.contentLeft = contentLeft
        this.contentTop = contentTop
        this.contentRight = contentRight
        this.contentBottom = contentBottom
        view.startActionMode(this, ActionMode.TYPE_FLOATING)
    }
}