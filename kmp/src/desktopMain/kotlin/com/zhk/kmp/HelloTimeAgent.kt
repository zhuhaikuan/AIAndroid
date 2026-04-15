package com.zhk.kmp

import com.google.adk.agents.BaseAgent
import com.google.adk.agents.LlmAgent
import com.google.adk.tools.Annotations
import com.google.adk.tools.FunctionTool

object HelloTimeAgent {
    var ROOT_AGENT: BaseAgent? = initAgent()

    private fun initAgent(): BaseAgent? {
        return LlmAgent.builder()
            .name("hello-time-agent")
            .description("Tells the current time in a specified city")
            .instruction(
                """
                You are a helpful assistant that tells the current time in a city.
                Use the 'getCurrentTime' tool for this purpose.
                
                """.trimIndent()
            )
            .model("gemini-2.5-flash")
            .tools(FunctionTool.create(HelloTimeAgent::class.java, "getCurrentTime"))
            .build()
    }

    /** Mock tool implementation  */
    @Annotations.Schema(description = "Get the current time for a given city")
    @JvmStatic
    fun getCurrentTime(
        @Annotations.Schema(
            name = "city",
            description = "Name of the city to get the time for"
        ) city: String
    ): MutableMap<String, String> {
        return mutableMapOf(
            Pair("city", city),
            Pair("forecast", "The time is 10:30am.")
        )
    }
}