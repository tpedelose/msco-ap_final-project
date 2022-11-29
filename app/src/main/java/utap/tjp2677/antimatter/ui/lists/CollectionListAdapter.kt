package utap.tjp2677.antimatter.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.databinding.CollectionItemBinding


class CollectionListAdapter(private val viewModel: MainViewModel, val onClickCallback: (Collection) -> Unit)
    : ListAdapter<Collection, CollectionListAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(val collectionBinding: CollectionItemBinding) :
        RecyclerView.ViewHolder(collectionBinding.root) {
            init {
                collectionBinding.root.setOnClickListener {
                    val collection = viewModel.getCollectionAt(absoluteAdapterPosition)
                    onClickCallback(collection)
                }

                collectionBinding.root.setOnLongClickListener {
                    val item = currentList[absoluteAdapterPosition]
                    // Todo:  figure out way of overriding vibrate for a REJECT when long click on immortal collection
                    if (item.immortal) {
                        // Can't delete immortal collections
                        // Show feedback so user knows what to expect
                        Toast.makeText(it.context,
                            "\"${item.name}\" cannot be deleted", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        // Make and show dialog
                        MaterialAlertDialogBuilder(it.context)
                            .setTitle("Delete collection?")
                            .setMessage("This action is irreversible.")
                            .setNegativeButton("Cancel") { _ /*dialog*/, _ /*which*/ ->
                                // Do nothing
                            }
                            .setPositiveButton("Delete") { _ /*dialog*/, _ /*which*/ ->
                                // Delete collection
                                viewModel.deleteCollection(item)
                            }
                            .show()
                    }
                    true  // Returning 'true' will initiate vibration feedback and block OnClick
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowBinding = CollectionItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ViewHolder(rowBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.collectionBinding

        binding.icon.text = item.icon
        binding.title.text = item.name

        // Change text style for "most important" collections (or reset)
        val (textStyle, bgcolor) = when (position) {
            0, 1, 2 -> {
                Pair(
                    R.style.TextAppearance_Material3_TitleLarge, // R.style.TextAppearance_Material3_HeadlineSmall
                    MaterialColors.getColor(binding.root, R.attr.colorSecondaryContainer)
                )
            }
            else -> {
                Pair(
                    R.style.TextAppearance_Material3_TitleMedium,
                    MaterialColors.getColor(binding.root, R.attr.colorSurfaceVariant)
                )
            }
        }
        binding.root.explicitStyle
        binding.title.setTextAppearance(textStyle)
        binding.root.setCardBackgroundColor(bgcolor)
    }

    class Diff : DiffUtil.ItemCallback<Collection>() {
        override fun areItemsTheSame(oldItem: Collection, newItem: Collection): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Collection, newItem: Collection): Boolean {
            return oldItem.name == newItem.name
        }
    }
}