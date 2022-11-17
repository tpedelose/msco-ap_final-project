package utap.tjp2677.antimatter.ui.lists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
//import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.authentication.models.Publication
import utap.tjp2677.antimatter.databinding.FeedItemBinding
import utap.tjp2677.antimatter.databinding.FollowItemBinding
import utap.tjp2677.antimatter.utils.toDp


class FollowListAdapter(private val viewModel: MainViewModel, val onClickCallback: (Publication) -> Unit)
    : ListAdapter<Publication, FollowListAdapter.ViewHolder>(Diff()) {

    private var glideOptions = RequestOptions()
        .fitCenter()
        .transform(
            // Todo:  Find a way to do this in XML styles. Inconsistent between devices.
            RoundedCorners (20.toDp.toInt())
        )

    inner class ViewHolder(val subscriptionBinding: FollowItemBinding) :
        RecyclerView.ViewHolder(subscriptionBinding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowBinding = FollowItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ViewHolder(rowBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.subscriptionBinding

        item.iconLink?.let {
            Glide.with(binding.root.context)
                .asBitmap() // Try to display animated Gifs and video still
                .load(it)
                .apply(glideOptions)
                .into(binding.image)
        }

        binding.title.text = item.title
    }

    class Diff : DiffUtil.ItemCallback<Publication>() {
        override fun areItemsTheSame(oldItem: Publication, newItem: Publication): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Publication, newItem: Publication): Boolean {
            return oldItem.title == newItem.title
        }
    }
}