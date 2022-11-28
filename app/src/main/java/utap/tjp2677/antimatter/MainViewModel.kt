package utap.tjp2677.antimatter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.core.text.parseAsHtml
import androidx.lifecycle.*
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import utap.tjp2677.antimatter.authentication.FirestoreAuthLiveData
import utap.tjp2677.antimatter.authentication.FirestoreHelper
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.authentication.models.Annotation
import utap.tjp2677.antimatter.ui.settings.SettingsActivity
import kotlin.math.max


class MainViewModel : ViewModel() {

    /* ========================================== */
    /*                  Firebase                  */
    /* ========================================== */

    private var firebaseAuthLiveData = FirestoreAuthLiveData()
    private lateinit var firestoreHelper: FirestoreHelper

    fun updateUser() {
        firebaseAuthLiveData.updateUser()
    }

    fun observeUser(): FirestoreAuthLiveData {
        return firebaseAuthLiveData
    }

    fun initHelper() {
        firestoreHelper = firebaseAuthLiveData.getCurrentUser()?.let { FirestoreHelper(it) }!!
    }


    /* ========================================== */
    /*                  Articles                  */
    /* ========================================== */

    private var articleList = MutableLiveData<List<Article>>()
    private var openArticle = MutableLiveData<Article>()

    /* --- Create --- */
    // N/A -- User should not be able to create articles


    /* --- Read --- */
    fun fetchArticles(collection: Collection, limit: Int? = null) {
        firestoreHelper.fetchArticles(articleList, collection, limit, 0, 0)
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

    fun getArticleAt(position: Int): Article {
        return articleList.value!![position]
    }


    /* --- Update --- */
    // Todo: Cache the special collections (Inbox, Recent, Queue) in viewModel

    fun toggleArticleReadStatus(position: Int, successCallback: () -> Unit) {
        val article = getArticleAt(position)
        firestoreHelper.setArticleReadState(article, !article.isRead) {
            article.isRead = !article.isRead
            successCallback()  // To notify dataset of change
        }
    }

    fun toggleArticleQueueStatus(position: Int, successCallback: () -> Unit) {
        val article = getArticleAt(position)
        firestoreHelper.setArticleQueueStatus(article, !article.isQueued) {
            article.isQueued = !article.isQueued
            successCallback()  // To notify dataset of change
        }
    }


    /* --- Destroy --- */
    // N/A -- User should not be able to destroy articles


    /* ------------------------------------------ */

    fun observeOpenedArticle(): MutableLiveData<Article> {
        return openArticle
    }

    fun getOpenArticle(): Article? {
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
    fun createAnnotation(article: Article, start: Int, end: Int, text: String)  {
        firestoreHelper.addAnnotationToArticle(openAnnotations, article, start, end, text)
    }

    /* --- Read --- */
    fun fetchAnnotations(article: Article) {
        firestoreHelper.fetchAnnotations(openAnnotations, article)
    }

    fun observeOpenAnnotations(): LiveData<List<Annotation>> {
        return openAnnotations
    }

    /* --- Update --- */

    /* --- Destroy --- */
    fun deleteAnnotation(article: Article, annotation: Annotation) {
        firestoreHelper.deleteAnnotationFromArticle(openAnnotations, article, annotation)
    }


    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */

    private var collectionList = MutableLiveData<List<Collection>>()
    private var openCollection = MutableLiveData<Collection>()

    /* --- Create --- */
    private val USER_ORDER_MIN = 100
    fun createCollection(name: String, icon: String, order: Int? = null) {
        collectionList.value?.let {
            // Get max order in collections if @order is null
            val maxOrder = order ?: it.maxBy { col -> col.order }.order
            // Set minimum of order
            val mOrder = max(maxOrder + 1, USER_ORDER_MIN)
            // Submit to firebase
            firestoreHelper.createCollection(collectionList, name, icon, mOrder)
        }
    }

    /* --- Read --- */
    fun fetchCollectionAsOpen(collectionId: String) {
        firestoreHelper.fetchCollectionAsLiveData(openCollection, collectionId)
    }

    fun fetchCollections() {
        firestoreHelper.fetchCollections(collectionList)
    }

    fun observeCollections(): LiveData<List<Collection>> {
        return collectionList
    }

    fun getCollectionFromId(id: String): Collection? {
        return collectionList.value?.firstOrNull { it.firestoreID == id }
    }

    fun getCollectionAt(position: Int): Collection {
        return collectionList.value!![position]
    }

    fun getUserCollections(): List<Collection> {
        var list = listOf<Collection>()
        // TODO: include Queue
        collectionList.value?.let {
            list = it.mapNotNull { col ->
                // Filter not-user-created collections
                when (col.immortal) {
                    true -> null
                    false -> col
                }
            }
        }
        return list
    }

    fun observeOpenCollection(): LiveData<Collection> {
        return openCollection
    }

    fun setOpenCollection(collection: Collection) {
        openCollection.value = collection
    }

    fun getOpenCollection(): Collection? {
        return openCollection.value
    }

    /* --- Update --- */
    fun addArticleToCollection(article: Article, collection: Collection) {
        firestoreHelper.addArticleToCollection(article, collection)
    }

    fun removeArticleFromCollection(article: Article, collection: Collection) {
        firestoreHelper.removeArticleFromCollection(article, collection)
    }


    /* --- Destroy --- */
    fun deleteCollection(collection: Collection) {
        firestoreHelper.deleteCollection(collectionList, collection) {
            collectionList.value?.let {
                setOpenCollection(it[0])
            }
        }
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
    /*                TTS / Player                */
    /* ========================================== */

    // TODO:  Turn this into a MediaBrowserService
    lateinit var ttsEngine: TextToSpeech
    private val playerIsActive = MutableLiveData(false)  // Quick and dirty way of doing this

    private val nowPlayingArticle = MutableLiveData<Article?>()
    private val isPlaying = MutableLiveData(false)


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
    /*                   Others                   */
    /* ========================================== */

    fun openSettings(context: Context) {
        launchSettingsActivity(context)
    }

    fun shareMessage(context: Context, message: String, title: String? = null) {
        launchShareMessage(context, message, title)
    }

    fun openInBrowser(context: Context, url: String) {
        launchOpenInBrowser(context, url)
    }

    // Convenient place to put it as it is shared
    companion object {

        fun launchSettingsActivity(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }

        fun launchShareMessage(context: Context, message: String, title: String? = null) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_TITLE, title)
                // Todo?  Can add an optional image:  https://developer.android.com/training/sharing/send
            }
            context.startActivity(Intent.createChooser(intent, null))
        }

        fun launchOpenInBrowser(context: Context, url: String) {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (context.applicationContext.let { intent.resolveActivity(it.packageManager) } != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No browsers installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

}