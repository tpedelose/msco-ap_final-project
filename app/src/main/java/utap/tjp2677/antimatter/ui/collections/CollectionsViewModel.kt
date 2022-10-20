package utap.tjp2677.antimatter.ui.collections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CollectionsViewModel: ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is collection Fragment"
    }
    val text: LiveData<String> = _text
}