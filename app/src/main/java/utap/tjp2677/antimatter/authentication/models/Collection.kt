package utap.tjp2677.antimatter.authentication.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentReference

data class Collection (
    // IDs
    @DocumentId
    var firestoreID: String = "",

    // Content
    var name: String = "",
    var icon: String = "",
    var order: Int = 100,
    var immortal: Boolean = false,
)