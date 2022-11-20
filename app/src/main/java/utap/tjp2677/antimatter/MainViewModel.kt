package utap.tjp2677.antimatter

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.text.parseAsHtml
import androidx.lifecycle.*
import com.google.firebase.firestore.DocumentId
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import utap.tjp2677.antimatter.authentication.FirestoreAuthLiveData
import utap.tjp2677.antimatter.authentication.FirestoreHelper
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.authentication.models.Annotation
import utap.tjp2677.antimatter.ui.settings.SettingsActivity


class MainViewModel : ViewModel() {


    /* ========================================== */
    /*                TTS / Player                */
    /* ========================================== */

    // TODO:  Turn this into a MediaBrowserSerivce

    lateinit var ttsEngine: TextToSpeech
    private val playerIsActive = MutableLiveData<Boolean>(false)  // Quick and dirty way of doing this
    private val playerPlaylist = MutableLiveData<List<Publication>>()
    private val playerCurrentIndex = MutableLiveData<Int>(0)

    private val nowPlayingArticle = MutableLiveData<Article?>()
    // Below: thread-safe, non-nullable alternative to LiveData: https://medium.com/swlh/migrating-from-livedata-to-stateflow-4f28d6889a04
    // SharedFlow has options to replay same value
    private val isPlaying = MutableLiveData<Boolean>(false)


    fun setPlayerIsActive(active: Boolean) {
        playerIsActive.value = active
    }

    fun observePlayerIsActive(): LiveData<Boolean> {
        return playerIsActive
    }

    fun observeNowPlaying(): LiveData<Article?> {
        return nowPlayingArticle
    }

    fun setNowPlaying(article: Article) {
        nowPlayingArticle.value = article
    }

    fun setNowPlayingArticleAsOpenArticle() {
        openArticle.value = nowPlayingArticle.value
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

    fun postIsPlayingStatus(playing: Boolean) {
        isPlaying.postValue(playing)
    }

    fun stopPlaying() {
        ttsEngine.stop()
        isPlaying.value = false
    }

    fun openArticleIsLoadedToPlayer(): Boolean {
        return (openArticle.value?.firestoreID == nowPlayingArticle.value?.firestoreID)
    }

    fun playArticle() {
        // https://rtdtwo.medium.com/speech-to-text-and-text-to-speech-with-android-85758ff0f6d3

        nowPlayingArticle.value?.let {
            val body: Sequence<String> = createTextSequence(it)
            if (body.none()) { return } // Skip speech if there's nothing to read

            val attr: String = createAttribution(it)

            // Time To Speak!
            setIsPlayingStatus(true)
            ttsEngine.speak(attr, TextToSpeech.QUEUE_FLUSH, null, "tts-attribution")
            ttsEngine.playSilentUtterance(450, TextToSpeech.QUEUE_ADD, "tts-pause")

            body.forEachIndexed fe@{ index, s ->
                if (s.isBlank()) { return@fe }  // i.e. "continue"
                ttsEngine.speak(s, TextToSpeech.QUEUE_ADD, null, "tts-body-$index")
                // Add a short pause between paragraphs
                ttsEngine.playSilentUtterance(120, TextToSpeech.QUEUE_ADD, "tts-body-$index-pause")
            }

            // To trigger some onDone action
            ttsEngine.playSilentUtterance(1, TextToSpeech.QUEUE_ADD, "tts-final")
        }
    }

    private fun createAttribution(article: Article): String {
        return when {
            article.author.isNotBlank() && article.publicationName.isNotBlank() -> {
                "${article.author} for ${article.publicationName}"
            }
            article.author.isNotBlank() -> "From ${article.author}"
            article.publicationName.isNotBlank() -> "From ${article.publicationName}"
            else -> ""
        }
    }

    private fun createTextSequence(article: Article): Sequence<String> {
        val cleanText = Jsoup.clean(article.content, Safelist.basic()).parseAsHtml().toString()
        if (cleanText.isBlank()) {
            return sequenceOf()
        }

        // TODO: Split text into smaller chunks for progress tracking. By sentence? Word?
        return cleanText.splitToSequence("\n").filter { it.isNotBlank() }
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
    /*            Article Annotations             */
    /* ========================================== */

    private var openAnnotations = MutableLiveData<List<Annotation>>()

    /* --- Create --- */

    fun addAnnotation(articleId: String, start: Int, end: Int) {
        firestoreHelper.addAnnotationToArticle(articleId, start, end)
        // TODO: Callback
    }

    /* --- Read --- */

    fun fetchAnnotations() {
        openArticle.value?.firestoreID?.let {
            firestoreHelper.fetchAnnotations(openAnnotations, it)
        }
    }

    fun observeOpenAnnotations(): LiveData<List<Annotation>> {
        return openAnnotations
    }

    /* --- Update --- */


    /* --- Destroy --- */


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