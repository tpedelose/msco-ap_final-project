package utap.tjp2677.antimatter.ui.article

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
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
import utap.tjp2677.antimatter.authentication.models.Annotation
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Collection
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
                        val subtext = textView.text.substring(textView.selectionEnd, textView.selectionEnd)
                        createAnnotation(subtext, textView.selectionStart, textView.selectionEnd)
                        true
                    }
                    else -> false
                }
        }

        // Observers
        viewModel.observeOpenedArticle().observe(viewLifecycleOwner) {
            val cid = viewModel.getOpenCollection()?.firestoreID
            viewModel.fetchAnnotations(cid!!, it.firestoreID)
            loadArticle(it)
            binding.article.fullScroll(ScrollView.FOCUS_UP)
        }

        viewModel.observeOpenAnnotations().observe(viewLifecycleOwner) { annotations ->
            val spanText = (binding.content.text as Spannable)

            // Clear any old Annotations, keeping HTML styles
            spanText.getSpans(0, spanText.length, AnnotationSpan::class.java).forEach {
                spanText.removeSpan(it)
            }

            // Add new annotations
            annotations?.forEach {
                val spanner = AnnotationSpan(highlightColor, annotation = it)
                spanner.clickHandler = this::annotationClickCallbackHandler
                spanText.setSpan(spanner, it.start, it.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            binding.content.text = spanText
        }

        viewModel.observeIsPlayingStatus().observe(viewLifecycleOwner) { isPlaying ->
            if (isPlaying && viewModel.openArticleIsLoadedToPlayer()) {
                binding.header.playArticle.text = "Pause"
                binding.header.playArticle.setIconResource(R.drawable.ic_outline_pause_24)
            } else {
                binding.header.playArticle.text = "Listen"
                binding.header.playArticle.setIconResource(R.drawable.ic_outline_play_arrow_24)
            }
        }

        // Listeners
        binding.bottomAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.header.playArticle.setOnClickListener {
            if (viewModel.getIsPlayingStatus() && viewModel.openArticleIsLoadedToPlayer()) {
                viewModel.stopPlaying()
            } else {
                viewModel.getOpenArticle()?.let {
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
        } ?: binding.header.publicationImage.setBackgroundResource(R.drawable.ic_outline_newspaper_24)

        // Todo:  Add placeholders?
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
        // Todo?  Remove certain icons if functions aren't available?
        binding.bottomAppBar.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                    menuInflater.inflate(R.menu.bottom_app_bar_article, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    val article = viewModel.getOpenArticle()
                    return when (menuItem.itemId) {
                        R.id.add -> {true}  // TODO: Handle favorite icon press
                        R.id.more -> {true}  // TODO: Handle more item (inside overflow menu) press
                        R.id.share -> {
                            article?.link?.let {
                                val messageTitle = "${article.title} - ${article.publicationName}"
                                viewModel.shareMessage(binding.root.context, it, messageTitle)
                            }
                            true
                        }
                        R.id.open_in_browser -> {
                            article?.link?.let { viewModel.openInBrowser(binding.root.context, it) }
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

    private fun createAnnotation(text: String, start: Int, end: Int) {
        if (start == end) { return }  // reject when annotation would be zero length

        // Todo? Put collectionId in article?
        val cid = viewModel.getOpenCollection()?.firestoreID
        val aid = viewModel.getOpenArticle()?.firestoreID
        (cid != null && aid != null).let {
            viewModel.addAnnotation(cid!!, aid!!, start, end, text)
        }
    }

    private fun annotationClickCallbackHandler(
        annotation: Annotation, mode: ActionMode, menuItem: MenuItem): Boolean {

        val article: Article? = viewModel.getOpenArticle()

        return when (menuItem.itemId) {
            R.id.annotation_delete -> {
                article ?: return false

                val collection: Collection? = viewModel.getOpenCollection()
                collection ?: return false

                viewModel.deleteAnnotation(
                    collection.firestoreID,
                    article.firestoreID,
                    annotation.firestoreID
                )
                true
            }
            R.id.annotation_share -> {
                annotation.text?.let {
                    viewModel.shareMessage(binding.root.context, it, article?.title)
                }
                true
            }
            else -> false
        }
    }
}

class SelectionCallback(private val textView: TextView, val clickHandler: (TextView, ActionMode?, MenuItem?) -> Boolean)
    : ActionMode.Callback {
    // https://stackoverflow.com/questions/12995439/custom-cut-copy-action-bar-for-edittext-that-shows-text-selection-handles/13004720#13004720
    val TAG = "TextSelectionCallback"

    override fun onCreateActionMode(p0: ActionMode, p1: Menu): Boolean {
        p0.menuInflater?.inflate(R.menu.article_text_selection, p1)
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode, p1: Menu): Boolean {
        // Remove select all?
        p1.removeItem(android.R.id.selectAll)
        return true
    }

    override fun onActionItemClicked(p0: ActionMode, p1: MenuItem): Boolean {
        Log.d(TAG, "onActionItemClicked item=${p1}/${p1.itemId}")
        return clickHandler(textView, p0, p1)
    }

    override fun onDestroyActionMode(p0: ActionMode?) {
    }
}

class AnnotationSpan(private val backgroundColor: Int, private val annotation: Annotation)
    : ClickableSpan()  {

    var clickHandler: ((Annotation, ActionMode, MenuItem) -> Boolean)? = null

    override fun onClick(p0: View) {

        // Skip if we don't have a TextView
        if (p0 !is TextView) { return }

        clickHandler?.let {
            val clickCallback = AnnotationClickCallback() ACC@{ mode, menuItem ->
                return@ACC it(annotation, mode, menuItem)
            }
            val rect = getActionModeExcludeZone(p0)
            clickCallback.startActionMode(p0, rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = backgroundColor
    }

    private fun getActionModeExcludeZone(textView: TextView): Rect {
        // Most of the following was based off of the answer in this StackOverflow post:
        // https://stackoverflow.com/questions/11905486/how-get-coordinate-of-a-clickablespan-inside-a-textview

        val excludeZone = Rect()

        // Get index of span's start/end characters in full text
        val spanned = (textView.text as Spanned)
        val start = spanned.getSpanStart(this)
        val end = spanned.getSpanEnd(this)

        // Get line numbers of text in span
        val startLine: Int = textView.layout.getLineForOffset(start)
        val endLine: Int = textView.layout.getLineForOffset(end)

        // Initially set excludeZone to rect of first line
        textView.getLineBounds(startLine, excludeZone)

        if (startLine != endLine) {  // i.e. If we have multiline text
            val rect2 = Rect()
            textView.getLineBounds(endLine, rect2)

            // Set bottom of excludeZone to include final line of text
            excludeZone.bottom = rect2.bottom
        } else {
            // Set excludeZone left/right based on line start/end characters when single line
            excludeZone.left = (textView.layout.getPrimaryHorizontal(start)
                    + textView.compoundPaddingLeft + textView.scrollX).toInt()
            excludeZone.right = (textView.layout.getPrimaryHorizontal(end)
                    + textView.compoundPaddingLeft + textView.scrollX).toInt()
        }

        return excludeZone
    }
}

class AnnotationClickCallback(val clickHandler: (ActionMode, MenuItem) -> Boolean) : ActionMode.Callback2() {
    // https://medium.com/over-engineering/using-androids-actionmode-e903181f2ee3

    private var TAG = "AnnoationClickCallback"

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
        Log.d(TAG, "onActionItemClicked item=${menuItem}/${menuItem.itemId}")
        mode.finish()
        return clickHandler(mode, menuItem)
    }

    override fun onDestroyActionMode(mode: ActionMode) {
    }

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
        outRect.set(contentLeft, contentTop, contentRight, contentBottom)
    }

    fun startActionMode(view: View, contentLeft: Int = 0, contentTop: Int = 0,
        contentRight: Int = view.width, contentBottom: Int = view.height,
    ) {
        this.contentLeft = contentLeft
        this.contentTop = contentTop
        this.contentRight = contentRight
        this.contentBottom = contentBottom
        view.startActionMode(this, ActionMode.TYPE_FLOATING)
    }
}