package utap.tjp2677.antimatter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import utap.tjp2677.antimatter.ui.article.ArticleActivity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import utap.tjp2677.antimatter.ui.settings.SettingsActivity

class MainViewModel : ViewModel() {

    private var repository = Repository()
    private var articleList = MutableLiveData<List<Article>>()

    init {
        reset()  // Initialize values on instantiation
    }

    private fun reset() {
        articleList.apply {
            value = repository.fetchData()
        }
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
}