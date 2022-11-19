package utap.tjp2677.antimatter

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import utap.tjp2677.antimatter.authentication.FirestoreAuthLiveData
import utap.tjp2677.antimatter.authentication.FirestoreHelper
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.ui.settings.SettingsActivity


class MainViewModel : ViewModel() {


    /* ========================================== */
    /*                TTS / Player                */
    /* ========================================== */

    // TODO:  Turn this into a MediaBrowserSerivce

    lateinit var ttsEngine: TextToSpeech
    private val playerPlaylist = MutableLiveData<List<Publication>>()
    private val playerCurrentIndex = MutableLiveData<Int>(0)

    private val nowPlayingArticle = MutableLiveData<Article?>()
    // Below: thread-safe, non-nullable alternative to LiveData: https://medium.com/swlh/migrating-from-livedata-to-stateflow-4f28d6889a04
    // SharedFlow has options to replay same value
    private val isPlaying = MutableLiveData<Boolean>(false)


    fun observeNowPlaying(): LiveData<Article?> {
        return nowPlayingArticle
    }

    fun setNowPlaying(article: Article) {
        nowPlayingArticle.value = article
    }

    fun observeIsPlayingStatus(): LiveData<Boolean> {
        return isPlaying
    }

    fun getIsPlayingStatus(): Boolean {
        return isPlaying.value ?: false
    }

    fun setIsPlayingStatus(playing: Boolean) {
        isPlaying.value = playing
    }

    fun stopPlaying() {
        ttsEngine.stop()
        isPlaying.value = false
        nowPlayingArticle.value = null
    }

    fun openArticleIsLoaded(): Boolean {
        return (openArticle.value?.firestoreID == nowPlayingArticle.value?.firestoreID)
    }


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

    private var articleList = MutableLiveData<List<Article>>()
    private var openArticle = MutableLiveData<Article>()


    /* --- Create --- */


    /* --- Read --- */

    fun fetchArticles(limit: Int?) {
        firestoreHelper.fetchArticles(articleList, limit, 0, 0)
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

    fun getArticleAt(position: Int): Article {
        return articleList.value!![position]
    }


    /* --- Update --- */

    fun toggleArticleReadStatus(position: Int) {
        val article = getArticleAt(position)
        firestoreHelper.setArticleReadState(article.firestoreID, !article.isRead) {
            article.isRead = !article.isRead
        }
    }

    fun setArticleReadStatus(position: Int, read: Boolean) {
        val article = getArticleAt(position)
        firestoreHelper.setArticleReadState(article.firestoreID, read) {
            article.isRead = read
        }
    }

    fun markArticleAsUnread(article: Article) {

    }


    /* --- Destroy --- */


    /* ------------------------------------------ */


    fun observeOpenedArticle(): MutableLiveData<Article> {
        return openArticle
    }

    fun getOpenedArticle(): Article? {
        Log.d(javaClass.simpleName, openArticle.value.toString())
        return openArticle.value
    }

    fun setOpenedArticle(article: Article) {
        openArticle.postValue(article)
    }



    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */

    private var userCollections = MutableLiveData<List<Collection>>()


    /* --- Create --- */

    fun createCollection() {

    }


    /* --- Read --- */

    fun fetchCollections() {
        firestoreHelper.fetchCollections(userCollections)
    }

    fun observeCollections(): LiveData<List<Collection>> {
        return userCollections
    }

    fun getCollectionAt(position: Int): Collection {
        return userCollections.value!![position]
    }


    /* --- Update --- */
    fun updateCollection() {

    }

    fun addArticleToCollection(position: Int) {
        val article = getArticleAt(position)
//        firestoreHelper.addArticleToCollection(article.firestoreID, !article.isRead) {
//            article.isRead = !article.isRead
//        }
    }

    fun toggleArticleInReadLater(position: Int) {
        val article = getArticleAt(position)
//        when (article.isSavedForLater) {
//            true -> firestoreHelper.addArticleToCollection()
//            false -> firestoreHelper.removeArticleFromCollection()
//        }
    }


    /* --- Destroy --- */
    fun deleteCollection() {

    }



    /* ========================================== */
    /*                Subscriptions               */
    /* ========================================== */

    private var userSubscriptions = MutableLiveData<List<Publication>>()


    /* --- Create --- */

    /* --- Read --- */

    fun fetchSubscriptions() {
        firestoreHelper.fetchSubscriptions(userSubscriptions)
    }

    fun observeSubscriptions(): LiveData<List<Publication>> {
        return userSubscriptions
    }


    /* --- Update --- */

    /* --- Destroy --- */




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