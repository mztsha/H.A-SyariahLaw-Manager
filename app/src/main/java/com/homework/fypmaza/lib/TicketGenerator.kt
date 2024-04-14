package com.homework.fypmaza.lib

import java.util.Random

class TicketGenerator {
    companion object {
        fun generateTicketNumber(): String {
            return String.format("#%06d", Random().nextInt(1000000))
        }
    }
}