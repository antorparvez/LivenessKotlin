package antor.parvez.liveness

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import antor.parvez.liveness.databinding.ActivityMainBinding
import com.bumptech.glide.Glide


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        lifecycle.currentState
        setContentView(binding.root)

        val livenessLauncher = registerForActivityResult(FaceDetectionActivity.ResultContract()) {


            Log.d("TAG", "onCreate: ${it.toString()}")

            binding.recyclerView.adapter = ImageAdapter(it.orEmpty())
        }

        binding.startBtn.setOnClickListener {
            livenessLauncher.launch(null)
        }
    }
}

class ImageAdapter(private val images: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val imageView = ImageView(parent.context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return object : RecyclerView.ViewHolder(imageView) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val image = images[position]
        Glide.with(holder.itemView).load(image).into(holder.itemView as ImageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }
}