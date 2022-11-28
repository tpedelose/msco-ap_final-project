package utap.tjp2677.antimatter.ui.collections

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.EditCollectionViewBinding
import utap.tjp2677.antimatter.databinding.FragmentCollectionsBinding
import utap.tjp2677.antimatter.ui.lists.CollectionListAdapter
import utap.tjp2677.antimatter.utils.toPx


class CollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()
    private var collectionListAdapter: CollectionListAdapter? = null
    private var initialFABBottomMargin: Int? = null
    private var initialCollectionListBottomMargin: Int? = null

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

        binding.extendedFab.setOnClickListener {

            val dialogBinding = EditCollectionViewBinding.inflate(LayoutInflater.from(binding.root.context), null, false)
            dialogBinding.emojiField.editText?.filters = arrayOf(
                EmojiFilter(dialogBinding.emojiField.context)
//                InputFilter.LengthFilter(9),
            )

            dialogBinding.emojiField.editText?.addTextChangedListener {
                // Always start with EmojiFilter
                val filters = mutableListOf<InputFilter>(
                    EmojiFilter(dialogBinding.emojiField.context)
                )

                // If text, add LengthFilter to cap to one input (since unicode Emoji vary in length)
                if (!it.isNullOrBlank()) {
                    filters.add(InputFilter.LengthFilter(it.length))
                }

                // Set filters
                dialogBinding.emojiField.editText?.filters = filters.toTypedArray()
            }

            MaterialAlertDialogBuilder(binding.root.context)
                // TODO? Check if collection with name already exists
                .setTitle("Create collection")
                .setIcon(R.drawable.ic_twotone_library_add_24)
                .setView(dialogBinding.root)
                .setNegativeButton("Cancel") { _ /*dialog*/, _ /*which*/ ->
                    // Do nothing
                }
                .setPositiveButton("Create") { _ /*dialog*/, _ /*which*/ ->
                    // Create a Collection
                    val collectionName = dialogBinding.textField.editText?.text.toString()
                    val emojiIcon = dialogBinding.emojiField.editText?.text.toString()

                    when {
                        collectionName.isBlank() -> {
                            Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                        }
                        emojiIcon.isBlank() -> {
                            Toast.makeText(context, "Emoji icon cannot be blank", Toast.LENGTH_SHORT).show()
                        }
                        collectionName.isNotBlank() && emojiIcon.isNotBlank() -> {
                            viewModel.createCollection(collectionName, emojiIcon)
                        }
                    }
                }
                .show()
        }

        // Observers
        viewModel.observeCollections().observe(viewLifecycleOwner) {
            collectionListAdapter?.submitList(it)
            Log.d("Collections", it[0].toString())
            binding.refresh.isRefreshing = false
        }

        viewModel.observePlayerIsActive().observe(viewLifecycleOwner) { isActive ->
            // Todo: avoid hard-setting of height + margins
            val buffer = (56+12).toPx

            // Keep the FAB from behind the Player
            binding.extendedFab.updateLayoutParams<MarginLayoutParams> {
                if (isActive) {  // Give FAB extra room
                    setMargins(leftMargin, topMargin, rightMargin, (bottomMargin + buffer).toInt())
                } else {  // Reset
                    initialFABBottomMargin?.let {
                        setMargins(leftMargin, topMargin, rightMargin, it)
                    }
                }
            }

            // Keep adapter list from behind the FAB
            binding.collectionList.updateLayoutParams<MarginLayoutParams> {
                if (isActive) {  // Give extra room
                    setMargins(leftMargin, topMargin, rightMargin, (bottomMargin + buffer).toInt())
                } else {  // Reset
                    initialCollectionListBottomMargin?.let {
                        setMargins(leftMargin, topMargin, rightMargin, it)
                    }
                }
            }
        }

        initialFABBottomMargin = binding.extendedFab.marginBottom
        initialCollectionListBottomMargin = binding.collectionList.marginBottom

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

class EmojiFilter(val context: Context) : InputFilter {
    // From:  https://github.com/rpandey1234/EmojiStatus/blob/5d1eb217bbc6294f8a7ff5515f9138ef2e9417b5/app/src/main/java/edu/stanford/rkpandey/emojistatus/MainActivity.kt#:~:text=inner%20class%20EmojiFilter%20%3A%20InputFilter%20%7B

    private companion object {
        private val TAG = "EmojiFilter"
        private val VALID_CHAR_TYPES = listOf(
            Character.NON_SPACING_MARK, // 6
//                Character.DECIMAL_DIGIT_NUMBER, // 9
//                Character.LETTER_NUMBER, // 10
//                Character.OTHER_NUMBER, // 11
            Character.SPACE_SEPARATOR, // 12
            Character.FORMAT, // 16
            Character.SURROGATE, // 19
//                Character.DASH_PUNCTUATION, // 20
//                Character.START_PUNCTUATION, // 21
//                Character.END_PUNCTUATION, // 22
//                Character.CONNECTOR_PUNCTUATION, // 23
//                Character.OTHER_PUNCTUATION, // 24
            Character.MATH_SYMBOL, // 25
            Character.CURRENCY_SYMBOL, //26
            Character.MODIFIER_SYMBOL, // 27
            Character.OTHER_SYMBOL // 28
        ).map { it.toInt() }.toSet()
    }

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dStart: Int,
        dEnd: Int
    ): CharSequence {
        if (source.isNullOrBlank()) { return "" }

        Log.d(TAG, "$source is ${source.length} chars long")

        for (inputChar in source) {
            val type = Character.getType(inputChar)
            if (!VALID_CHAR_TYPES.contains(type)) {
                // This gives an error (but doesn't crash) for some reason? Maybe the context is bad?
                Toast.makeText(context, "Only supported emojis are allowed", Toast.LENGTH_SHORT).show()
                return ""
            }
        }
        // The CharSequence being added is a valid emoji! Allow it to be added
        return source
    }
}