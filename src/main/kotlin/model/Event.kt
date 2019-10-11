package community.flock.eco.workday.model

import community.flock.eco.core.events.EventEntityListeners
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@EntityListeners(EventEntityListeners::class)
data class Event(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        val id: Long = 0,

        val name: String,
        val date: LocalDateTime = LocalDateTime.now(),

        @Enumerated(EnumType.STRING)
        val type: EventType
)