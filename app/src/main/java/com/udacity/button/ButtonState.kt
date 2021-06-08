package com.udacity.button


sealed class ButtonState {
    object Clicked : ButtonState()
    data class Loading(val percent: Float) : ButtonState()
    object Completed : ButtonState()
}