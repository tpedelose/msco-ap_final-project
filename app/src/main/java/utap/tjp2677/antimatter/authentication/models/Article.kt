package utap.tjp2677.antimatter.authentication.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentReference

data class Article(
    // IDs
    @DocumentId
    var firestoreID: String = "",

    // Content
    var title: String = "",
    var author: String = "",
    var content: String = "",
    var link: String = "",
    var image: String? = null,

    // User related data
    var read: Boolean = false,

    // Publication
    var publication: DocumentReference? = null,
    var publicationName: String = "",
    var publicationIconLink: String? = null,

    // Metadata
    @ServerTimestamp
    var published: Timestamp? = null,
    @ServerTimestamp
    var updated:  Timestamp? = null,
)