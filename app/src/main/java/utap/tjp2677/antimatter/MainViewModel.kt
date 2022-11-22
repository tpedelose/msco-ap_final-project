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


    /* --- Read --- */
    fun fetchArticles(collectionId: String, limit: Int? = null, isRead: Boolean? = null) {
        firestoreHelper.fetchArticles(articleList, collectionId, limit, isRead, 0, 0)
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

    fun getArticleAt(position: Int): Article {
        return articleList.value!![position]
    }


    /* --- Update --- */
    fun toggleArticleReadStatus(position: Int, successCallback: () -> Unit) {
        val cid = openCollection.value?.firestoreID
        val article = getArticleAt(position)
        firestoreHelper.setArticleReadState(cid!!, article.firestoreID, !article.isRead) {
            article.isRead = !article.isRead
            successCallback()  // To notify dataset of change
        }
    }


    /* --- Destroy --- */


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
    fun addAnnotation(collectionId: String, articleId: String, start: Int, end: Int, text: String)  {
        firestoreHelper.addAnnotationToArticle(openAnnotations, collectionId, articleId, start, end, text)
    }

    /* --- Read --- */
    fun fetchAnnotations(collectionId: String, articleId: String) {
        firestoreHelper.fetchAnnotations(openAnnotations, collectionId, articleId)
    }

    fun observeOpenAnnotations(): LiveData<List<Annotation>> {
        return openAnnotations
    }

    /* --- Update --- */

    /* --- Destroy --- */
    fun deleteAnnotation(collectionId: String, articleId: String, annotationId: String) {
        firestoreHelper.deleteAnnotationFromArticle(openAnnotations, collectionId, articleId, annotationId)
    }


    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */

    private var userCollections = MutableLiveData<List<Collection>>()
    private var openCollection = MutableLiveData<Collection>()

    /* --- Create --- */

    /* --- Read --- */
    fun fetchCollectionAsOpen(collectionId: String) {
        firestoreHelper.fetchCollectionAsLiveData(openCollection, collectionId)
    }

    fun fetchCollections() {
        firestoreHelper.fetchCollections(userCollections)
    }

    fun observeCollections(): LiveData<List<Collection>> {
        return userCollections
    }

    fun getCollectionAt(position: Int): Collection {
        return userCollections.value!![position]
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

    fun addArticleToCollection(sourceCollectionId: String, targetCollectionId: String, position: Int) {
        val article = getArticleAt(position)
        firestoreHelper.addArticleToCollection(sourceCollectionId, targetCollectionId, article.firestoreID)
    }

    fun toggleArticleInReadLater(position: Int) {
        val article = getArticleAt(position)
//        when (article.isSavedForLater) {
//            true -> firestoreHelper.addArticleToCollection()
//            false -> firestoreHelper.removeArticleFromCollection()
//        }
    }

    /* --- Destroy --- */



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