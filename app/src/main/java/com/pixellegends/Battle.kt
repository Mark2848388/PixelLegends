package com.pixellegends

data class Fighter(
    val name: String,
    var hp: Int,
    val maxHp: Int,
    val atk: Int,
    val speed: Int,
    val color: Int,
    val skillName: String,
    val skillMul: Float,
    val isEnemy: Boolean,
    var skillCd: Int = 0
)

class BattleEngine(
    playerTeam: List<HeroInstance>,
    stage: Int
) {
    val allies = playerTeam.map { h ->
        val d = h.def(Catalog.heroes)
        Fighter(d.name, h.hp(Catalog.heroes), h.hp(Catalog.heroes), h.atk(Catalog.heroes), d.speed, d.color, d.skillName, d.skillMul, false)
    }.toMutableList()

    val enemies = mutableListOf<Fighter>().apply {
        val n = (2 + stage / 3).coerceAtMost(4)
        repeat(n) { i ->
            val hp = 80 + stage * 25 + i * 15
            val atk = 16 + stage * 4
            add(Fighter("Враг ${i + 1}", hp, hp, atk, 8 + i, 0xFFFF5A6A.toInt(), "Удар", 1.5f, true))
        }
        if (stage % 5 == 0) {
            val hp = 300 + stage * 40
            add(Fighter("БОСС", hp, hp, 30 + stage * 3, 10, 0xFFFF3A5A.toInt(), "Ярость", 2.2f, true))
        }
    }

    var log = "Бой · этап $stage"
    var over = false
    var won = false
    private var turn = 0

    fun living(side: List<Fighter>) = side.filter { it.hp > 0 }

    fun step() {
        if (over) return
        val all = (living(allies) + living(enemies)).sortedByDescending { it.speed }
        if (all.isEmpty()) return
        val f = all[turn % all.size]
        turn++
        if (f.hp <= 0) return
        val foes = if (f.isEnemy) living(allies) else living(enemies)
        if (foes.isEmpty()) {
            over = true
            won = living(enemies).isEmpty()
            log = if (won) "Победа!" else "Поражение"
            return
        }
        val target = foes.random()
        val useSkill = f.skillCd <= 0 && Math.random() < 0.35
        val dmg = if (useSkill) {
            f.skillCd = 3
            (f.atk * f.skillMul).toInt()
        } else {
            if (f.skillCd > 0) f.skillCd--
            f.atk + (Math.random() * 8).toInt()
        }
        target.hp = (target.hp - dmg).coerceAtLeast(0)
        log = "${f.name}: ${if (useSkill) f.skillName else "атака"} → ${target.name} -$dmg"
        if (living(enemies).isEmpty()) {
            over = true; won = true; log = "Победа! +монеты"
        } else if (living(allies).isEmpty()) {
            over = true; won = false; log = "Поражение"
        }
    }
}
