package utap.tjp2677.antimatter.ui.lists

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.authentication.models.Collection
import utap.tjp2677.antimatter.databinding.CollectionItemBinding


class CollectionListAdapter(private val viewModel: MainViewModel, val onClickCallback: (Collection) -> Unit)
    : ListAdapter<Collection, CollectionListAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(val collectionBinding: CollectionItemBinding) :
        RecyclerView.ViewHolder(collectionBinding.root) {}

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

        // Change text style for "most important" collection (or resest)
        val textStyle = when (position) {
            0 -> com.google.android.material.R.style.TextAppearance_M3_Sys_Typescale_TitleLarge
            else -> com.google.android.material.R.style.TextAppearance_M3_Sys_Typescale_TitleMedium
        }
        binding.title.setTextAppearance(textStyle)

        // Click listeners
        binding.root.setOnClickListener {
            val collection = viewModel.getCollectionAt(position)
            onClickCallback(collection)
        }
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