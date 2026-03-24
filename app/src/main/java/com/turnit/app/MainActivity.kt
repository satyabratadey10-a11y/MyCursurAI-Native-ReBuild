package com.turnit.app

import android.animation.ValueAnimator
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turnit.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var reqCtrl: RequestController
    private val msgs = mutableListOf<Pair<String, Int>>()
    private val models = buildModels()
    private var model = models[0] // Default: Gemini 3.1 Pro

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)
        
        setupSidebar() 
        setupRecycler()
        
        binding.toolbar.post { setupAnimatedRGBLogo() } 
        setupRGBInputBorder() 
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.navView.setRenderEffect(null)
            binding.inputBorderContainer.setRenderEffect(null)
        }

        // FIXED: Now references the function below correctly
        binding.btnModelChip.setOnClickListener { showModelPicker() }
        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupSidebar() {
        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.drawer_open, R.string.drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_chat -> { msgs.clear(); binding.recyclerMessages.adapter?.notifyDataSetChanged() }
                R.id.nav_api_key -> { /* Open API Settings */ }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupRGBInputBorder() {
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.RED)
        val sweep = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            gradientType = android.graphics.drawable.GradientDrawable.SWEEP_GRADIENT
            cornerRadius = 24f
        }
        val rd = android.graphics.drawable.RotateDrawable().apply { drawable = sweep; fromDegrees = 0f; toDegrees = 360f }
        binding.inputBorderContainer.background = rd
        ValueAnimator.ofInt(0, 10000).apply {
            duration = 3000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { rd.level = it.animatedValue as Int }
            start()
        }
    }

    private fun setupAnimatedRGBLogo() {
        val logoText = binding.toolbar.getChildAt(0) as? TextView ?: return
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.RED)
        val textWidth = logoText.paint.measureText(logoText.text.toString())
        val shader = LinearGradient(0f, 0f, textWidth, 0f, colors, null, Shader.TileMode.REPEAT)
        logoText.paint.shader = shader
        val matrix = Matrix()
        ValueAnimator.ofFloat(0f, textWidth).apply {
            duration = 2500; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { matrix.setTranslate(it.animatedValue as Float, 0f); shader.setLocalMatrix(matrix); logoText.invalidate() }
            start()
        }
    }

    /**
     * FIXED: This was missing in your last build.
     */
    private fun showModelPicker() {
        ModelSelectionDialog(models, model.modelId) { selected ->
            model = selected
            binding.btnModelChip.text = selected.displayName
        }.show(supportFragmentManager, "model_picker")
    }

    /**
     * 2026 FREE PREVIEW MODELS (MARCH UPDATE)
     */
    private fun buildModels() = listOf(
        ModelOption("Gemini 3.1 Pro", "gemini-3.1-pro-preview", "Google - NextGen", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google - Rapid", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 3.1 Flash Lite", "gemini-3.1-flash-lite-preview", "Google - Fast", ModelOption.TYPE_GEMINI),
        ModelOption("Qwen 3.5 397B", "Qwen/Qwen3.5-397B-A17B:novita", "Alibaba - Vision Pro", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 3.5 35B", "Qwen/Qwen3.5-35B-A3B:novita", "Alibaba - Vision Lite", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 2.5 72B", "Qwen/Qwen2.5-72B-Instruct:novita", "Alibaba - Balanced", ModelOption.TYPE_HUGGINGFACE)
    )

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        binding.etInput.setText("")
        msgs.add(txt to 0) // User (Type 0)
        val pos = msgs.size
        msgs.add("Thinking..." to 1) // AI (Type 1)
        binding.recyclerMessages.adapter?.notifyItemRangeInserted(pos - 1, 2)
        binding.recyclerMessages.smoothScrollToPosition(msgs.size - 1)
        reqCtrl.send(txt, model, null, { r -> updateMsg(pos, r) }, { e -> updateMsg(pos, "Error: $e") })
    }

    private fun updateMsg(pos: Int, text: String) {
        runOnUiThread {
            msgs[pos] = text to 1
            binding.recyclerMessages.adapter?.notifyItemChanged(pos)
        }
    }

    private fun setupRecycler() {
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = ChatAdapter(msgs)
    }

    inner class ChatAdapter(private val m: List<Pair<String, Int>>) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_chat_message, p, false))
        override fun onBindViewHolder(h: VH, p: Int) {
            val (text, type) = m[p]
            h.tv.text = text
            h.tv.setBackgroundResource(R.drawable.bg_glass_bubble) 
            
            val params = h.tv.layoutParams as android.widget.LinearLayout.LayoutParams
            params.gravity = if (type == 0) android.view.Gravity.END else android.view.Gravity.START 
            h.tv.layoutParams = params
        }
        override fun getItemCount() = m.size
    }
    class VH(v: View) : RecyclerView.ViewHolder(v) { val tv: TextView = v.findViewById(R.id.tv_message) }
}
