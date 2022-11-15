package utap.tjp2677.antimatter

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.lifecycle.*
import utap.tjp2677.antimatter.ui.article.ArticleActivity
import utap.tjp2677.antimatter.api.FeedlyApi
import utap.tjp2677.antimatter.api.FeedlyRepository
import utap.tjp2677.antimatter.ui.settings.SettingsActivity

class MainViewModel : ViewModel() {

    // Initialize the API
    private val redditApi = FeedlyApi.create()
    private val feedlyRepository = FeedlyRepository(redditApi)

    private var repository = Repository()
    private var articleList = MutableLiveData<List<Article>>()
    private var readLaterArticleList = MutableLiveData<List<Article>>()

    init {
        reset()  // Initialize values on instantiation
    }

    private fun reset() {
        articleList.apply {
            value = repository.fetchData()
        }

        // Already URL encoded
//        val entryId = "8IBoZ/4+aoMdAnjTfyUVvn7dmGJHLYw5Dnfigc11Sqg=_184543ca833:30d07de:4fb599db"
//        val entryId = "8IBoZ%2F4%2BaoMdAnjTfyUVvn7dmGJHLYw5Dnfigc11Sqg%3D_184543ca833%3A30d07de%3A4fb599db"
//        viewModelScope.launch(context = (viewModelScope.coroutineContext + Dispatchers.IO)) {
//            val article = feedlyRepository.getPost(entryId)
//            Log.d(javaClass.simpleName, "Got article $article")
//
//        }
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

    fun getArticleAt(position: Int): Article {
        return articleList.value!![position]
    }


    // ### Launch Activities ###
    fun openArticle(context: Context, article: Article) {
        launchArticleActivity(context, article)
    }

    fun openSettings(context: Context) {
        launchSettingsActivity(context)
    }

    // Convenient place to put it as it is shared
    companion object {
        fun launchArticleActivity(context: Context, article: Article) {
            // https://stackoverflow.com/questions/47593205/how-to-pass-custom-object-via-intent-in-kotlin
            val intent = Intent(context, ArticleActivity::class.java)
                .putExtra(ArticleActivity.articleKey, article as java.io.Serializable)
            context.startActivity(intent)
        }

        fun launchSettingsActivity(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

//    val articleContent = MutableLiveData<Spannable>()
//
//    fun parseArticleContent(content: String, view: TextView) {
//
//        class GlideImageGetter(val context: Context) : Html.ImageGetter {
//            override fun getDrawable(source: String?): Drawable {
//                Glide.with(context)
//                    .asBitmap()
//                    .load(source)
//                    .placeholder(R.drawable.ic_outline_album_24)
//                    .submit()
//
//                return .drawable
//            }
//        }
//
//        val imageGetter = MyImageGetter(view.context, view, 300, 300)
//        viewModelScope.launch(context = (viewModelScope.coroutineContext + Dispatchers.IO)) {
//            val articleContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
//            acticleContent.postValue
//        }
//    }

}