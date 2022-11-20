package utap.tjp2677.antimatter.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.google.android.material.color.MaterialColors
import com.google.android.material.divider.MaterialDividerItemDecoration
import utap.tjp2677.antimatter.MainActivity
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.FragmentArticlesListBinding
import utap.tjp2677.antimatter.databinding.FragmentFeedBinding
import utap.tjp2677.antimatter.ui.lists.ArticleItemTouchHelper
import utap.tjp2677.antimatter.ui.lists.ArticleListAdapter
import utap.tjp2677.antimatter.utils.toPx

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ArticleListAdapter
    private lateinit var articlesListBinding: FragmentArticlesListBinding

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

        // Interaction Listeners
        articlesListBinding.refresh.setOnRefreshListener {
            viewModel.fetchArticles(limit=10)
        }

        // Observers
        viewModel.observeArticles().observe(viewLifecycleOwner) {
            adapter.submitList(it)
            articlesListBinding.refresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        val rv = articlesListBinding.feedList

        // Content
        adapter = ArticleListAdapter { position ->
            val article = viewModel.getArticleAt(position)
            viewModel.setOpenedArticle(article)
            findNavController().navigate(R.id.navigate_to_article_detail)
//            viewModel.setArticleReadStatus(position, true)
        }
        rv.adapter = adapter

        // Interactions
        val touchHelper = ItemTouchHelper(
            ArticleItemTouchHelper(adapter, this::markAsReadCallback, this::addToReadLaterCallback)
        )
        touchHelper.attachToRecyclerView(rv)
    }

    fun markAsReadCallback(position: Int) {
        viewModel.toggleArticleReadStatus(position)
    }

    fun addToReadLaterCallback(position: Int) {
//        viewModel.toggleArticleInReadLater(position)
    }

}