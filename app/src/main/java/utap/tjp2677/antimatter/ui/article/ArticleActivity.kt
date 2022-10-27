package utap.tjp2677.antimatter.ui.article

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.xeoh.android.texthighlighter.TextHighlighter
import utap.tjp2677.antimatter.Article
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.ActivityArticleBinding

class ArticleActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    companion object {
        const val articleKey = "article"
    }

    private lateinit var binding: ActivityArticleBinding
    var article: Article? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityOnePostBinding = ActivityArticleBinding.inflate(layoutInflater)
        binding = activityOnePostBinding

        setContentView(binding.root)
        setSupportActionBar(binding.bottomAppBar)

        initActionBar()
        initHighlighter()

        // Todo:  Find a non-deprecated way
        (intent.extras?.getSerializable(articleKey) as Article).let {
            binding.header.author.text = it.author.name
            binding.header.publication.text = it.publication.name
            binding.header.title.text = it.title
            binding.content.text = Html.fromHtml(it.content.toString())
        }

    }

    private fun initActionBar() {
        supportActionBar?.let {
            // Disable the default and enable the custom
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowCustomEnabled(true)
        }.also {
            val appbar = binding.bottomAppBar

            appbar.setNavigationOnClickListener {
                finish()
            }

            appbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.add -> {
                        // Handle favorite icon press
                        true
                    }
                    R.id.more -> {
                        // Handle more item (inside overflow menu) press
                        true
                    }
                    else -> false
                }
            }

            initMenuProvider()
        }


    }

    private fun initMenuProvider() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.bottom_app_bar_article, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                TODO("Not yet implemented")
            }
        }, this, Lifecycle.State.RESUMED)
    }

    private fun initHighlighter() {
        TextHighlighter()
            .setBackgroundColor(Color.YELLOW)
            .setForegroundColor(Color.YELLOW)

        binding.content.setOnClickListener {
            TextHighlighter()
                .setBackgroundColor(Color.parseColor("#FFFF00"))
                .addTarget(it)
                .highlight("Princess Heart", TextHighlighter.BASE_MATCHER)

        }

    }

}