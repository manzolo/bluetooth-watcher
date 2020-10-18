package it.manzolo.bluetoothwatcher.utils

import android.os.Handler

class HandlerList(val classname: Class<*>, val handler: Handler, val runnable: Runnable, val frequency: Long)