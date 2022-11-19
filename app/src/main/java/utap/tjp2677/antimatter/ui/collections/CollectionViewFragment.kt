package utap.tjp2677.antimatter.ui.collections

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.FragmentArticlesListBinding
import utap.tjp2677.antimatter.databinding.FragmentCollectionViewBinding
import utap.tjp2677.antimatter.ui.lists.ArticleItemTouchHelper
import utap.tjp2677.antimatter.ui.lists.ArticleListAdapter


class CollectionViewFragment : Fragment() {

    private var _binding: FragmentCollectionViewBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewModel: CollectionViewViewModel
    private lateinit var adapter: ArticleListAdapter
    private lateinit var articlesListBinding: FragmentArticlesListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[CollectionViewViewModel::class.java]
        _binding = FragmentCollectionViewBinding.inflate(inflater, container, false)
        articlesListBinding = FragmentArticlesListBinding.bind(binding.root)  // Weirdness with <merge/>:  https://betterprogramming.pub/exploring-viewbinding-in-depth-598925821e41
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.fetchArticles(limit=10)
        }

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

    private fun initRecyclerView() {
        val rv = articlesListBinding.feedList

        // Context
        adapter = ArticleListAdapter { position ->
            val article = viewModel.getArticleAt(position)
            mainViewModel.setOpenedArticle(article)
            findNavController().navigate(R.id.action_collectionViewFragment_to_article_view)
        }
        rv.adapter = adapter

        // Interactions
        val touchHelper = ItemTouchHelper(
            ArticleItemTouchHelper(adapter, this::markAsReadCallback, this::addToReadLaterCallback)
        )
        touchHelper.attachToRecyclerView(rv)
    }

    fun markAsReadCallback(position: Int) {

    }

    fun addToReadLaterCallback(position: Int) {

    }
}