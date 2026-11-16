package com.pixellegends

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class GameView(context: Context) : View(context), Runnable {
    private var thread: Thread? = null
    @Volatile private var running = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Color.parseColor("#0A0E1A")

    enum class Screen { HOME, TEAM, BATTLE, GACHA, RESULT }

    private var screen = Screen.HOME
    private var save = SaveData()
    private var engine: BattleEngine? = null
    private var battleTimer = 0f
    private var toast = ""
    private var toastT = 0f
    private val buttons = mutableListOf<Btn>()

    data class Btn(val x: Float, val y: Float, val w: Float, val h: Float, val label: String, val color: Int, val action: () -> Unit)

    private fun toast(s: String) { toast = s; toastT = 2.2f }

    private fun teamHeroes(): List<HeroInstance> =
        save.team.filter { it.isNotEmpty() }.mapNotNull { id -> save.heroes.find { it.defId == id } }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        thread = Thread(this).also { it.start() }
    }

    override fun onDetachedFromWindow() {
        running = false
        try { thread?.join(300) } catch (_: Exception) {}
        super.onDetachedFromWindow()
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            update(dt)
            postInvalidate()
            try { Thread.sleep(16) } catch (_: Exception) {}
        }
    }

    private fun update(dt: Float) {
        toastT -= dt
        if (screen == Screen.BATTLE) {
            battleTimer += dt
            if (battleTimer > 0.85f) {
                battleTimer = 0f
                engine?.step()
                val e = engine
                if (e != null && e.over) {
                    if (e.won) {
                        val reward = 40 + save.stage * 12
                        save.coins += reward
                        save.stage++
                        if (save.stage % 3 == 0) save.chests++
                        toast("+$reward монет")
                    }
                    screen = Screen.RESULT
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        buttons.clear()
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(bg)
        // stars
        paint.color = Color.WHITE
        paint.alpha = 120
        for (i in 0 until 40) {
            val sx = ((i * 97) % 1000) / 1000f * w
            val sy = ((i * 53) % 1000) / 1000f * h
            canvas.drawCircle(sx, sy, 2f, paint)
        }
        paint.alpha = 255
        when (screen) {
            Screen.HOME -> drawHome(canvas, w, h)
            Screen.TEAM -> drawTeam(canvas, w, h)
            Screen.BATTLE -> drawBattle(canvas, w, h)
            Screen.GACHA -> drawGacha(canvas, w, h)
            Screen.RESULT -> drawResult(canvas, w, h)
        }
        if (toastT > 0) {
            paint.color = Color.parseColor("#FFD34A")
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 36f
            canvas.drawText(toast, w / 2, h * 0.12f, paint)
        }
    }

    private fun btn(x: Float, y: Float, bw: Float, bh: Float, label: String, color: Int, action: () -> Unit) {
        buttons.add(Btn(x, y, bw, bh, label, color, action))
    }

    private fun drawButtons(canvas: Canvas) {
        for (b in buttons) {
            paint.style = Paint.Style.FILL
            paint.color = b.color
            paint.alpha = 40
            canvas.drawRoundRect(b.x, b.y, b.x + b.w, b.y + b.h, 24f, 24f, paint)
            paint.alpha = 255
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = b.color
            canvas.drawRoundRect(b.x, b.y, b.x + b.w, b.y + b.h, 24f, 24f, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = min(b.h * 0.38f, 40f)
            canvas.drawText(b.label, b.x + b.w / 2, b.y + b.h * 0.62f, paint)
        }
    }

    private fun drawHome(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.parseColor("#FFD34A")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 56f
        canvas.drawText("PIXEL LEGENDS", w / 2, h * 0.12f, paint)
        paint.textSize = 28f
        paint.color = Color.parseColor("#9EC9FF")
        canvas.drawText("пиксельный RAID-like", w / 2, h * 0.17f, paint)
        paint.color = Color.WHITE
        paint.textSize = 30f
        canvas.drawText("Монеты: ${save.coins}   Гемы: ${save.gems}", w / 2, h * 0.24f, paint)
        canvas.drawText("Этап: ${save.stage}   Сундуки: ${save.chests}", w / 2, h * 0.29f, paint)

        val bw = w * 0.8f
        val bx = (w - bw) / 2
        btn(bx, h * 0.38f, bw, 100f, "▶  КАМПАНИЯ", Color.parseColor("#4A9AFF")) {
            val team = teamHeroes()
            if (team.isEmpty()) toast("Собери отряд")
            else {
                engine = BattleEngine(team, save.stage)
                battleTimer = 0f
                screen = Screen.BATTLE
            }
        }
        btn(bx, h * 0.52f, bw, 90f, "Отряд", Color.parseColor("#3AD86A")) { screen = Screen.TEAM }
        btn(bx, h * 0.64f, bw, 90f, "Сундук (${save.chests})", Color.parseColor("#FFC030")) { screen = Screen.GACHA }
        btn(bx, h * 0.76f, bw, 90f, "Клан-босс (скоро)", Color.parseColor("#C04AFF")) { toast("В следующем обновлении") }
        drawButtons(canvas)
    }

    private fun drawTeam(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 44f
        canvas.drawText("Отряд", w / 2, 80f, paint)
        var y = 140f
        save.heroes.forEach { hi ->
            val d = hi.def(Catalog.heroes)
            paint.color = d.color
            canvas.drawRoundRect(40f, y, w - 40f, y + 90f, 16f, 16f, paint)
            paint.color = Color.BLACK
            paint.textSize = 32f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("${d.name}  Lv${hi.level}  ATK ${hi.atk(Catalog.heroes)}", 60f, y + 55f, paint)
            val inTeam = save.team.contains(hi.defId)
            btn(w - 220f, y + 15f, 160f, 60f, if (inTeam) "В отряде" else "Взять", if (inTeam) Color.GRAY else Color.parseColor("#4A9AFF")) {
                if (inTeam) {
                    val idx = save.team.indexOf(hi.defId)
                    if (idx >= 0) save.team[idx] = ""
                } else {
                    val slot = save.team.indexOfFirst { it.isEmpty() }
                    if (slot >= 0) save.team[slot] = hi.defId else toast("Отряд полон (4)")
                }
            }
            y += 105f
        }
        btn(40f, h - 120f, w - 80f, 80f, "Назад", Color.parseColor("#6A7A9A")) { screen = Screen.HOME }
        drawButtons(canvas)
    }

    private fun drawBattle(canvas: Canvas, w: Float, h: Float) {
        val e = engine ?: return
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 28f
        canvas.drawText(e.log, w / 2, 60f, paint)

        fun drawSide(list: List<Fighter>, baseY: Float) {
            val living = list
            val gap = w / (living.size + 1)
            living.forEachIndexed { i, f ->
                val x = gap * (i + 1)
                paint.color = f.color
                paint.alpha = if (f.hp > 0) 255 else 80
                canvas.drawRect(x - 40f, baseY - 70f, x + 40f, baseY + 20f, paint)
                paint.alpha = 255
                // hp bar
                paint.color = Color.DKGRAY
                canvas.drawRect(x - 50f, baseY + 30f, x + 50f, baseY + 42f, paint)
                paint.color = if (f.isEnemy) Color.parseColor("#FF5A6A") else Color.parseColor("#3AD86A")
                val ratio = f.hp.toFloat() / f.maxHp
                canvas.drawRect(x - 50f, baseY + 30f, x - 50f + 100f * ratio, baseY + 42f, paint)
                paint.color = Color.WHITE
                paint.textSize = 22f
                canvas.drawText(f.name, x, baseY - 80f, paint)
                canvas.drawText("${f.hp}/${f.maxHp}", x, baseY + 65f, paint)
            }
        }
        drawSide(e.allies, h * 0.72f)
        drawSide(e.enemies, h * 0.35f)
        btn(w * 0.25f, h - 110f, w * 0.5f, 70f, "Пропустить ход", Color.parseColor("#4A9AFF")) {
            engine?.step()
        }
        drawButtons(canvas)
    }

    private fun drawGacha(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.parseColor("#FFD34A")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 44f
        canvas.drawText("Сундук героев", w / 2, 100f, paint)
        paint.color = Color.WHITE
        paint.textSize = 30f
        canvas.drawText("Сундуков: ${save.chests}", w / 2, 160f, paint)
        btn(w * 0.15f, h * 0.4f, w * 0.7f, 100f, "Открыть (1 сундук)", Color.parseColor("#FFC030")) {
            if (save.chests <= 0) { toast("Нет сундуков"); return@btn }
            save.chests--
            val roll = Math.random()
            val pool = when {
                roll > 0.92 -> Catalog.heroes.filter { it.rarity == Rarity.LEGENDARY }
                roll > 0.70 -> Catalog.heroes.filter { it.rarity == Rarity.EPIC }
                roll > 0.40 -> Catalog.heroes.filter { it.rarity == Rarity.RARE }
                else -> Catalog.heroes.filter { it.rarity == Rarity.COMMON }
            }
            val got = pool.random()
            val existing = save.heroes.find { it.defId == got.id }
            if (existing != null) {
                existing.level++
                toast("${got.name} +1 ур.!")
            } else {
                save.heroes.add(HeroInstance(got.id))
                toast("Новый герой: ${got.name}!")
            }
        }
        btn(w * 0.15f, h * 0.55f, w * 0.7f, 80f, "Купить сундук (100🪙)", Color.parseColor("#4A9AFF")) {
            if (save.coins < 100) toast("Мало монет")
            else { save.coins -= 100; save.chests++; toast("+1 сундук") }
        }
        btn(40f, h - 120f, w - 80f, 80f, "Назад", Color.parseColor("#6A7A9A")) { screen = Screen.HOME }
        drawButtons(canvas)
    }

    private fun drawResult(canvas: Canvas, w: Float, h: Float) {
        val e = engine
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 52f
        paint.color = if (e?.won == true) Color.parseColor("#FFD34A") else Color.parseColor("#FF5A6A")
        canvas.drawText(if (e?.won == true) "ПОБЕДА" else "ПОРАЖЕНИЕ", w / 2, h * 0.35f, paint)
        btn(w * 0.15f, h * 0.5f, w * 0.7f, 90f, "В меню", Color.parseColor("#4A9AFF")) {
            engine = null
            screen = Screen.HOME
        }
        if (e?.won == true) {
            btn(w * 0.15f, h * 0.62f, w * 0.7f, 90f, "Дальше", Color.parseColor("#3AD86A")) {
                val team = teamHeroes()
                engine = BattleEngine(team, save.stage)
                battleTimer = 0f
                screen = Screen.BATTLE
            }
        }
        drawButtons(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            for (i in buttons.size - 1 downTo 0) {
                val b = buttons[i]
                if (x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h) {
                    b.action()
                    return true
                }
            }
        }
        return true
    }
}
