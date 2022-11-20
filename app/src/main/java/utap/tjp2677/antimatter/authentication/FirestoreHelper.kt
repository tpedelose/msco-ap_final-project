package utap.tjp2677.antimatter.authentication

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestoreSettings
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.authentication.models.Annotation
import kotlin.math.min

const val FETCH_DEFAULT = 20
const val FETCH_LIMIT = 100


// Extension to deal with malformed documents by rejecting them
fun <T> DocumentSnapshot.toObjectOrNull(dataclass: Class<T>): T? {
    return try {
        this.toObject(dataclass)
    } catch (e: java.lang.RuntimeException) {
        null
    }
}

/*  TODO:  Reduce reads
 *     https://medium.com/firebase-tips-tricks/how-to-drastically-reduce-the-number-of-reads-when-no-documents-are-changed-in-firestore-8760e2f25e9e
 */

class FirestoreHelper() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        this.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
    }
    private val DEFAULT_ORDERING = Query.Direction.DESCENDING


    /////////////////////////////////////////////////////////////
    // Interact with Firestore db
    // https://firebase.google.com/docs/firestore/query-data/get-data
    //
    // If we want to listen for real time updates use this
    // .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
    // But be careful about how listener updates live data
    // and noteListener?.remove() in onCleared

    /* ========================================== */
    /*                  Articles                  */
    /* ========================================== */

    fun listenForArticles(articleList: MutableLiveData<List<Article>>) {
        TODO()
    }

    fun fetchArticles(articleList: MutableLiveData<List<Article>>, limit: Int?, start: Int, end: Int) {

        // TODO!!!  Pagination, Order, Filter
        val ordering = DEFAULT_ORDERING

        val _limit = when(limit) {
            null -> FETCH_DEFAULT
            else -> min(limit, FETCH_LIMIT)
        }.toLong()

        val collectionRef = db.collection("articles")

        collectionRef
            .whereEqualTo("isRead", false)  // Todo: allow this to be passed in
            .orderBy("published", ordering)
            .limit(_limit)
            .get()
            .addOnSuccessListener { result ->
                articleList.postValue(
                    result.documents.mapNotNull {
                        it.toObjectOrNull(Article::class.java)
                    }
                )
            }
    }

    fun setArticleReadState(articleId: String, isRead: Boolean, onSuccessCallback: () -> Unit) {
        val collectionRef = db.collection("articles")

        collectionRef
            .document(articleId)
            .update(
                mapOf("isRead" to isRead)
            )
            .addOnSuccessListener {
                onSuccessCallback()
            }
    }

    /* ========================================== */
    /*            Article Annotations             */
    /* ========================================== */


    fun fetchAnnotations(annotationList: MutableLiveData<List<Annotation>>, articleId: String) {

        val collectionRef = db.collection("articles/$articleId/annotations")

        collectionRef
            .get()
            .addOnSuccessListener { result ->
                annotationList.postValue(
                    result.documents.mapNotNull {
                        it.toObjectOrNull(Annotation::class.java)
                    }
                )
            }
    }

    fun addAnnotationToArticle(articleId: String, start: Int, end: Int) {
        val TAG = "AddAnnotationToArticle"

        val collectionRef = db.collection("articles/$articleId/annotations")

        val annotationData = hashMapOf(
            "start" to start,
            "end" to end
        )

        collectionRef
            .document()
            .set(annotationData)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener {
                e -> Log.w(TAG, "Error writing document", e)
            }
    }

    fun deleteAnnotationFromArticle(articleId: String, annotationId: String) {
        val TAG = "DeleteAnnotationFromArticle"

        val collectionRef = db.collection("articles/$articleId/annotations")

        collectionRef
            .document(annotationId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener {
                    e -> Log.w(TAG, "Error deleting document", e)
            }

    }


    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */

    fun fetchCollections(collectionList: MutableLiveData<List<Collection>>) {
        val collectionRef = db.collection("collections")

        collectionRef
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                collectionList.postValue(
                    result.documents.mapNotNull {
                        it.toObjectOrNull(Collection::class.java)
                    }
                )
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Utter failure")
            }
    }

    fun fetchCollectionArticles(articleList: MutableLiveData<List<Article>>) {

    }

    fun addArticleToCollection(articleId: String, collectionId: String) {
        val collectionRef = db.collection("collections")

        collectionRef
//            .orderBy("published", ordering)
//            .limit(_limit)
//            .get(Source.SERVER)
//            .addOnSuccessListener { result ->
//                articleList.postValue(
//                    result.documents.mapNotNull {
//                        it.toObjectOrNull(Article::class.java)
//                    }
//                )
//            }
    }

    fun removeArticleFromCollection(articleId: String, collectionId: String) {

    }


    /* ========================================== */
    /*                Subscriptions               */
    /* ========================================== */

    fun fetchSubscriptions(subscriptionList: MutableLiveData<List<Publication>>) {
        val collectionRef = db.collection("publications")

        collectionRef
            .orderBy("updated", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                subscriptionList.postValue(
                    result.documents.mapNotNull {
                        it.toObjectOrNull(Publication::class.java)
                    }
                )
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Utter failure")
            }
    }



    fun Query.getCacheBeforeServer() {
        TODO()
    }

}