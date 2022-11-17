package utap.tjp2677.antimatter.authentication.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentReference

data class Publication (
    // IDs
    @DocumentId
    var firestoreID: String = "",

    // Content
    var title: String = "",
    var subtitle: String = "",
    var link: String = "",
    var iconLink: String? = null,

    // Feed Metadata
    @ServerTimestamp
    var updated: Timestamp? = null,
    var source: String? = null,
    var etag: String? = null,

    // Service Metadata
    @ServerTimestamp
    var crawled: Timestamp? = null,
    @ServerTimestamp
    var lastModified: Timestamp? = null,
)