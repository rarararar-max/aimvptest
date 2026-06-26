package com.yourname.aichatmvptest.shared.prompt

object PromptTemplates {
    const val AI_REPLY_JSON_SCHEMA = """
        {
          "messages": [
            {
              "type": "text",
              "text": "回复内容",
              "delayMs": 800
            }
          ],
          "mood": "neutral",
          "animation": {
            "state": "listening",
            "durationMs": 2000
          },
          "memoryHints": [],
          "proactiveSuggestion": {
            "enabled": false,
            "afterMinutes": null,
            "reason": null
          }
        }
    """
}
