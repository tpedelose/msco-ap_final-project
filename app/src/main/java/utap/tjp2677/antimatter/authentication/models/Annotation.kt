package utap.tjp2677.antimatter.authentication.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.PropertyName

data class Annotation(
    // IDs
    @DocumentId
    var firestoreID: String = "",

    // Content
    var start: Int = -1,
    var end: Int = -1,
    var text: String? = null
)