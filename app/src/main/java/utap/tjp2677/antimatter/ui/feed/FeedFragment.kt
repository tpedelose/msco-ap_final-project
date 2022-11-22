package utap.tjp2677.antimatter.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.databinding.FragmentArticlesListBinding
import utap.tjp2677.antimatter.databinding.FragmentFeedBinding
import utap.tjp2677.antimatter.ui.lists.ArticleItemTouchHelper
import utap.tjp2677.antimatter.ui.lists.ArticleListAdapter


class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ArticleListAdapter
    private lateinit var articlesListBinding: FragmentArticlesListBinding

    private val fetchLimit = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        articlesListBinding = FragmentArticlesListBinding.bind(binding.root)  // Weirdness with <merge/>:  https://betterprogramming.pub/exploring-viewbinding-in-depth-598925821e41
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        // Observers
        viewModel.observeOpenCollection().observe(viewLifecycleOwner) {
            articlesListBinding.titleText.title = it.name
            fetchWithFilter(it)
        }

        viewModel.observeArticles().observe(viewLifecycleOwner) {
            adapter.submitList(it)
            articlesListBinding.refresh.isRefreshing = false
            // Todo: Placeholder if no data in list
        }

        // Listeners
        articlesListBinding.refresh.setOnRefreshListener {
            val collection = viewModel.getOpenCollection()
            fetchWithFilter(collection!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchWithFilter(collection: Collection) {
        val cid = collection.firestoreID
        val isReadFilter = collection.filter["isRead"]?.let { it as Boolean }
        viewModel.fetchArticles(cid, fetchLimit, isReadFilter)
    }

    private fun initRecyclerView() {
        val rv = articlesListBinding.feedList

        // Content
        adapter = ArticleListAdapter { position ->
            val article = viewModel.getArticleAt(position)
            viewModel.setOpenedArticle(article)
            findNavController().navigate(R.id.navigate_to_article_detail)
//            viewModel.setArticleReadStatus(position, true)  // Todo:  turn into setting
        }
        rv.adapter = adapter

        // Interactions
        val touchHelper = ItemTouchHelper(
            ArticleItemTouchHelper(adapter, this::markAsReadCallback, this::toggleQueueCallback)
        )
        touchHelper.attachToRecyclerView(rv)
    }

    fun markAsReadCallback(position: Int) {
        viewModel.toggleArticleReadStatus(position) {
            // I guess this is the only way of doing a smooth update?
            adapter.notifyItemChanged(position)
        }
    }

    fun toggleQueueCallback(position: Int) {
        val cid = viewModel.getOpenCollection()?.firestoreID
        viewModel.addArticleToCollection(cid!!, "Queue", position)
    }

}