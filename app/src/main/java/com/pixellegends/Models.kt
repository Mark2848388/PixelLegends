package com.pixellegends

enum class Rarity { COMMON, RARE, EPIC, LEGENDARY }

data class HeroDef(
    val id: String,
    val name: String,
    val rarity: Rarity,
    val maxHp: Int,
    val atk: Int,
    val speed: Int,
    val color: Int,
    val skillName: String,
    val skillMul: Float
)

data class HeroInstance(
    val defId: String,
    var level: Int = 1,
    var xp: Int = 0
) {
    fun def(catalog: List<HeroDef>) = catalog.first { it.id == defId }
    fun hp(catalog: List<HeroDef>) = (def(catalog).maxHp * (1 + (level - 1) * 0.12f)).toInt()
    fun atk(catalog: List<HeroDef>) = (def(catalog).atk * (1 + (level - 1) * 0.1f)).toInt()
}

object Catalog {
    val heroes = listOf(
        HeroDef("spark", "Искра", Rarity.COMMON, 120, 28, 12, 0xFFFFD34A.toInt(), "Вспышка", 1.8f),
        HeroDef("shade", "Тень", Rarity.COMMON, 100, 34, 16, 0xFF9AA0FF.toInt(), "Укол", 1.6f),
        HeroDef("oak", "Дуб", Rarity.RARE, 200, 22, 8, 0xFF3AD86A.toInt(), "Щит", 1.2f),
        HeroDef("ember", "Уголь", Rarity.RARE, 140, 40, 11, 0xFFFF6A3A.toInt(), "Жар", 2.0f),
        HeroDef("nova", "Нова", Rarity.EPIC, 160, 48, 14, 0xFFFF5AD0.toInt(), "Взрыв", 2.4f),
        HeroDef("aegis", "Эгида", Rarity.EPIC, 260, 26, 9, 0xFF5AD0FF.toInt(), "Бастион", 1.4f),
        HeroDef("d56", "Д-56", Rarity.LEGENDARY, 220, 55, 15, 0xFF7FD0FF.toInt(), "Ядро", 2.8f),
        HeroDef("void", "Войд", Rarity.LEGENDARY, 180, 60, 18, 0xFFC04AFF.toInt(), "Разрыв", 3.0f)
    )
}

data class SaveData(
    var coins: Int = 500,
    var gems: Int = 20,
    var stage: Int = 1,
    var heroes: MutableList<HeroInstance> = mutableListOf(
        HeroInstance("spark"),
        HeroInstance("shade"),
        HeroInstance("oak")
    ),
    var team: MutableList<String> = mutableListOf("spark", "shade", "oak", ""),
    var chests: Int = 2
)
