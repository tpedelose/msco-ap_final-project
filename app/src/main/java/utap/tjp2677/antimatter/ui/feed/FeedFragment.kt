package utap.tjp2677.antimatter.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.databinding.FragmentFeedBinding
import utap.tjp2677.antimatter.ui.ArticleListAdapter

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by viewModels()

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

        val adapter = ArticleListAdapter(viewModel)
        binding.feedList.adapter = adapter

        feedViewModel.observeArticles().observe(viewLifecycleOwner) {
            adapter.submitList(it)
//            adapter.notifyDataSetChanged() // Need for update on refresh
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}