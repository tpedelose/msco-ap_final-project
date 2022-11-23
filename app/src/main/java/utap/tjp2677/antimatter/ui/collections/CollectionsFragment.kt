package utap.tjp2677.antimatter.ui.collections

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.divider.MaterialDividerItemDecoration
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.FragmentCollectionsBinding
import utap.tjp2677.antimatter.ui.lists.CollectionListAdapter
import utap.tjp2677.antimatter.utils.toPx


class CollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private var collectionListAdapter: CollectionListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.fetchCollections()
        }

        initCollectionsRecyclerView()

        // Interaction Listeners
        binding.refresh.setOnRefreshListener {
            viewModel.fetchCollections()
        }

        // Observers
        viewModel.observeCollections().observe(viewLifecycleOwner) {
            collectionListAdapter?.submitList(it)
            binding.refresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initCollectionsRecyclerView() {
        val rv = binding.collectionList

        // Content
        collectionListAdapter = CollectionListAdapter(viewModel) {
            // Todo: Don't add to backstack
            viewModel.setOpenCollection(it)
            findNavController().navigate(R.id.action_navigation_collections_to_navigation_feed)
        }
        rv.adapter = collectionListAdapter

        // Style
        val dividerThickness = 6.toPx.toInt()

        // Make first element larger than the others
        // Inspired by:  https://stackoverflow.com/questions/36514887/layoutmanager-for-recyclerview-grid-with-different-cell-width
        val lm = GridLayoutManager(this.context, 2)
        // Todo?: Probably better to make this an extension class
        lm.spanSizeLookup = object: SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (position) {
                    0 -> 2
                    else -> 1
                }
            }
        }
        rv.layoutManager = lm

        // The following is a kluge to get dividers working with gridlayouts.
        binding.collectionList.setPadding(
            binding.collectionList.paddingLeft,
            binding.collectionList.paddingTop,
            binding.collectionList.paddingRight - dividerThickness,
            binding.collectionList.paddingBottom
        )

        listOf(GridLayoutManager.HORIZONTAL, GridLayoutManager.VERTICAL).forEach { orientation ->
            val divider = MaterialDividerItemDecoration(requireContext(), orientation)
            divider.dividerThickness = dividerThickness
            divider.dividerColor = Color.TRANSPARENT
            rv.addItemDecoration(divider)
        }
    }
}