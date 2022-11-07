package utap.tjp2677.antimatter.ui.article

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.xeoh.android.texthighlighter.TextHighlighter
import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.ActivityArticleBinding
import utap.tjp2677.antimatter.utils.utils.*
import kotlin.math.max
import kotlin.math.roundToInt

//https://www.google.com/search?q=how+fast+does+the+average+person+read
const val wordsPerMinute = 200

class ArticleActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var glideOptions = RequestOptions ()
        .fitCenter()
        .transform(RoundedCorners (20))

    companion object {
        const val articleKey = "article"
    }

    private lateinit var binding: ActivityArticleBinding
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
//        initHighlighter()

        // Todo:  Find a non-deprecated way
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

            val articleContent =  Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY)
            binding.content.text = articleContent
            binding.content.movementMethod = LinkMovementMethod.getInstance();

            Log.d("HTML", articleContent.toString())

            // Todo: Precompute?
            val wordCount = articleContent.count { str -> str in " "}
            binding.header.wordCount.text = "$wordCount words"
            val readTime = max(1, (wordCount / wordsPerMinute.toFloat()).roundToInt())
            binding.header.readTime.text = "$readTime min"
        }


        binding.content.setOnCreateContextMenuListener { contextMenu, view, contextMenuInfo ->
            super.onCreateContextMenu(contextMenu, view, contextMenuInfo)
        }

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


    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        Log.d(javaClass.simpleName, "WERE HERE")
        Log.d(javaClass.simpleName, "$menuInfo")
    }

    private fun initActionBar() {
        supportActionBar?.let {
            // Disable the default and enable the custom
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowCustomEnabled(true)
        }.also {
            val appbar = binding.bottomAppBar

            appbar.setNavigationOnClickListener {
                finish()
            }

            appbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.add -> {
                        // Handle favorite icon press
                        true
                    }
                    R.id.more -> {
                        // Handle more item (inside overflow menu) press
                        true
                    }
                    else -> false
                }
            }

            initMenuProvider()
        }
    }

    private fun initMenuProvider() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.bottom_app_bar_article, menu)

//                menu.add(
//                    Menu.NONE, Menu.NONE, 0, "Test"
//                ).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                TODO("Not yet implemented")
            }
        }, this, Lifecycle.State.RESUMED)
    }

    private fun glideFetch(url: String?, imageView: ImageView) {
        com.bumptech.glide.Glide.with(imageView.context)
            .asBitmap() // Try to display animated Gifs and video still
            .load(url)
            .apply(glideOptions)
            .into(imageView)
    }

    private fun initHighlighter() {
        TextHighlighter()
            .setBackgroundColor(Color.YELLOW)
            .setForegroundColor(Color.YELLOW)

        binding.content.setOnClickListener {
            TextHighlighter()
                .setBackgroundColor(Color.parseColor("#FFFF00"))
                .addTarget(it)
                .highlight("Princess Heart", TextHighlighter.BASE_MATCHER)

        }
    }
}

val Number.toPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics)

val Number.toDp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_PX,
    this.toFloat(),
    Resources.getSystem().displayMetrics)