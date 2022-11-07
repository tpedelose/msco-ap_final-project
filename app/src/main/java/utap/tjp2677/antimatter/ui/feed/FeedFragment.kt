package utap.tjp2677.antimatter.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.databinding.FragmentFeedBinding
import utap.tjp2677.antimatter.ui.lists.ArticleItemTouchHelper
import utap.tjp2677.antimatter.ui.lists.ArticleListAdapter

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by viewModels()
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

        initRecyclerView()

        feedViewModel.observeArticles().observe(viewLifecycleOwner) {
            adapter?.submitList(it)
//            adapter.notifyDataSetChanged() // Need for update on refresh
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        val rv = binding.feedList

        // Content
        adapter = ArticleListAdapter(viewModel)
        rv.adapter = adapter

        // Style
        val divider = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        rv.addItemDecoration(divider)

        // Interactions
        adapter?.let {
            val touchHelper = ItemTouchHelper(ArticleItemTouchHelper(rv.context, it))
            touchHelper.attachToRecyclerView(rv)
        }

    }

//    private fun initTouchHelper(adapter: ArticleListAdapter): ItemTouchHelper {
//        // Drag:  Slow, controlled gesture
//        val dragDirs = ItemTouchHelper.START or ItemTouchHelper.END
//        // Swipe:  Fast gesture
//        val swipeDirs = ItemTouchHelper.START or ItemTouchHelper.END
//
//        val simpleItemTouchCallback =
//            object : ItemTouchHelper.SimpleCallback(0, swipeDirs)
//            {
//                var hapticsTriggered = false
//                var reachedMax = false
//
//                override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
//                    return (1/5f)
//                }
//
//                override fun onMove(recyclerView: RecyclerView,
//                                    viewHolder: RecyclerView.ViewHolder,
//                                    target: RecyclerView.ViewHolder): Boolean {
//                    return false
//                }
//
//                override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
//                                      direction: Int) {
//                    // Code for horizontal swipe.
//                    Log.d(javaClass.simpleName, "Swipe dir $direction")
//                    val position = viewHolder.adapterPosition
//
////                    when(direction) {
////                        ItemTouchHelper.START -> TODO("Mark as read")
////                        ItemTouchHelper.END -> TODO("Save to Read Later")
////                    }
//
//                }
//
//                override fun onChildDraw(
//                    c: Canvas,
//                    recyclerView: RecyclerView,
//                    viewHolder: RecyclerView.ViewHolder,
//                    dX: Float,
//                    dY: Float,
//                    actionState: Int,
//                    isCurrentlyActive: Boolean
//                ) {
//
//                    Log.d("THIS", abs(dX).toString())
//
//                    val view = viewHolder.itemView
//                    var xD = dX
//
//                    if (!isCurrentlyActive) {
//                        recyclerView.setBackgroundColor(Color.TRANSPARENT)
//                        hapticsTriggered = false
//
//                        if (reachedMax) {
//                            view.x = 0f
//                        }
//
//                    } else if (dX != 0f) {
//
//                        val swipeThreshold = this.getSwipeThreshold(viewHolder)
//                        val maxDX = view.width * swipeThreshold
//                        xD = when(dX >= 0) {
//                            true -> min(dX, maxDX)
//                            false -> max(dX, -maxDX)
//                        }
//
//                        when (xD) {
//                            maxDX -> {
//                                recyclerView.setBackgroundColor(Color.GREEN)
//                                if (view.isHapticFeedbackEnabled && !hapticsTriggered) {
//                                    // TODO: Try/catch
//                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
//                                    hapticsTriggered = true
//                                    reachedMax = true
//                                }
//                            }
//                            -maxDX -> {
//                                recyclerView.setBackgroundColor(Color.RED)
//                                if (view.isHapticFeedbackEnabled && !hapticsTriggered) {
//                                    // TODO: Try/catch
//                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
//                                    hapticsTriggered = true
//                                    reachedMax = true
//                                }
//                            }
//                        }
//                    }
//
//                    super.onChildDraw(
//                        c,
//                        recyclerView,
//                        viewHolder,
//                        xD,
//                        dY,
//                        actionState,
//                        isCurrentlyActive
//                    )
//
//                }
//            }
//
////        val itemTouchCallback = ArticleItemTouchHelper(view.context, adapter)
//
////        return ItemTouchHelper(itemTouchCallback)
//        false
//    }


}