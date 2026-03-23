package com.turnit.app

import android.animation.ValueAnimator
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turnit.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var reqCtrl: RequestController
    private val msgs = mutableListOf<Pair<String, Int>>()
    private val models = buildModels()
    private var model = models[0] // Default: Gemini 3 Flash

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize API controller with environment keys
        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)
        
        setupRecycler()
        
        // Post to toolbar to ensure view dimensions are calculated for the gradient shader
        binding.toolbar.post { setupAnimatedRGBLogo() }
        
        // Android 12+ (API 31) Blur cleanup to ensure text clarity over Nebula background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.navView.setRenderEffect(null)
            binding.inputBorderContainer.setRenderEffect(null)
        }

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    /**
     * Logic for the flowing Red-Green-Blue gradient on the "TurnIt" logo.
     */
    private fun setupAnimatedRGBLogo() {
        val logoText = binding.toolbar.getChildAt(0) as? TextView ?: return
        val colors = intArrayOf(
            Color.RED, 
            Color.GREEN, 
            Color.BLUE, 
            Color.RED // Loop back to Red for seamless flow
        )
        
        val textWidth = logoText.paint.measureText(logoText.text.toString())
        val shader = LinearGradient(0f, 0f, textWidth, 0f, colors, null, Shader.TileMode.REPEAT)
        logoText.paint.shader = shader
        
        val matrix = Matrix()
        ValueAnimator.ofFloat(0f, textWidth).apply {
            duration = 3000 // Flow speed
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { 
                matrix.setTranslate(it.animatedValue as Float, 0f)
                shader.setLocalMatrix(matrix)
                logoText.invalidate() 
            }
            start()
        }
    }

    /**
     * Complete AI Model List with default names and Novita routing for Qwen.
     */
    private fun buildModels() = listOf(
        ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google - NextGen", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 2.5 Fast", "gemini-2.5-flash", "Google - Stable", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 1.5 Pro", "gemini-1.5-pro", "Google - Advanced", ModelOption.TYPE_GEMINI),
        ModelOption("Qwen 2.5 72B", "Qwen/Qwen2.5-72B-Instruct:novita", "Alibaba - Max", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 3.5 397B", "Qwen/Qwen3.5-397B-A17B:novita", "Alibaba - Vision", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 3.5 35B", "Qwen/Qwen3.5-35B-A3B:novita", "Alibaba - Vision", ModelOption.TYPE_HUGGINGFACE)
    )

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        
        binding.etInput.setText("")
        
        // Add User message
        msgs.add(txt to 0) 
        val pos = msgs.size
        
        // Add AI placeholder
        msgs.add("Thinking..." to 1) 
        
        binding.recyclerMessages.adapter?.let {
            it.notifyItemRangeInserted(pos - 1, 2)
            binding.recyclerMessages.smoothScrollToPosition(msgs.size - 1)
        }
        
        reqCtrl.send(txt, model, null, { r -> 
            msgs[pos] = r to 1
            runOnUiThread { binding.recyclerMessages.adapter?.notifyItemChanged(pos) }
        }, { e -> 
            msgs[pos] = "Error: $e" to 1
            runOnUiThread { binding.recyclerMessages.adapter?.notifyItemChanged(pos) }
        })
    }

    private fun setupRecycler() {
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { 
            stackFromEnd = true 
        }
        binding.recyclerMessages.adapter = ChatAdapter(msgs)
    }

    // --- Recycler Components ---

    inner class ChatAdapter(private val m: List<Pair<String, Int>>) : RecyclerView.Adapter<VH>() {
        override fun getItemViewType(p: Int) = m[p].second
        
        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_chat_message, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, p: Int) {
            val (text, type) = m[p]
            h.tv.text = text
            
            // Glassmorphism implementation:
            h.tv.setBackgroundResource(R.drawable.bg_glass_bubble)
            h.tv.setTextColor(Color.WHITE)
            
            // Layout alignment based on sender type
            val params = h.tv.layoutParams as ViewGroup.MarginLayoutParams
            if (type == 0) { // User
                params.marginStart = 80
                params.marginEnd = 16
                h.tv.backgroundTintList = android.content.res.ColorStateList.valueOf(0x804285F4.toInt())
            } else { // AI
                params.marginStart = 16
                params.marginEnd = 80
                h.tv.backgroundTintList = null
            }
            h.tv.layoutParams = params
        }

        override fun getItemCount() = m.size
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) { 
        // Must match the ID in item_chat_message.xml
        val tv: TextView = v.findViewById(R.id.tv_message) 
    }
}
