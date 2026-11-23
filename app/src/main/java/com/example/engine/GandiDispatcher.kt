package com.example.engine

import com.example.model.EstateSector
import com.example.model.GandiMood
import com.example.model.GateStatus
import com.example.model.PingType

data class GandiBroadcast(
    val line: String,
    val mood: GandiMood
)

/**
 * Generates expressive, chatty, humorous, and situationally aware dispatches
 * from GANDI, Emerald Crest's hyper-talkative security AI dispatcher.
 */
object GandiDispatcher {

    private var lineCounter = 0

    fun thiefSpotted(sector: EstateSector): GandiBroadcast {
        lineCounter++
        val templates = listOf(
            "HOLD MY ESPRESSO! Somebody in suspicious sneakers just darted past ${sector.name} on ${sector.streetName}! Go, go, GO!",
            "Motion sensors going bananas on ${sector.streetName}! A sneaky intruder is prowling right by ${sector.name}—don't blink!",
            "Ooh, ooh! Security cam #4 just caught a hooded silhouette tiptoeing around ${sector.name} (${sector.streetName})! Corner 'em!",
            "ATTENTION ALL UNITS! Mystery jogger—definitely NOT a resident—spotted near ${sector.name} heading along ${sector.streetName}!",
            "Why do they ALWAYS pick my shift?! Intruder detected at ${sector.name}, ${sector.streetName}! Cut off their angles!"
        )
        val line = templates[lineCounter % templates.size]
        val mood = if (sector.kind.name == "GATE") GandiMood.PANICKED else GandiMood.ALERT
        return GandiBroadcast(line = line, mood = mood)
    }

    fun gateStatusChanged(gateName: String, newStatus: GateStatus): GandiBroadcast {
        lineCounter++
        return when (newStatus) {
            GateStatus.LOCKED -> GandiBroadcast(
                line = "CLANG! $gateName is SEALED tight! Good luck vaulting 12 feet of wrought iron, sneaky-boots!",
                mood = GandiMood.PROUD
            )
            GateStatus.ALERT -> GandiBroadcast(
                line = "WHOA! Perimeter tripwire tripped at $gateName! Somebody check the turnstiles before they slip out!",
                mood = GandiMood.PANICKED
            )
            GateStatus.OPEN -> GandiBroadcast(
                line = "Uh, team? $gateName is wide OPEN again! Please tell me that was a delivery van and not our intruder!",
                mood = GandiMood.SUSPICIOUS
            )
        }
    }

    fun apprehensionCelebration(
        guardName: String,
        sector: EstateSector,
        pointsEarned: Long,
        uniformId: String
    ): GandiBroadcast {
        lineCounter++
        val uniformNote = if (uniformId == "gandi_fanclub") {
            " AND looking fabulous in my signature amber jacket!"
        } else {
            ""
        }
        val lines = listOf(
            "BOOM! $guardName bagged the intruder on ${sector.streetName} right by ${sector.name}! +$pointsEarned pts$uniformNote!",
            "YESSS! Textbook intercept at ${sector.name} ($guardName +$pointsEarned pts)! That's going on the HOA newsletter$uniformNote!",
            "GOTCHA! $guardName slammed the door at ${sector.name} on ${sector.streetName}! +$pointsEarned Patrol Points—treat yourself in the Armory!"
        )
        return GandiBroadcast(
            line = lines[lineCounter % lines.size],
            mood = GandiMood.PROUD
        )
    }

    fun pingReact(senderName: String, pingType: PingType, sector: EstateSector): GandiBroadcast {
        return when (pingType) {
            PingType.SUSPECT -> GandiBroadcast(
                line = "Copy $senderName! Pinning suspect flare at ${sector.name} on ${sector.streetName}! Squad, converge!",
                mood = GandiMood.ALERT
            )
            PingType.BACKUP -> GandiBroadcast(
                line = "$senderName is calling for backup at ${sector.name}! Somebody hustle down ${sector.streetName} right now!",
                mood = GandiMood.PANICKED
            )
            PingType.BLOCK_GATE -> GandiBroadcast(
                line = "Smart call, $senderName! Everybody pinch the exits near ${sector.name} (${sector.streetName})!",
                mood = GandiMood.CHATTY
            )
            PingType.CLEAR -> GandiBroadcast(
                line = "Nice sweep by $senderName—${sector.name} looks spotless. Stay sharp though, it's TOO quiet on ${sector.streetName}...",
                mood = GandiMood.SUSPICIOUS
            )
        }
    }

    fun shiftWelcome(guardName: String, roomCode: String): GandiBroadcast = GandiBroadcast(
        line = "Welcome to Shift $roomCode, $guardName! Tap any street or block to patrol, drop pings, or hit PTT radio. Let's keep Emerald Crest spotless!",
        mood = GandiMood.CHATTY
    )
}
