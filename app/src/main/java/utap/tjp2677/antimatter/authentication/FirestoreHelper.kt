package utap.tjp2677.antimatter.authentication

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestoreSettings
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.authentication.models.Collection
import kotlin.math.min

const val FETCH_DEFAULT = 20
const val FETCH_LIMIT = 100

class FirestoreHelper() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        this.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
    }
    private val collectionRoot = "articles"

    /////////////////////////////////////////////////////////////
    // Interact with Firestore db
    // https://firebase.google.com/docs/firestore/query-data/get-data
    //
    // If we want to listen for real time updates use this
    // .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
    // But be careful about how listener updates live data
    // and noteListener?.remove() in onCleared

    fun fetchArticles(articleList: MutableLiveData<List<Article>>, limit: Int? = FETCH_DEFAULT) {
        // TODO: https://medium.com/firebase-tips-tricks/how-to-drastically-reduce-the-number-of-reads-when-no-documents-are-changed-in-firestore-8760e2f25e9e

        var _limit = when(limit) {
            null -> FETCH_DEFAULT
            else -> min(limit, FETCH_LIMIT)
        }.toLong()

        val collectionRef = db.collection(collectionRoot)

        val mainQuery = collectionRef
            .orderBy("published", Query.Direction.DESCENDING)
            .limit(_limit)

        mainQuery.get(Source.SERVER)
            .addOnSuccessListener { result ->
                articleList.postValue(
                    result.documents.mapNotNull {
                        it.toObject(Article::class.java)
                    }
                )
            }
    }

    fun fetchPublication() {

    }

    fun fetchUserSubscriptions(subscriptionList: MutableLiveData<List<Publication>>) {
        val collectionRef = db.collection("publications")

        collectionRef
            .orderBy("updated", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                subscriptionList.postValue(
                    result.documents.mapNotNull { result ->
                        result.toObject(Publication::class.java)
                    }
                )
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Utter failure")
            }
    }

    fun fetchUserCollections(collectionList: MutableLiveData<List<Collection>>) {
        val collectionRef = db.collection("collections")

        collectionRef
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                collectionList.postValue(
                    result.documents.mapNotNull { result ->
                        result.toObject(Collection::class.java)
                    }
                )
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Utter failure")
            }
    }

    fun fetchUserReadLater() {

    }

    fun Query.getCacheBeforeServer() {
        TODO()
    }

}