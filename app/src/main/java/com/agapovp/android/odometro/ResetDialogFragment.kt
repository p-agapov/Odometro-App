package com.agapovp.android.odometro

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment

class ResetDialogFragment : DialogFragment() {

    private lateinit var listener: ResetDialogListener

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as ResetDialogListener
        } catch (e: ClassCastException) {
            Log.e(LOG_TAG, "$context must implement ResetDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it).run {
                setMessage("A sure you, you want to reset distance?")
                setPositiveButton("Yes") { _, _ ->
                    listener.onDialogPositiveClick(this@ResetDialogFragment)
                }
                setNegativeButton("No") { _, _ ->
                    listener.onDialogNegativeClick(this@ResetDialogFragment)
                }
                create()
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    interface ResetDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    companion object {
        private val LOG_TAG = ResetDialogFragment::class.java.canonicalName
    }
}
