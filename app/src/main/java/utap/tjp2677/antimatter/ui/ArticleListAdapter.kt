package utap.tjp2677.antimatter.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.databinding.FeedItemBinding


class ArticleListAdapter(private val mainViewModel: MainViewModel)
    : ListAdapter<Article, ArticleListAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(val articleBinding: FeedItemBinding) :
        RecyclerView.ViewHolder(articleBinding.root) {
            init {
                articleBinding.root.setOnClickListener {
                    val article = mainViewModel.getArticleAt(adapterPosition)
                    mainViewModel.openArticle(it.context, article)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowBinding = FeedItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ViewHolder(rowBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.articleBinding

        binding.author.text = item.author.name
        binding.publication.text = item.publication.name
        binding.title.text = item.title
    }

    class Diff : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.title == newItem.title
//                    && oldItem.rating == newItem.rating
        }
    }


}