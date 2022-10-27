package utap.tjp2677.antimatter.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.Repository

class FeedViewModel: ViewModel() {

    private var repository = Repository()
    private var articleList = MutableLiveData<List<Article>>()

    init {
        reset()
    }

    private fun reset() {
        articleList.apply {
            value = repository.fetchData()
        }
    }

    fun observeArticles(): LiveData<List<Article>> {
        return articleList
    }

}