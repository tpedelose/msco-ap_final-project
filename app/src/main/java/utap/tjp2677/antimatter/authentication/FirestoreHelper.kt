package utap.tjp2677.antimatter.authentication

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
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

class FirestoreHelper(private val fbUser: FirebaseUser) {

    val userPrefix = "/userData/${fbUser.uid}"

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        this.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
    }
    private val DEFAULT_ORDERING = Query.Direction.DESCENDING

    fun minMaxArticleLimit(limit: Int?): Long {
        return when(limit) {
            null -> FETCH_DEFAULT
            else -> min(limit, FETCH_LIMIT)
        }.toLong()
    }

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

    fun fetchArticles(articleList: MutableLiveData<List<Article>>,
                      collectionId: String, limit: Int?, isRead: Boolean? = null, start: Int, end: Int) {

        val TAG = "FetchArticles"
        // TODO!!!  Pagination, Order, Filter
        val ordering = DEFAULT_ORDERING
        val _limit = minMaxArticleLimit(limit)

        val collectionRef = db.collection(
            "${userPrefix}/collections/$collectionId/articles")

        // This when is a strange way of doing this, but it's the only way I found that works for
        // the same operation on two different types: CollectionReference and Query
        when (isRead != null) {
            true -> collectionRef.whereEqualTo("isRead", isRead)
            false -> collectionRef
        }
            .orderBy("published", ordering)
            .limit(_limit)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Articles successfully fetched!")
                Log.d(TAG, result.documents.toString())
                articleList.postValue(
                    result.documents.mapNotNull {
                        Log.d(TAG, it.id)
                        it.toObject(Article::class.java)
                    }
                )
            }
            .addOnFailureListener { e->
                Log.w(TAG, "Error deleting document", e)
                articleList.postValue(listOf())
            }
    }

    fun setArticleReadState(collectionId: String, articleId: String, isRead: Boolean, onSuccessCallback: () -> Unit) {

        val recentDocId = "Recent"

        // TODO: Solve what happens when marking articles as unread in the Recently Read section

        val collectionRef = db.collection(
            "${userPrefix}/collections/$collectionId/articles")
        val recentlyReadRef = db.collection(
            "${userPrefix}/collections/$recentDocId/articles")

        val baseDocRef = collectionRef.document(articleId)
        val recentDocRef = recentlyReadRef.document(articleId)

        db.runTransaction { transaction ->
            // Ideally this would all be handled on the server and/or via a relational DB, but...
            val baseSnapshot = transaction.get(baseDocRef)
            val recentSnapshot = transaction.get(recentDocRef)

            // Update the base document
            transaction.update(baseDocRef, "isRead", isRead)

            if (isRead) {
                // Marking as read. Create a copy in the Recent collection
                baseSnapshot.data?.let {
                    it["isRead"] = true
                    transaction.set(recentSnapshot.reference, it)
                }
            } else if (recentSnapshot.exists()) {
                // Marking as unread. Delete the copy in the Recent collection (if it exists)
                transaction.delete(recentSnapshot.reference)
            }

            null
        }.addOnSuccessListener {
            onSuccessCallback()
        }
    }


    /* ========================================== */
    /*            Article Annotations             */
    /* ========================================== */


    fun fetchAnnotations(annotationList: MutableLiveData<List<Annotation>>,
                         collectionId: String, articleId: String) {

        val collectionRef = db.collection(
            "${userPrefix}/collections/$collectionId/articles/$articleId/annotations")

        collectionRef
            .get()
            .addOnSuccessListener { result ->
                annotationList.postValue(
                    result.documents.mapNotNull {
                        it.toObjectOrNull(Annotation::class.java)
                    }
                )
            }.addOnFailureListener {
                annotationList.postValue(listOf())
            }
    }

    fun addAnnotationToArticle(annotationList: MutableLiveData<List<Annotation>>,
                               collectionId: String, articleId: String,
                               start: Int, end: Int, text: String) {
        val TAG = "AddAnnotationToArticle"

        val collectionRef = db.collection(
            "${userPrefix}/collections/$collectionId/articles/$articleId/annotations")

        val annotationData = hashMapOf(
            "start" to start,
            "end" to end,
            "text" to text
        )

        collectionRef
            .document()
            .set(annotationData)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully written!")
                fetchAnnotations(annotationList, collectionId, articleId)
            }
            .addOnFailureListener {
                e -> Log.w(TAG, "Error writing document", e)
                fetchAnnotations(annotationList, collectionId, articleId)
            }
    }

    fun deleteAnnotationFromArticle(annotationList: MutableLiveData<List<Annotation>>,
                                    collectionId: String, articleId: String, annotationId: String) {
        val TAG = "DeleteAnnotationFromArticle"

        val collectionRef = db.collection(
            "${userPrefix}/collections/$collectionId/articles/$articleId/annotations")

        collectionRef
            .document(annotationId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
                fetchAnnotations(annotationList, collectionId, articleId)
            }
            .addOnFailureListener {
                e ->
                Log.w(TAG, "Error deleting document", e)
                fetchAnnotations(annotationList, collectionId, articleId)
            }
    }


    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */

    fun fetchCollections(collectionList: MutableLiveData<List<Collection>>) {
        val collectionRef = db.collection("${userPrefix}/collections")

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
                collectionList.postValue(listOf())
            }
    }

    fun fetchCollectionAsLiveData(collection: MutableLiveData<Collection>, collectionId: String) {
        val collectionRef = db.collection("${userPrefix}/collections/")

        collectionRef
            .document(collectionId)
            .get()
            .addOnSuccessListener { result ->
                collection.postValue(
                    result.toObjectOrNull(Collection::class.java)
                )
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Utter failure")
            }
    }

    fun addArticleToCollection(sourceCollectionId: String, targetCollectionId: String, articleId: String) {

        // Todo:  Maybe flatten this structure.  Article has list of ids of collections?  Requires multiple fetches though

        val sourceCollectionRef = db.collection(
            "${userPrefix}/collections/$sourceCollectionId/articles")
        val targetCollectionRef = db.collection(
            "${userPrefix}/collections/$targetCollectionId/articles")

        val sourceDocRef = sourceCollectionRef.document(articleId)
        val targetDocRef = targetCollectionRef.document(articleId)

        db.runTransaction { transaction ->
            val sourceSnapshot = transaction.get(sourceDocRef)
            val targetSnapshot = transaction.get(targetDocRef)

            if (!targetSnapshot.exists()) {
                sourceSnapshot.data?.let {
                    transaction.set(targetSnapshot.reference, it)
                }
            }
        }
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