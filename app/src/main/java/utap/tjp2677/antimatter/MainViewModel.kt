package utap.tjp2677.antimatter

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.*
import utap.tjp2677.antimatter.authentication.FirestoreAuthLiveData
import utap.tjp2677.antimatter.authentication.FirestoreHelper
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.ui.settings.SettingsActivity


class MainViewModel : ViewModel() {

    private var articleList = MutableLiveData<List<Article>>()
    private var articleOpened = MutableLiveData<Article>()

    private var userSubscriptions = MutableLiveData<List<Publication>>()

    /* ========================================== */
    /*                TTS / Player                */
    /* ========================================== */
    lateinit var ttsEngine: TextToSpeech
    private var playerPlaylist = MutableLiveData<List<Publication>>()
    private var playerCurrentIndex = MutableLiveData<Int>(0)



    /* ========================================== */
    /*                  Firebase                  */
    /* ========================================== */
    private var firebaseAuthLiveData = FirestoreAuthLiveData()
    private var firestoreHelper = FirestoreHelper()

    fun updateUser() {
        firebaseAuthLiveData.updateUser()
    }


    /* ========================================== */
    /*                  Articles                  */
    /* ========================================== */

    fun fetchArticles(limit: Int?) {
        firestoreHelper.fetchArticles(articleList, limit)
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

    fun getArticleAt(position: Int): Article {
        return articleList.value!![position]
    }

    fun getOpenedArticle(): Article? {
        Log.d(javaClass.simpleName, articleOpened.value.toString())
        return articleOpened.value
    }

    fun setOpenedArticle(article: Article) {
        articleOpened.postValue(article)
    }

    fun observeOpenedArticle(): MutableLiveData<Article> {
        return articleOpened
    }

    fun addArticleToCollection(article: Article, collection: Collection) {

    }

    fun markArticleAsRead(article: Article) {

    }
    fun markArticleAsUnread(article: Article) {

    }



    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */
    private var userCollections = MutableLiveData<List<Collection>>()

    fun fetchCollections() {
        firestoreHelper.fetchUserCollections(userCollections)
    }

    fun observeCollections(): LiveData<List<Collection>> {
        return userCollections
    }

    fun createCollection() {

    }

    fun readCollection() {

    }

    fun updateCollection() {

    }

    fun deleteCollection() {

    }



    /* ========================================== */
    /*                Subscriptions               */
    /* ========================================== */

    fun fetchSubscriptions() {
         firestoreHelper.fetchUserSubscriptions(userSubscriptions)
    }

    fun observeSubscriptions(): LiveData<List<Publication>> {
        return userSubscriptions
    }



    /* ========================================== */
    /*                   Others                   */
    /* ========================================== */

    fun openSettings(context: Context) {
        launchSettingsActivity(context)
    }

    // Convenient place to put it as it is shared
    companion object {

        fun launchSettingsActivity(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

}