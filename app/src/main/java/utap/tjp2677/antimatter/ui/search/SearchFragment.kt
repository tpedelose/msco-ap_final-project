package utap.tjp2677.antimatter.ui.search

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.databinding.FragmentSearchBinding
import utap.tjp2677.antimatter.ui.lists.FollowListAdapter
import utap.tjp2677.antimatter.utils.toPx


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private var followListAdapter: FollowListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.fetchSubscriptions()
        }

        initSubscriptionsRecyclerView()

        // Interactions
        binding.testButton.setOnClickListener {
            viewModel.openSettings(it.context)
        }

        binding.refresh.setOnRefreshListener {
            viewModel.fetchSubscriptions()
        }

        // Observers
        viewModel.observeSubscriptions().observe(viewLifecycleOwner) {
            followListAdapter?.submitList(it)
            binding.refresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initSubscriptionsRecyclerView() {
        val rv = binding.followList
        followListAdapter = FollowListAdapter(viewModel) {}
        rv.adapter = followListAdapter

        val dividerThickness = 6.toPx.toInt()

        val divider = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
        divider.dividerThickness = dividerThickness
        divider.dividerColor = Color.TRANSPARENT
        rv.addItemDecoration(divider)
    }
}