package com.turnit.app

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turnit.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var security: SecurityManager
    private lateinit var reqCtrl: RequestController
    private lateinit var btnCtrl: ButtonController
    private lateinit var adapter: ChatAdapter

    private lateinit var tfDeltha: Typeface
    private lateinit var tfEquinox: Typeface
    private lateinit var tfSpaceGrotesk: Typeface

    private val msgs = mutableListOf<ChatMsg>()
    private val models = buildModels()
    private var model = models[0] // Default: QX Flash (Gemini 2.0)
    private var convId = UUID.randomUUID().toString()
    private var pending = -1
    private var tier = AppTier.Q

    private var svc: TurnItService? = null
    private val svcConn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            svc = (b as TurnItService.LocalBinder).get()
        }
        override fun onServiceDisconnected(n: ComponentName) { svc = null }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Initialize Logic Controllers
        security = SecurityManager(this)
        reqCtrl = RequestController(
            scope = lifecycleScope,
            geminiKey = BuildConfig.GEMINI_API_KEY,
            hfKey = BuildConfig.HUGGINGFACE_API_KEY
        )
        btnCtrl = ButtonController(binding.btnSend)

        // 2. UI Setup
        loadTypefaces()
        applyTypefaces()
        startAndBindService()
        setupDrawer()
        setupRecycler()
        setupMovingBorder()
        
        // 3. APPLY CLEAR VISION (Fixes sidebar/nav blur)
        applyHardwareClearVision() 
        
        setupModelChip()
        setupSendButton()
        boot()
    }

    private fun loadTypefaces() {
        runCatching {
            tfDeltha = Typeface.createFromAsset(assets, "fonts/Deltha.ttf")
            tfEquinox = Typeface.createFromAsset(assets, "fonts/Equinox.ttf")
            tfSpaceGrotesk = Typeface.createFromAsset(assets, "fonts/SpaceGrotesk.ttf")
        }.onFailure {
            tfDeltha = Typeface.DEFAULT_BOLD
            tfEquinox = Typeface.MONOSPACE
            tfSpaceGrotesk = Typeface.SANS_SERIF
        }
    }

    private fun applyTypefaces() {
        binding.toolbar.post {
            for (i in 0 until binding.toolbar.childCount) {
                val child = binding.toolbar.getChildAt(i)
                if (child is TextView) {
                    child.typeface = tfDeltha
                    break
                }
            }
        }
        binding.btnModelChip.typeface = tfEquinox
        binding.etInput.typeface = tfSpaceGrotesk
    }

    /**
     * CLEAR VISION PROTOCOL:
     * We explicitly clear blur from text-heavy areas (NavView).
     * We only keep a subtle glow for the neon input border.
     */
    private fun applyHardwareClearVision() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Force-remove any stuck blur from the sidebar to ensure text clarity
            binding.navView.setRenderEffect(null)

            // Apply a localized blur ONLY to the input border for the neon effect
            val neonBlur = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP)
            binding.inputBorderContainer.setRenderEffect(neonBlur)
        }
    }

    private fun setupMovingBorder() {
        val sweep = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF00D1FF.toInt(), 0xFF7B4FBF.toInt(), 0xFF00D1FF.toInt())
        ).apply {
            gradientType = GradientDrawable.SWEEP_GRADIENT
            cornerRadius = dp(28).toFloat()
        }
        val rd = RotateDrawable().apply {
            drawable = sweep
            fromDegrees = 0f; toDegrees = 360f
        }
        binding.inputBorderContainer.background = rd
        ValueAnimator.ofInt(0, 10000).apply {
            duration = 3500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { rd.level = it.animatedValue as Int }
            start()
        }
    }

    private fun startAndBindService() {
        val i = Intent(this, TurnItService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i) else startService(i)
        bindService(i, svcConn, Context.BIND_AUTO_CREATE)
    }

    private fun setupDrawer() {
        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 
            R.string.drawer_open, R.string.drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_chat -> { newConversation(); true }
                else -> false
            }.also { binding.drawerLayout.closeDrawer(GravityCompat.START) }
        }
    }

    private fun boot() {
        lifecycleScope.launch {
            security.ensureDir()
            security.getCachedUid()?.let { setHeader(tier, security.getCachedUname() ?: "Operator") }
        }
    }

    private fun setHeader(t: AppTier, name: String) {
        tier = t
        val badge = if (t == AppTier.QX) "QX" else "Q"
        supportActionBar?.title = "TurnIt $badge"
        val hv = binding.navView.getHeaderView(0)
        hv?.findViewById<TextView>(R.id.nav_header_username)?.apply {
            text = name
            typeface = tfSpaceGrotesk
        }
    }

    private fun setupRecycler() {
        adapter = ChatAdapter(msgs)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = adapter
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

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        
        binding.etInput.setText("")
        addMsg(txt, ChatMsg.USER)
        
        pending = msgs.size
        addMsg("Thinking...", ChatMsg.AI)
        
        btnCtrl.toProcessing()
        reqCtrl.send(
            prompt = txt,
            model = model,
            onResult = { r -> updateMsg(pending, r.text) },
            onError = { e -> updateMsg(pending, "Error: $e") }
        )
    }

    private fun updateMsg(at: Int, text: String) {
        if (at in msgs.indices) {
            msgs[at] = ChatMsg(text, ChatMsg.AI)
            adapter.notifyItemChanged(at)
            scrollToBottom()
        }
        btnCtrl.toIdle()
    }

    private fun addMsg(t: String, tp: Int) {
        msgs.add(ChatMsg(t, tp))
        adapter.notifyItemInserted(msgs.size - 1)
        scrollToBottom()
    }

    private fun newConversation() {
        msgs.clear()
        adapter.notifyDataSetChanged()
        convId = UUID.randomUUID().toString()
    }

    private fun scrollToBottom() {
        if (msgs.isNotEmpty()) binding.recyclerMessages.smoothScrollToPosition(msgs.size - 1)
    }

    /**
     * 2026 STABLE CONFIG:
     * Switched from retired gemini-1.5 to gemini-2.0-flash.
     */
    private fun buildModels() = listOf(
        ModelOption("QX Flash", "gemini-2.0-flash", "Rapid - Stable v1", ModelOption.TYPE_GEMINI),
        ModelOption("QX Apex 397", "Qwen/Qwen2.5-72B-Instruct", "Max - Reasoning", ModelOption.TYPE_HUGGINGFACE)
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unbindService(svcConn) }
        reqCtrl.close()
    }

    // --- Adapter & Holder ---

    data class ChatMsg(val text: String, val type: Int) {
        companion object { const val USER = 0; const val AI = 1 }
    }

    inner class ChatAdapter(private val m: List<ChatMsg>) : RecyclerView.Adapter<ChatAdapter.VH>() {
        override fun getItemViewType(p: Int) = m[p].type
        override fun onCreateViewHolder(parent: ViewGroup, t: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return VH(v)
        }
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
