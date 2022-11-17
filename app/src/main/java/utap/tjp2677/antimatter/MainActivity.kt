package utap.tjp2677.antimatter

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import utap.tjp2677.antimatter.authentication.AuthInit
import utap.tjp2677.antimatter.authentication.models.Article
import utap.tjp2677.antimatter.databinding.ActivityMainBinding
import utap.tjp2677.antimatter.ui.article.ArticleFragment
import utap.tjp2677.antimatter.ui.article.ArticleToolbarFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // Authentication
            AuthInit(viewModel, signInLauncher)

            // Draw behind navigation and status bars
            window.setDecorFitsSystemWindows(false)

            // Initialize TTS Engine
            // TODO:  Make a class to handle player
            viewModel.ttsEngine = initTTSEngine()
        }

        // Initialize Navigation
        initNavigation()
    }

    private fun initNavigation() {
        val navView: BottomNavigationView = binding.navView
        // https://stackoverflow.com/questions/50502269/illegalstateexception-link-does-not-have-a-navcontroller-set
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)
//        navView.minimumHeight = 176
    }

    private fun initTTSEngine(): TextToSpeech {
        // Pass in context and the listener.
        return TextToSpeech(this) { status ->
            // set our locale only if init was success.
//            if (status == TextToSpeech.SUCCESS) {
//                .language = textToSpeechEngine.defaultVoice.locale
//            }
        }
    }

    fun navigateToArticleDetail(article: Article) {
        viewModel.setOpenedArticle(article)
        supportFragmentManager.commit {
            replace(R.id.nav_view, ArticleToolbarFragment(), "OpenArticleToolbar")
            replace(R.id.nav_host_fragment_activity_main, ArticleFragment(), "OpenArticle")
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            addToBackStack("OpenArticle")
        }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.updateUser()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.d("MainActivity", "sign in failed $result")
            }
        }
}