package utap.tjp2677.antimatter.ui.player

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import utap.tjp2677.antimatter.MainViewModel
import utap.tjp2677.antimatter.R
import utap.tjp2677.antimatter.databinding.FragmentPlayerBinding

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!  // This property is only valid between onCreateView and onDestroyView.
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeNowPlaying().observe(viewLifecycleOwner) {
            if (it == null) { return@observe }
            binding.title.text = it.title
            binding.title.isSelected = true
        }

        viewModel.observeIsPlayingStatus().observe(viewLifecycleOwner) { isPlaying ->
            when (isPlaying) {
                true -> binding.playbackButton.setIconResource(R.drawable.ic_outline_pause_24)
                false -> binding.playbackButton.setIconResource(R.drawable.ic_outline_play_arrow_24)
            }
        }

        binding.playbackButton.setOnClickListener {
            viewModel.getIsPlayingStatus()?.let {
                if (it) {
                    viewModel.stopPlaying()
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}