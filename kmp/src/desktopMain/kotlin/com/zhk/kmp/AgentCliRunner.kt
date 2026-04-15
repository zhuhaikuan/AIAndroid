package com.zhk.kmp

import com.google.adk.agents.RunConfig
import com.google.adk.events.Event
import com.google.adk.runner.InMemoryRunner
import com.google.adk.sessions.Session
import com.google.genai.types.Content
import com.google.genai.types.Part
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.Consumer
import java.nio.charset.StandardCharsets
import java.util.Scanner


fun main(args: Array<String>) {
    val runConfig: RunConfig? = RunConfig.builder().build()
    val runner = InMemoryRunner(HelloTimeAgent.ROOT_AGENT)
    var i = 0

    val session: Session = runner
        .sessionService()
        .createSession(runner.appName(), "user1234")
        .blockingGet()

    Scanner(System.`in`, StandardCharsets.UTF_8)
        .use { scanner ->
            while (true) {
                print("\nYou > ")
                val userInput: String? = scanner.nextLine()
                if ("quit".equals(userInput, ignoreCase = true)) {
                    break
                }

                val userMsg: Content? =
                    Content.fromParts(
                        Part.fromText(userInput)
                    )
                val events: Flowable<Event> =
                    runner.runAsync(session.userId(), session.id(), userMsg, runConfig)

                print("\nAgent > ")
                events.blockingForEach(Consumer { event: Event ->
//                    if (event.finalResponse()) {
//                        println(event.stringifyContent())
//                    }
                    println("${i++}\n")
                    println("$event\n")
                    println(event.stringifyContent())
                })
            }
        }
}
