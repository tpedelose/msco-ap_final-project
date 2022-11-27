package utap.tjp2677.antimatter.authentication.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentReference

data class Collection (
    // IDs
    @DocumentId
    var firestoreID: String = "",

    // Metadata
    var name: String = "",
    var icon: String = "",
    var order: Int = 100,

    // Articles
    var articles: List<DocumentReference> = listOf(),
    var count: Int = 0,

    // Special
    var immortal: Boolean = false,
    var filter: Map<String, Any> = mapOf()
)