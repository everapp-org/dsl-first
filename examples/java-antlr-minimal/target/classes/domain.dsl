domain Agent {
    states { OFF, IDLE, WORKING }
    transitions {
        activate: OFF -> IDLE
        assignTask: IDLE -> WORKING
        completeTask: WORKING -> IDLE
    }
}
