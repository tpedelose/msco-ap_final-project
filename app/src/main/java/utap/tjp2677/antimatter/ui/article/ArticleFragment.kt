package utap.tjp2677.antimatter.ui.article

import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.parseAsHtml
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialContainerTransform
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.databinding.FragmentArticleBinding
import utap.tjp2677.antimatter.utils.toDp
import utap.tjp2677.antimatter.utils.toPx
import kotlin.math.max


//https://www.google.com/search?q=how+fast+does+the+average+person+read
const val wordsPerMinute = 200

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()

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

        sharedElementEnterTransition = MaterialContainerTransform()

        // Interactions
        binding.content.movementMethod = LinkMovementMethod.getInstance()
        binding.content.customSelectionActionModeCallback = SelectionCallback(binding.content)

        // Observers
        viewModel.observeOpenedArticle().observe(viewLifecycleOwner) {
            loadArticle(it)
        }

        // Listeners
        binding.header.playArticle.setOnClickListener { playArticle() }
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
            val imageView = ImageView(this.context)
            glideFetch(it, imageView)
            imageView.setPadding(12.toPx.toInt())
            val position = 0 //binding.contentContainer.childCount - 1
            binding.contentContainer.addView(imageView, position)
        }

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

    private fun playArticle() {

        // https://rtdtwo.medium.com/speech-to-text-and-text-to-speech-with-android-85758ff0f6d3

        // Build the speech
        val article: Article? = viewModel.getOpenedArticle()

        article?.let {
            // Attribution
            var attributionText = ""
            if (it.author.isNotBlank() && it.publicationName.isNotBlank()) {
                attributionText = "${it.author} for ${it.publicationName}"
            } else
                if (it.author.isNotBlank()) {
                attributionText = "From ${it.author}"
            }
            else if (it.publicationName.isNotBlank()) {
                attributionText = "From ${it.publicationName}"
            }

            viewModel.ttsEngine.speak(attributionText, TextToSpeech.QUEUE_ADD, null, "tts-attribution")
            viewModel.ttsEngine.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, "tts-pause")

            // Add article content
//            val textLenMax = TextToSpeech.getMaxSpeechInputLength()
            //TODO: Split text into smaller chunks. By sentence?
            val contentText = Jsoup.clean(article.content, Whitelist.basic()).parseAsHtml().toString()

            contentText.splitToSequence("\n").forEachIndexed { i, text ->
                if (text.isNotBlank()) {
                    viewModel.ttsEngine.speak(text, TextToSpeech.QUEUE_ADD, null, "tts-content$i")
                    viewModel.ttsEngine.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, "tts-content$i pause")
                }
            }
        }
    }

    class SelectionCallback(textView: TextView) : ActionMode.Callback {
        // https://stackoverflow.com/questions/12995439/custom-cut-copy-action-bar-for-edittext-that-shows-text-selection-handles/13004720#13004720
        private val TAG = "TextSelectionCallback"
        private val selectedTextView = textView

        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            p0!!.menuInflater.inflate(R.menu.acticle_text_selection, p1)
            return true
        }

        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            // Remove select all?
            p1?.removeItem(android.R.id.selectAll)
            return true
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            Log.d(TAG, "onActionItemClicked item=${p1.toString()}/${p1?.itemId}")

            val start: Int = selectedTextView.selectionStart
            val end: Int = selectedTextView.selectionEnd

            return when (p1?.itemId) {
                utap.tjp2677.antimatter.R.id.highlight -> {
                    val textToHighlight = selectedTextView.text as Spannable
                    val highlightColor = MaterialColors.getColor(
                        selectedTextView.context, com.google.android.material.R.attr.colorPrimaryContainer, Color.YELLOW)

                    textToHighlight.setSpan(BackgroundColorSpan(highlightColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    selectedTextView.text = textToHighlight
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
        }
    }
}