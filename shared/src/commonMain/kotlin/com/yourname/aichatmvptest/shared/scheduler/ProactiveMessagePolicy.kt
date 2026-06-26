package com.yourname.aichatmvptest.shared.scheduler

import com.yourname.aichatmvptest.shared.model.AiCharacter

data class ProactiveDecision(
    val shouldSend: Boolean,
    val reason: String,
)

class ProactiveMessagePolicy {
    fun evaluate(character: AiCharacter, unreadCount: Int, currentHour: Int): ProactiveDecision {
        if (currentHour in 0..7) return ProactiveDecision(false, "免打扰时间")
        if (unreadCount >= 3) return ProactiveDecision(false, "未读过多，降低打扰")
        return ProactiveDecision(character.proactiveLevel > 0, "角色允许主动消息")
    }
}
