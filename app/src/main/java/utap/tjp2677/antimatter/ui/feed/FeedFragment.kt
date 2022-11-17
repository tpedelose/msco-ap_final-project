package utap.tjp2677.antimatter.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration
import utap.tjp2677.antimatter.MainActivity
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.databinding.FragmentFeedBinding
import utap.tjp2677.antimatter.ui.lists.ArticleItemTouchHelper
import utap.tjp2677.antimatter.ui.lists.ArticleListAdapter
import utap.tjp2677.antimatter.utils.toPx

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private var adapter: ArticleListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.fetchArticles(limit=10)
        }

        initRecyclerView()

        // Interaction Listeners
        binding.refresh.setOnRefreshListener {
            viewModel.fetchArticles(limit=10)
        }

        // Observers
        viewModel.observeArticles().observe(viewLifecycleOwner) {
            adapter?.submitList(it)
            binding.refresh.isRefreshing = false
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        val rv = binding.feedList

        // Content
        adapter = ArticleListAdapter(viewModel) {
            (requireActivity() as MainActivity).navigateToArticleDetail(it)
        }
        rv.adapter = adapter

        // Style
//        val divider = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        // TODO: Better match above color
        val divider = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.dividerInsetStart = 80.toPx.toInt()
        divider.isLastItemDecorated = false
//        divider.dividerColor = com.google.android.material.R.attr.colorOnSurfaceVariant
        divider.dividerColor = com.google.android.material.R.attr.colorSurfaceVariant
        rv.addItemDecoration(divider)


        // Interactions
        adapter?.let {
            val touchHelper = ItemTouchHelper(ArticleItemTouchHelper(rv.context, it))
            touchHelper.attachToRecyclerView(rv)
        }

    }

}