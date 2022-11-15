package utap.tjp2677.antimatter.ui.article

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Html
import android.text.Html.ImageGetter
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.ViewTarget
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.ActivityArticleBinding
import utap.tjp2677.antimatter.utils.toDp
import utap.tjp2677.antimatter.utils.toPx
import kotlin.math.max
import kotlin.math.roundToInt


//https://www.google.com/search?q=how+fast+does+the+average+person+read
const val wordsPerMinute = 200

class ArticleActivity : AppCompatActivity() {

    companion object {
        const val articleKey = "article"
    }

    private lateinit var binding: ActivityArticleBinding
    private val viewModel: MainViewModel by viewModels()
    private var glideOptions = RequestOptions()
        .fitCenter()
        .transform(RoundedCorners (20.toDp.toInt()))
    private val textToSpeechEngine: TextToSpeech by lazy {
        // Pass in context and the listener.
        TextToSpeech(this,
            TextToSpeech.OnInitListener { status ->
                // set our locale only if init was success.
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeechEngine.language = textToSpeechEngine.defaultVoice.locale
                }
            })
    }

    var article: Article? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityOnePostBinding = ActivityArticleBinding.inflate(layoutInflater)
        binding = activityOnePostBinding

        setContentView(binding.root)
        setSupportActionBar(binding.bottomAppBar)
        initActionBar()

        // Todo:  Find a non-deprecated way;  Switch to fragment with ViewModel?
        (intent.extras?.getSerializable(articleKey) as Article).let {
            // Save article for later;  TODO?: move into viewModel
            article = it

            // Fetch images
            it.publication.logo?.let { logo: String ->
                glideFetch(logo, binding.header.publicationImage)
            }

            it.image?.let { imageUrl ->
                val imageView = ImageView(this)
                glideFetch(imageUrl, imageView)
                imageView.setPadding(12.toPx.toInt())
                val position = 0 //binding.contentContainer.childCount - 1
                binding.contentContainer.addView(imageView, position)
            }

            // Text
            binding.header.authorName.text = it.author.name
            binding.header.publicationName.text = it.publication.name
            binding.header.title.text = it.title

            val articleContent = Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY)
//            val articleContent = Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY, GlideImageGetter(this), null)

            Log.d("Helo:", articleContent[0].toString())

            binding.content.text = articleContent
            binding.content.movementMethod = LinkMovementMethod.getInstance();


            Log.d("HTML", articleContent.toString())

            // Todo: Precompute?
            val wordCount = articleContent.count { str -> str in " "}
            binding.header.wordCount.text = "$wordCount words"
            val readTime = max(1, (wordCount / wordsPerMinute.toFloat()).roundToInt())
            binding.header.readTime.text = "$readTime min"
        }


        // Set a text-selection callback
        binding.content.customSelectionActionModeCallback = SelectionCallback(binding.content)

        // Set a listener to play the article
        binding.header.playArticle.setOnClickListener {
            // https://rtdtwo.medium.com/speech-to-text-and-text-to-speech-with-android-85758ff0f6d3

            // Build the speech
            var attrText = ""
            var contentText = ""

            article?.let {
                // Attribution
                if (it.author.name.isNotBlank() && it.publication.name.isNotBlank()) {
                    attrText = "${it.author.name} for ${it.publication.name}"
                } else if (it.author.name.isNotBlank()) {
                    attrText = "From ${it.author.name}"
                } else if (it.publication.name.isNotBlank()) {
                    attrText = "From ${it.publication.name}"
                }

                // Add article content
                // Todo:  don't rely on view to hold article content
                contentText += binding.content.text.toString()
                contentText = contentText.trim()
            }

            Log.d("TTS", attrText.isNotEmpty().toString())

            val text = "$attrText. $contentText"
            if (text.isNotEmpty()) {
                textToSpeechEngine.speak(attrText, TextToSpeech.QUEUE_FLUSH, null, "tts1")
                textToSpeechEngine.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, "tts2")
                textToSpeechEngine.speak(contentText, TextToSpeech.QUEUE_ADD, null, "tts3")
            }
        }
    }

    // Todo:  Move TTS to background task
    override fun onPause() {
        textToSpeechEngine.stop()
        super.onPause()
    }

    override fun onDestroy() {
        textToSpeechEngine.shutdown()
        super.onDestroy()
    }

    private fun initActionBar() {
        supportActionBar?.let {
            // Disable the default and enable the custom
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowCustomEnabled(true)
        }.also {
            val appbar = binding.bottomAppBar

            initMenuProvider()

            appbar.setNavigationOnClickListener {
                // Finish activity on back
                finish()
            }

            appbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.add -> {
                        // TODO
                        // Handle favorite icon press
                        true
                    }
                    R.id.more -> {
                        // TODO
                        // Handle more item (inside overflow menu) press
                        true
                    }
                    R.id.open_in_browser -> {
                        // Handle open in browser
                        Log.d("URL", "${article?.url}")
                        article?.url?.let { url: String -> openWebPage(url) }
                        true
                    }
                    else -> false
                }
            }


        }
    }

    private fun initMenuProvider() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.bottom_app_bar_article, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                TODO("Not yet implemented")
            }
        }, this, Lifecycle.State.RESUMED)
    }

    private fun glideFetch(url: String?, imageView: ImageView) {
        Glide.with(imageView.context)
            .asBitmap() // Try to display animated Gifs and video still
            .load(url)
            .apply(glideOptions)
            .into(imageView)
    }

//    private fun glideFetchDrawable(url: String?): Drawable {
//        Glide.with(this)
//            .asBitmap()
//            .load(url)
//            .into(object : CustomTarget<Bitmap>(){
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    val im = ImageView(this)
//                    im.setImageBitmap(resource)
//                    return im
//                }
//                override fun onLoadCleared(placeholder: Drawable?) {
//                    // this is called when imageView is cleared on lifecycle call or for
//                    // some other reason.
//                    // if you are referencing the bitmap somewhere else too other than this imageView
//                    // clear it here as you can no longer have the bitmap
//                }
//            })
//    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No browsers installed", Toast.LENGTH_SHORT).show()
        }
    }

    class GlideImageGetter(val context: Context) : ImageGetter {

        override fun getDrawable(source: String?): Drawable? {

            return context.getDrawable(R.drawable.ic_outline_album_24)

//            return CoroutineScope(Dispatchers.IO).launch {
//                return@launch Glide.with(context)
//                    .asDrawable()
//                    .load(source)
//                    .placeholder(R.drawable.ic_outline_album_24)
//                    .submit(100, 100)
//                    .get()
//            }

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


