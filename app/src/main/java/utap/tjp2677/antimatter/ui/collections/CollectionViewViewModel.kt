package utap.tjp2677.antimatter.ui.collections

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import utap.tjp2677.antimatter.authentication.FirestoreHelper
import utap.tjp2677.antimatter.authentication.models.Article

class CollectionViewViewModel : ViewModel() {

    private var firestoreHelper = FirestoreHelper()
    private var articleList = MutableLiveData<List<Article>>()

    fun fetchArticles(limit: Int?) {
        firestoreHelper.fetchArticles(articleList, limit, 0, 0)
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

    fun getArticleAt(position: Int): Article {
        return articleList.value!![position]
    }
}