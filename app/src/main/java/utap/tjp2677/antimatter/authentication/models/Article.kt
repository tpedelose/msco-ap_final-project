package utap.tjp2677.antimatter.authentication.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.PropertyName

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
    @get:PropertyName("isRead") // Well this is annoying... https://medium.com/@eeddeellee/boolean-fields-that-start-with-is-in-firebase-firestore-49afb65e3639
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    @get:PropertyName("is_saved_for_later")
    @set:PropertyName("is_saved_for_later")
    var isSavedForLater: Boolean = false,

    // Publication
    var publication: DocumentReference? = null,
    var publicationName: String = "",
    var publicationIconLink: String? = null,

    // Metadata
    @ServerTimestamp
    var published: Timestamp? = null,
    @ServerTimestamp
    var updated:  Timestamp? = null,
) {
    // Local only data
//    var isReadNetworkState = false
}