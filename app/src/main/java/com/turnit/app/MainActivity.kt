package com.turnit.app

import android.animation.ValueAnimator
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turnit.app.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var reqCtrl: RequestController
    private lateinit var tfSpaceGrotesk: Typeface
    private val msgs = mutableListOf<ChatMsg>()
    private val models = buildModels()
    private var model = models[0] 

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)
        
        loadFonts()
        setupRecycler()
        setupModelChip()
        setupGoogleRGBBorder() // RGB Light logic
        
        // Remove blur to keep text sharp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.navView.setRenderEffect(null)
            binding.inputBorderContainer.setRenderEffect(null)
        }

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun loadFonts() {
        tfSpaceGrotesk = runCatching { Typeface.createFromAsset(assets, "fonts/SpaceGrotesk.ttf") }.getOrDefault(Typeface.SANS_SERIF)
        binding.etInput.typeface = tfSpaceGrotesk
    }

    

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        binding.etInput.setText("")
        addMsg(txt, ChatMsg.USER)
        val pos = msgs.size
        addMsg("Thinking...", ChatMsg.AI)
        reqCtrl.send(txt, model, { r -> updateMsg(pos, r.text) }, { e -> updateMsg(pos, "Error: $e") })
    }

    private fun addMsg(t: String, tp: Int) {
        msgs.add(ChatMsg(t, tp))
        binding.recyclerMessages.adapter?.notifyItemInserted(msgs.size - 1)
        binding.recyclerMessages.smoothScrollToPosition(msgs.size - 1)
    }

    // Inside MainActivity.kt, update the setupGoogleRGBBorder function:

private fun setupGoogleRGBBorder() {
    // Google's Official Brand Colors
    val googleColors = intArrayOf(
        0xFF4285F4.toInt(), // Blue
        0xFFEA4335.toInt(), // Red
        0xFFFBBC05.toInt(), // Yellow
        0xFF34A853.toInt(), // Green
        0xFF4285F4.toInt()  // Loop back to Blue
    )

    val sweep = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, googleColors).apply {
        gradientType = GradientDrawable.SWEEP_GRADIENT
        cornerRadius = dp(28).toFloat()
    }

    val rd = RotateDrawable().apply {
        drawable = sweep
        fromDegrees = 0f
        toDegrees = 360f
    }

    // Apply to the border container
    binding.inputBorderContainer.background = rd

    ValueAnimator.ofInt(0, 10000).apply {
        duration = 4000 // Slower, more "fluid" rotation like Gemini
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { rd.level = it.animatedValue as Int }
        start()
    }
}

    private fun updateMsg(at: Int, text: String) {
        if (at in msgs.indices) {
            msgs[at] = ChatMsg(text, ChatMsg.AI)
            binding.recyclerMessages.adapter?.notifyItemChanged(at)
        }
    }

    /**
     * PROBLEM 2 FIX: All 5 AI Models restored with default names.
     */
    private fun buildModels() = listOf(
        ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google - NextGen", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 2.5 Fast", "gemini-2.5-flash", "Google - Stable", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 1.5 Pro", "gemini-1.5-pro", "Google - High Intelligence", ModelOption.TYPE_GEMINI),
        ModelOption("Qwen 2.5 72B", "Qwen/Qwen2.5-72B-Instruct", "Alibaba - Powerhouse", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 2.5 32B", "Qwen/Qwen2.5-32B-Instruct", "Alibaba - Balanced", ModelOption.TYPE_HUGGINGFACE)
    )

    private fun setupRecycler() {
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = ChatAdapter(msgs)
    }

    private fun setupModelChip() {
        binding.btnModelChip.text = model.displayName
        binding.btnModelChip.setOnClickListener {
            ModelSelectionDialog(models, model.modelId) { selected ->
                model = selected
                binding.btnModelChip.text = selected.displayName
            }.show(supportFragmentManager, "model_dialog")
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()

    data class ChatMsg(val text: String, val type: Int) {
        companion object { const val USER = 0; const val AI = 1 }
    }

    inner class ChatAdapter(private val m: List<ChatMsg>) : RecyclerView.Adapter<ChatAdapter.VH>() {
        override fun getItemViewType(p: Int) = m[p].type
        override fun onCreateViewHolder(parent: ViewGroup, t: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val msg = m[pos]
            h.cu.visibility = if (msg.type == ChatMsg.USER) View.VISIBLE else View.GONE
            h.ca.visibility = if (msg.type == ChatMsg.AI) View.VISIBLE else View.GONE
            val tv = if (msg.type == ChatMsg.USER) h.tu else h.ta
            tv.text = msg.text
            tv.typeface = tfSpaceGrotesk
        }
        override fun getItemCount() = m.size
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val cu: View = v.findViewById(R.id.container_user)
            val ca: View = v.findViewById(R.id.container_ai)
            val tu: TextView = v.findViewById(R.id.tv_user_message)
            val ta: TextView = v.findViewById(R.id.tv_ai_message)
        }
    }
}
