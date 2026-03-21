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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
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

    private lateinit var binding:  ActivityMainBinding
    private lateinit var toggle:   ActionBarDrawerToggle
    private lateinit var security: SecurityManager
    private lateinit var reqCtrl:  RequestController
    private lateinit var btnCtrl:  ButtonController
    private lateinit var adapter:  ChatAdapter

    // Custom typefaces loaded once from assets
    private lateinit var tfDeltha:       Typeface   // Headers / branding
    private lateinit var tfEquinox:      Typeface   // Chips / buttons
    private lateinit var tfSpaceGrotesk: Typeface   // Body / chat text

    private val msgs    = mutableListOf<ChatMsg>()
    private val models  = buildModels()
    private var model   = models[0]
    private var convId  = UUID.randomUUID().toString()
    private var pending = -1
    private var tier    = AppTier.Q

    private var svc: TurnItService? = null
    private val svcConn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            svc = (b as TurnItService.LocalBinder).get()
        }
        override fun onServiceDisconnected(n: ComponentName) { svc = null }
    }

    // ---- Lifecycle ----------------------------------------------------

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding  = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        security = SecurityManager(this)
        reqCtrl  = RequestController(
            scope      = lifecycleScope,
            geminiKey  = BuildConfig.GEMINI_API_KEY,
            hfKey      = BuildConfig.HUGGINGFACE_API_KEY
        )
        btnCtrl = ButtonController(binding.btnSend)

        loadTypefaces()           // 1. Load fonts from assets
        applyTypefaces()          // 2. Bind fonts to views
        startAndBindService()     // 3. Foreground service
        setupDrawer()             // 4. 3-line nav drawer
        setupRecycler()           // 5. Chat list
        setupMovingBorder()       // 6. Neon border animator
        applyHardwareBlur()       // 7. RenderEffect (SDK 33+)
        setupModelChip()          // 8. Model selector
        setupSendButton()         // 9. Morphing send/stop
        boot()                    // 10. Handshake + auth
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(svcConn)
        reqCtrl.close()
    }

    // ---- Custom typefaces -------------------------------------------
    //
    // Fonts are bundled in app/src/main/assets/fonts/
    // Deltha.ttf      -> sharp geometric headers
    // Equinox.ttf     -> sci-fi display (chips, buttons)
    // SpaceGrotesk.ttf -> readable body text

    private fun loadTypefaces() {
        tfDeltha       = Typeface.createFromAsset(assets, "fonts/Deltha.ttf")
        tfEquinox      = Typeface.createFromAsset(assets, "fonts/Equinox.ttf")
        tfSpaceGrotesk = Typeface.createFromAsset(assets, "fonts/SpaceGrotesk.ttf")
    }

    private fun applyTypefaces() {
        // Toolbar title -> Deltha
        binding.toolbar.post {
            for (i in 0 until binding.toolbar.childCount) {
                val child = binding.toolbar.getChildAt(i)
                if (child is TextView) {
                    child.typeface = tfDeltha
                    break
                }
            }
        }
        // Model chip -> Equinox
        binding.btnModelChip.typeface = tfEquinox
        // Input hint/text -> Space Grotesk
        binding.etInput.typeface = tfSpaceGrotesk
    }

    // ---- Hardware blur (SDK 33+) ------------------------------------
    //
    // Two blur surfaces:
    //   input_border_container : 30x30 = strong "neon glow" blur
    //   nav_view               : 20x20 = frosted glass drawer
    //
    // On API < 31 the views render without blur (no crash, graceful
    // degradation).

    private fun applyHardwareBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            // Input container: 30f x 30f gaussian blur
            binding.inputBorderContainer.setRenderEffect(
                RenderEffect.createBlurEffect(
                    30f, 30f, Shader.TileMode.CLAMP
                )
            )

            // Navigation drawer: 20f x 20f frosted glass
            binding.navView.setRenderEffect(
                RenderEffect.createBlurEffect(
                    20f, 20f, Shader.TileMode.CLAMP
                )
            )
        }
    }

    // ---- Moving neon border -----------------------------------------

    private fun setupMovingBorder() {
        val sweep = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                0xFF00D1FF.toInt(),
                0xFF7B4FBF.toInt(),
                0xFF1A0B2E.toInt(),
                0xFF7B4FBF.toInt(),
                0xFF00D1FF.toInt()
            )
        ).apply {
            gradientType = GradientDrawable.SWEEP_GRADIENT
            cornerRadius = dp(28).toFloat()
        }
        val rd = RotateDrawable().apply {
            drawable = sweep
            fromDegrees = 0f; toDegrees = 360f
            isPivotXRelative = true; pivotX = 0.5f
            isPivotYRelative = true; pivotY = 0.5f
        }
        binding.inputBorderContainer.background = rd
        ValueAnimator.ofInt(0, 10000).apply {
            duration     = 3500
            repeatCount  = ValueAnimator.INFINITE
            repeatMode   = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { rd.level = it.animatedValue as Int }
            start()
        }
        // Re-apply blur AFTER background is set (SDK 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.inputBorderContainer.setRenderEffect(
                RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
            )
        }
    }

    // ---- Service ----------------------------------------------------

    private fun startAndBindService() {
        val i = Intent(this, TurnItService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i) else startService(i)
        bindService(i, svcConn, Context.BIND_AUTO_CREATE)
    }

    // ---- 3-line navigation drawer -----------------------------------

    private fun setupDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_chat -> { newConversation(); true }
                R.id.nav_history  -> { showHistory();    true }
                R.id.nav_api_key  -> {
                    toast("Use setUserGeminiKey() / setUserHfKey()")
                    true
                }
                R.id.nav_logout -> {
                    security.clearSession()
                    setHeader(AppTier.Q, "Disconnected")
                    true
                }
                else -> false
            }.also { binding.drawerLayout.closeDrawer(GravityCompat.START) }
        }
    }

    // ---- Boot / handshake -------------------------------------------

    private fun boot() {
        lifecycleScope.launch {
            security.ensureDir()
            when (val hs = security.readHandshake()) {
                is HandshakeResult.Valid   ->
                    setHeader(tier, hs.data.username)
                is HandshakeResult.Missing ->
                    security.getCachedUid()?.let {
                        setHeader(tier, security.getCachedUname() ?: "Operator")
                    } ?: toast("Authentication required.")
                is HandshakeResult.Invalid ->
                    toast("Identity error: ${hs.reason}")
            }
        }
    }

    // ---- Header / tier badge ----------------------------------------

    private fun setHeader(t: AppTier, name: String) {
        tier = t
        val badge = when (t) {
            AppTier.QX -> "QX"; AppTier.Q -> "Q"; else -> ""
        }
        supportActionBar?.title =
            if (badge.isEmpty()) "TurnIt QX" else "TurnIt $badge"
        val hv = binding.navView.getHeaderView(0)
        hv?.findViewById<TextView>(R.id.nav_header_username)?.apply {
            text     = name
            typeface = tfSpaceGrotesk
        }
        hv?.findViewById<TextView>(R.id.nav_header_tier)?.apply {
            text      = badge
            typeface  = tfEquinox
            visibility = if (badge.isEmpty()) View.GONE else View.VISIBLE
            setTextColor(
                if (t == AppTier.QX) 0xFFF87171.toInt()
                else 0xFF00D1FF.toInt()
            )
        }
    }

    // ---- User API key overrides -------------------------------------

    fun setUserGeminiKey(key: String?) {
        reqCtrl.setUserGeminiKey(key)
        toast(if (!key.isNullOrEmpty()) "Direct mode: unlimited"
              else "Built-in key: 10 RPM")
    }

    fun setUserHfKey(key: String?) {
        reqCtrl.setUserHfKey(key)
        toast(if (!key.isNullOrEmpty()) "Inference: direct"
              else "Inference: 10 RPM")
    }

    // ---- Chat recycler ----------------------------------------------

    private fun setupRecycler() {
        adapter = ChatAdapter(msgs)
        binding.recyclerMessages.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = adapter
    }

    // ---- Model chip -------------------------------------------------

    private fun setupModelChip() {
        binding.btnModelChip.text = model.displayName
        binding.btnModelChip.setOnClickListener {
            ModelSelectionDialog(models, model.modelId) { sel ->
                model = sel
                binding.btnModelChip.text = sel.displayName
                pulseChip()
            }.show(supportFragmentManager, "model_select")
        }
    }

    private fun pulseChip() {
        val ld = binding.btnModelChip.background
            as? android.graphics.drawable.LayerDrawable ?: return
        val g  = ld.getDrawable(0) as? GradientDrawable ?: return
        ValueAnimator.ofInt(60, 255, 60).apply {
            duration = 500
            addUpdateListener {
                g.alpha = it.animatedValue as Int
                binding.btnModelChip.invalidate()
            }
            start()
        }
    }

    // ---- Send/Stop button -------------------------------------------

    private fun setupSendButton() {
        binding.btnSend.setOnTouchListener { v, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) btnCtrl.animatePress()
            if (ev.action == MotionEvent.ACTION_UP)   v.performClick()
            true
        }
        binding.btnSend.setOnClickListener {
            when (btnCtrl.state) {
                ButtonState.IDLE       -> sendMessage()
                ButtonState.PROCESSING -> stopRequest()
                ButtonState.CANCELLING -> Unit
            }
        }
    }

    // ---- Message flow -----------------------------------------------

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        binding.etInput.setText("")
        addMsg(txt, ChatMsg.USER)
        pending = msgs.size
        addMsg("Processing...", ChatMsg.AI)
        persistLog("User", txt)
        btnCtrl.toProcessing()
        reqCtrl.send(
            prompt   = txt,
            model    = model,
            onResult = { r ->
                val badge = "\u26a1 ${r.latencyMs}ms"
                updateMsg(pending, "${r.text}\n\n$badge")
            },
            onError  = { e -> updateMsg(pending, "Error: $e") }
        )
    }

    private fun stopRequest() {
        reqCtrl.cancel()
        btnCtrl.toCancelling {
            val i = pending
            if (i >= 0 && i < msgs.size) {
                msgs[i] = ChatMsg("Cancelled.", ChatMsg.AI)
                adapter.notifyItemChanged(i)
            }
            pending = -1
        }
    }

    private fun updateMsg(at: Int, text: String) {
        if (at >= 0 && at < msgs.size) {
            msgs[at] = ChatMsg(text, ChatMsg.AI)
            adapter.notifyItemChanged(at)
            scrollToBottom()
        }
        persistLog("AI", text)
        btnCtrl.toIdle()
        pending = -1
    }

    // ---- Helpers ----------------------------------------------------

    private fun addMsg(t: String, tp: Int) {
        msgs.add(ChatMsg(t, tp))
        adapter.notifyItemInserted(msgs.size - 1)
        scrollToBottom()
    }

    private fun persistLog(r: String, t: String) = lifecycleScope.launch {
        security.appendLog(convId, r, t)
    }

    private fun newConversation() {
        convId = UUID.randomUUID().toString()
        msgs.clear(); adapter.notifyDataSetChanged()
    }

    private fun showHistory() = lifecycleScope.launch {
        toast("${security.listConversations().size} sessions stored")
    }

    private fun scrollToBottom() {
        val p = msgs.size - 1
        if (p >= 0) binding.recyclerMessages.post {
            binding.recyclerMessages.smoothScrollToPosition(p)
        }
    }

    private fun toast(m: String) =
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density + .5f).toInt()

    // ---- Model definitions (proprietary TurnIt QX names only) -------

    private fun buildModels() = listOf(
        ModelOption(
            "QX Flash",
            "gemini-1.5-flash",
            "Quantum  -  Rapid multimodal",
            ModelOption.TYPE_GEMINI
        ),
        ModelOption(
            "QX Apex 397",
            "Qwen/Qwen2-72B-Instruct",
            "Quantum  -  Max reasoning",
            ModelOption.TYPE_HUGGINGFACE
        ),
        ModelOption(
            "QX Core 35",
            "Qwen/Qwen1.5-32B-Chat",
            "Quantum  -  Balanced",
            ModelOption.TYPE_HUGGINGFACE
        ),
        ModelOption(
            "QX Micro 9",
            "Qwen/Qwen1.5-7B-Chat",
            "Quantum  -  Low latency",
            ModelOption.TYPE_HUGGINGFACE
        )
    )

    // ---- Data + Adapter ---------------------------------------------

    data class ChatMsg(val text: String, val type: Int) {
        companion object { const val USER = 0; const val AI = 1 }
    }

    inner class ChatAdapter(private val m: MutableList<ChatMsg>)
        : RecyclerView.Adapter<ChatAdapter.VH>() {
        override fun getItemViewType(p: Int) = m[p].type
        override fun onCreateViewHolder(parent: ViewGroup, t: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, pos: Int) {
            val msg = m[pos]
            if (msg.type == ChatMsg.USER) {
                h.cu.visibility = View.VISIBLE
                h.ca.visibility = View.GONE
                h.tu.apply { text = msg.text; typeface = tfSpaceGrotesk }
            } else {
                h.cu.visibility = View.GONE
                h.ca.visibility = View.VISIBLE
                h.ta.apply { text = msg.text; typeface = tfSpaceGrotesk }
            }
        }
        override fun getItemCount() = m.size
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val cu: LinearLayout = v.findViewById(R.id.container_user)
            val ca: LinearLayout = v.findViewById(R.id.container_ai)
            val tu: TextView     = v.findViewById(R.id.tv_user_message)
            val ta: TextView     = v.findViewById(R.id.tv_ai_message)
        }
    }
}
