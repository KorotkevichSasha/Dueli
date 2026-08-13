package com.example.duelingo.utils

import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.response.DuelResponse
import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.dto.response.UserInDuelResponse
import java.util.UUID

object OfflineDuelFactory {
    fun create(difficulty: String): DuelFoundEvent {
        val level = difficulty.uppercase().takeIf { it in setOf("EASY", "MEDIUM", "HARD") } ?: "MEDIUM"
        val opponent = UserInDuelResponse(UUID.randomUUID(), names.random(), 700, "default:${(1..10).random()}")
        val player = UserInDuelResponse(UUID.randomUUID(), "You", 700, "default:1")
        val duration = when (level) { "EASY" -> 180_000L; "HARD" -> 75_000L; else -> 120_000L }
        return DuelFoundEvent(DuelResponse(UUID.randomUUID().toString(), player, opponent,
            questions.getValue(level).shuffled()), opponent.userId.toString(), level, duration, false)
    }

    private fun choice(id: String, level: String, text: String, answer: String, vararg distractors: String) =
        QuestionDetailedResponse(id, level, "FILL_IN_CHOICE", text, (listOf(answer) + distractors).shuffled(), listOf(answer))
    private fun sentence(id: String, level: String, russian: String, english: String) =
        QuestionDetailedResponse(id, level, "SENTENCE_CONSTRUCTION", russian, english.split(' ').shuffled(), english.split(' '))

    private val questions = mapOf(
        "EASY" to listOf(
            choice("oe1", "EASY", "She ___ coffee every morning.", "drinks", "drink", "drinking"),
            choice("oe2", "EASY", "We ___ at home yesterday.", "were", "was", "are"),
            choice("oe3", "EASY", "This book is ___ than that one.", "more interesting", "interest", "most interesting"),
            choice("oe4", "EASY", "I have ___ apple in my bag.", "an", "a", "the"),
            choice("oe5", "EASY", "They ___ football on Sundays.", "play", "plays", "played now"),
            sentence("oe6", "EASY", "Я обычно хожу в школу пешком.", "I usually walk to school"),
            sentence("oe7", "EASY", "Моей сестре нравится читать книги.", "My sister likes reading books"),
            sentence("oe8", "EASY", "Сегодня мы изучаем английский язык.", "We are learning English today"),
            choice("oe9", "EASY", "There ___ two windows in the room.", "are", "is", "be"),
            choice("oe10", "EASY", "Can you ___ me, please?", "help", "helps", "helping")
        ),
        "MEDIUM" to listOf(
            choice("om1", "MEDIUM", "If it rains, we ___ at home.", "will stay", "stayed", "would stayed"),
            choice("om2", "MEDIUM", "I have lived here ___ 2022.", "since", "for", "during"),
            choice("om3", "MEDIUM", "The report ___ by Friday.", "must be finished", "must finish", "must finished"),
            choice("om4", "MEDIUM", "She asked me where I ___ the keys.", "had put", "have put", "putted"),
            choice("om5", "MEDIUM", "He is good ___ explaining difficult ideas.", "at", "in", "on"),
            sentence("om6", "MEDIUM", "Собрание уже началось.", "The meeting has already started"),
            sentence("om7", "MEDIUM", "Не могли бы вы подсказать мне дорогу?", "Could you tell me the way"),
            sentence("om8", "MEDIUM", "Нам следует рассмотреть другое решение.", "We should consider another solution"),
            choice("om9", "MEDIUM", "By next month, they ___ the project.", "will have completed", "complete", "had completing"),
            choice("om10", "MEDIUM", "Despite ___ tired, she continued working.", "being", "was", "be")
        ),
        "HARD" to listOf(
            choice("oh1", "HARD", "Hardly ___ the speech when the lights went out.", "had she begun", "she had begun", "did she began"),
            choice("oh2", "HARD", "The proposal requires that every member ___ present.", "be", "is", "will be"),
            choice("oh3", "HARD", "Were I to reconsider, I ___ the same conclusion.", "would reach", "will reach", "reached"),
            choice("oh4", "HARD", "His explanation was plausible, ___ incomplete.", "albeit", "unless", "whereas of"),
            choice("oh5", "HARD", "No sooner had we arrived ___ the storm began.", "than", "when", "then"),
            sentence("oh6", "HARD", "Обстоятельства редко складываются настолько идеально.", "Rarely do circumstances align so perfectly"),
            sentence("oh7", "HARD", "Доказательства не подтверждают это утверждение.", "The evidence does not substantiate the claim"),
            sentence("oh8", "HARD", "Если бы они знали, то возразили бы.", "Had they known they would have objected"),
            choice("oh9", "HARD", "The findings are consistent ___ earlier research.", "with", "to", "for"),
            choice("oh10", "HARD", "It is imperative that the issue ___ immediately.", "be addressed", "is addressed", "will address")
        )
    )
    private val names = listOf("MiaSpark", "AlexNova", "WordPilot", "LunaPhrase", "MaxVerbal", "IvySyntax")
}
