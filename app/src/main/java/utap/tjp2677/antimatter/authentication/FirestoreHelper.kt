package utap.tjp2677.antimatter.authentication

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentReference
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

    private fun minMaxArticleLimit(limit: Int?): Long {
        return when(limit) {
            null -> FETCH_DEFAULT
            else -> min(limit, FETCH_LIMIT)
        }.toLong()
    }

    /////////////////////////////////////////////////////////////
    // If we want to listen for real time updates use this
    // .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
    // But be careful about how listener updates live data
    // and noteListener?.remove() in onCleared

    /* ========================================== */
    /*                  Articles                  */
    /* ========================================== */

    fun fetchArticles(articleList: MutableLiveData<List<Article>>,
                      collection: Collection, limit: Int?, start: Int, end: Int) {
        // TODO!!!  Pagination, Order, Filter

        val TAG = "FetchArticles"
        val isReadKey = "isRead"
        val collectionsKey = "collections"
        val publishedKey = "published"

        val ordering = DEFAULT_ORDERING
        val fetchLimit = minMaxArticleLimit(limit)

        val articlesRef = db.collection("${userPrefix}/articles")
        val collectionDocRef = db.collection("${userPrefix}/collections")
            .document(collection.firestoreID)

        // This when is a strange way of doing this, but it's the only way I found that works for
        // the same operation on two different types: CollectionReference and Query
        when(collection.filter[isReadKey]) {
            null -> articlesRef
            else -> articlesRef.whereEqualTo(isReadKey, collection.filter[isReadKey])
        }
            .whereArrayContains(collectionsKey, collectionDocRef)
            .orderBy(publishedKey, ordering)
            .limit(fetchLimit)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "${result.documents.size} articles successfully fetched!")
                articleList.postValue(
                    result.documents.mapNotNull {
                        it.toObject(Article::class.java)
                    }
                )
            }
            .addOnFailureListener { e->
                Log.w(TAG, "Error fetching articles", e)
                articleList.postValue(listOf())
            }
    }

    fun setArticleReadState(article: Article, isRead: Boolean, onSuccessCallback: () -> Unit) {

        val isReadKey = "isRead"
        val articlesKey = "articles"
        val collectionsKey = "collections"
        val recentDocId = "Recent"

        val articleDocRef = db.collection("$userPrefix/articles")
            .document(article.firestoreID)
        val recentDocRef = db.collection("$userPrefix/collections")
            .document(recentDocId)

        db.runTransaction { transaction ->
            // article: add/remove Recently Read document reference to @collectionsKey field
            // collection: add/remove Article document reference to @articlesKey field
            if (isRead) {
                // Mark as read
                transaction.update(articleDocRef, collectionsKey, FieldValue.arrayUnion(recentDocRef))
                transaction.update(recentDocRef, articlesKey, FieldValue.arrayUnion(articleDocRef))
            } else {
                // Mark as unread
                transaction.update(articleDocRef, collectionsKey, FieldValue.arrayRemove(recentDocRef))
                transaction.update(recentDocRef, articlesKey, FieldValue.arrayRemove(articleDocRef))
            }

            // Set read status on article
            transaction.update(articleDocRef, isReadKey, isRead)

            null
        }.addOnSuccessListener {
            onSuccessCallback()
        }
    }


    /* ========================================== */
    /*            Article Annotations             */
    /* ========================================== */


    fun fetchAnnotations(annotationList: MutableLiveData<List<Annotation>>, article: Article) {

        val collectionRef = db.collection(
            "$userPrefix/articles/${article.firestoreID}/annotations")

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

    fun addAnnotationToArticle(annotationList: MutableLiveData<List<Annotation>>, article: Article,
                               start: Int, end: Int, text: String) {
        val TAG = "AddAnnotationToArticle"

        val collectionRef = db.collection(
            "$userPrefix/articles/${article.firestoreID}/annotations")

        val annotationData = hashMapOf(
            "start" to start,
            "end" to end,
            "text" to text
        )

        collectionRef
            .document()
            .set(annotationData)
            .addOnSuccessListener {
                Log.d(TAG, "Annotation successfully written to article ${article.firestoreID}!")
                fetchAnnotations(annotationList, article)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing annotation to article ${article.firestoreID}", e)
                fetchAnnotations(annotationList, article)
            }
    }

    fun deleteAnnotationFromArticle(annotationList: MutableLiveData<List<Annotation>>,
                                    article: Article, annotation: Annotation) {
        val TAG = "DeleteAnnotationFromArticle"

        val collectionRef = db.collection(
            "$userPrefix/articles/${article.firestoreID}/annotations")

        collectionRef
            .document(annotation.firestoreID)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Annotation ${annotation.firestoreID} successfully deleted!")
                fetchAnnotations(annotationList, article)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting annotation ${annotation.firestoreID}", e)
                fetchAnnotations(annotationList, article)
            }
    }


    /* ========================================== */
    /*                 Collections                */
    /* ========================================== */

    fun createCollection(collectionList: MutableLiveData<List<Collection>>,
                         name: String, icon: String, order: Int) {
        // Todo? Consider checking for distinct collection names
        val TAG = "CreateCollection"
        val collectionRef = db.collection("$userPrefix/collections/")

        val annotationData = hashMapOf(
            "name" to name,
            "icon" to icon,
            "order" to order,
            "count" to 0,
        )

        collectionRef
            .document()
            .set(annotationData)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully written!")
                fetchCollections(collectionList)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing document", e)
                fetchCollections(collectionList)
            }
    }

    fun deleteCollection(collectionList: MutableLiveData<List<Collection>>,
                         collection: Collection, onSuccessCallback: () -> Unit) {
        val TAG = "DeleteCollection"

        val collectionsKey = "collections"

        val articlesRef = db.collection("$userPrefix/articles/")
        val collectionDocRef = db.collection("$userPrefix/collections/")
            .document(collection.firestoreID)

        // Get articles that reference this collection
        articlesRef
            .whereArrayContains(collectionsKey, collectionDocRef)
            .get()
            .addOnSuccessListener { result ->
                // Run transaction
                db.runTransaction { transaction ->
                    // Remove references to this collections
                    Log.d("GELP", result.documents.toString())
                    result.documents.forEach {
                        Log.d("GELP", it.toString())
                        transaction.update(it.reference, collectionsKey,
                            FieldValue.arrayRemove(collectionDocRef))
                    }
                    // Finally: Delete collection
                    collectionDocRef.delete()

                    null
                }.addOnSuccessListener {
                    // Refresh collections
                    fetchCollections(collectionList)
                }
            }
    }

    fun fetchCollections(collectionList: MutableLiveData<List<Collection>>) {
        val collectionRef = db.collection("$userPrefix/collections")

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
        val collectionRef = db.collection("$userPrefix/collections/")

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

    fun addArticleToCollection(article: Article, collection: Collection) {

        val articlesKey = "articles"  // for collection
        val collectionsKey = "collections"  // for article

        val articleRef = db.collection("$userPrefix/articles")
            .document(article.firestoreID)
        val collectionRef = db.collection("$userPrefix/collections")
            .document(collection.firestoreID)

        db.runTransaction { transaction ->
            // Update article with reference to collection
            transaction.update(articleRef, collectionsKey, FieldValue.arrayUnion(collectionRef))

            // Update collection with reference to article (and increment counter)
            transaction.update(collectionRef, articlesKey, FieldValue.arrayUnion(articleRef))

            null
        }
    }

    fun removeArticleFromCollection(article: Article, collection: Collection) {
        val articlesKey = "articles"  // for collection
        val collectionsKey = "collections"  // for article

        val articleRef = db.collection("$userPrefix/articles")
            .document(article.firestoreID)
        val collectionRef = db.collection("$userPrefix/collections")
            .document(collection.firestoreID)

        // Todo: Switch to batch write?
        db.runTransaction { transaction ->
            // Remove reference to collection from article
            transaction.update(articleRef, collectionsKey, FieldValue.arrayRemove(collectionRef))

            // Remove reference to article from collection
            transaction.update(collectionRef, articlesKey, FieldValue.arrayRemove(articleRef))

            null
        }
    }

    fun setArticleQueueStatus(article: Article, isQueued: Boolean, onSuccessCallback: () -> Unit) {
        val isQueuedKey = "isQueued"
        val articlesKey = "articles"
        val collectionsKey = "collections"
        val queueDocKey = "Queue"

        val articleDocRef = db.collection("$userPrefix/articles")
            .document(article.firestoreID)
        val queueDocRef = db.collection("$userPrefix/collections")
            .document(queueDocKey)

        db.runTransaction { transaction ->
            // article: add/remove Recently Read document reference to @collectionsKey field
            // collection: add/remove Article document reference to @articlesKey field
            if (isQueued) {
                // Add to queue
                transaction.update(articleDocRef, collectionsKey, FieldValue.arrayUnion(queueDocRef))
                transaction.update(queueDocRef, articlesKey, FieldValue.arrayUnion(articleDocRef))
            }
            else {
                // Remove from queue
                transaction.update(articleDocRef, collectionsKey, FieldValue.arrayRemove(queueDocRef))
                transaction.update(queueDocRef, articlesKey, FieldValue.arrayRemove(articleDocRef))
            }

            // Set queue status on article
            transaction.update(articleDocRef, isQueuedKey, isQueued)

            null
        }.addOnSuccessListener {
            onSuccessCallback()
        }
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