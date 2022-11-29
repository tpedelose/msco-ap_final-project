package utap.tjp2677.antimatter

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.marginBottom
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.FirebaseAuth
import utap.tjp2677.antimatter.authentication.AuthInit
import utap.tjp2677.antimatter.databinding.ActivityMainBinding
import utap.tjp2677.antimatter.ui.player.PlayerFragment
import utap.tjp2677.antimatter.utils.toDp
import utap.tjp2677.antimatter.utils.toPx


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val color = SurfaceColors.SURFACE_2.getColor(this)
        window.navigationBarColor = color
        window.statusBarColor = color

        if (savedInstanceState == null) {

            // Watch for change in user
            viewModel.observeUser().observe(this) {
                if (it == null) { return@observe }

                // Fetch starting data
                Log.d("ObserveUser", "${it.uid} logged in")

                viewModel.initHelper()
                viewModel.fetchCollectionAsOpen("Inbox")
                viewModel.fetchCollections()
                viewModel.fetchSubscriptions()
            }

            // Watch for playing article
            viewModel.observeNowPlaying().observe(this) {
                // ignore if null
                if (it == null) { return@observe }

                // ignore if fragment already exists
                // Todo:  I think this is causing it to not be recreated in some cases
                val frag = supportFragmentManager.findFragmentById(R.id.fragment_player) as? PlayerFragment
                if (frag != null) { return@observe }

                // Else, create the fragment!
                Log.d("Create player?","Player does not exist, creating")

                supportFragmentManager.commit {
                    add(R.id.fragment_player, PlayerFragment(), "Player")
                    // TRANSIT_FRAGMENT_FADE calls for the Fragment to fade away
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    viewModel.setPlayerIsActive(true)
                }
            }

            // Draw behind navigation and status bars
            window.setDecorFitsSystemWindows(false)

            // Initialize TTS Engine
            // TODO:  Make a class/service to handle player
            viewModel.ttsEngine = initTTSEngine()

            // Start authentication
            AuthInit(viewModel, signInLauncher)
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
        navView.setOnApplyWindowInsetsListener(null)

        // ♥ this:  https://developer.android.com/guide/navigation/navigation-ui#argument
        // Todo? Animation:  https://stackoverflow.com/questions/54087740/how-to-hide-bottomnavigationview-on-android-navigation-lib
        navController.addOnDestinationChangedListener { _, _, arguments ->
            navView.isGone = arguments?.getBoolean("HideAppBar", false) == true
        }
//        navView.minimumHeight = 176
    }

    private fun initTTSEngine(): TextToSpeech {

        val tts = TextToSpeech(this) {
            // could do things here like set language, voice, etc.
            // https://stackoverflow.com/questions/35049850/unresolved-reference-inside-anonymous-kotlin-listener
        }

        val utterListener = object: UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {

            }

            override fun onDone(utteranceId: String) {
                Log.d("UTTER", utteranceId)
                if (utteranceId == "tts-final") {
                    viewModel.postIsPlayingStatus(false)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onError(utteranceId: String, errorCode: Int) {

            }
        }
        tts.setOnUtteranceProgressListener(utterListener)

        return tts
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
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