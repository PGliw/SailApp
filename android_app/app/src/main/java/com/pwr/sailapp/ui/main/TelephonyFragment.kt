package com.pwr.sailapp.ui.main


import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.*
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.ncorti.slidetoact.SlideToActView

import com.pwr.sailapp.R
import com.pwr.sailapp.ui.generic.MainScopedFragment
import com.pwr.sailapp.ui.generic.ScopedFragment
import kotlinx.android.synthetic.main.fragment_telephony.*
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 */
class TelephonyFragment : MainScopedFragment() {

    companion object {
        const val READ_PHONE_STATE_PERMISSION_REQUEST = 71
        const val SEND_SMS_PERMISSION_REQUEST = 107
        const val TAG = "Telephony"
    }

    private var telephonyManager: TelephonyManager? = null
    private var smsManager: SmsManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_telephony, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        text_input_edit_text_cancel_phone.setText(mainViewModel.rentalToCancel?.centrePhoneNumber ?: "?")

        val phonePermission =
            requireContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
        if (phonePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                READ_PHONE_STATE_PERMISSION_REQUEST
            )
        }
        val smsPermission =
            requireActivity().checkSelfPermission(Manifest.permission.SEND_SMS)
        if (smsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_PERMISSION_REQUEST
            )
        }
        telephonyManager = requireActivity().getSystemService(
            Context.TELEPHONY_SERVICE
        ) as TelephonyManager

        smsManager = SmsManager.getDefault()
        slide_to_act.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {

                val smsPerm =
                    requireActivity().checkSelfPermission(Manifest.permission.SEND_SMS)
                if (smsPerm != PackageManager.PERMISSION_GRANTED) {
                    slide_to_act.resetSlider()
                    requestPermissions(
                        arrayOf(Manifest.permission.SEND_SMS),
                        SEND_SMS_PERMISSION_REQUEST
                    )
                } else {
                    val dest = text_input_edit_text_cancel_phone.text.toString()
                    val message = text_input_edit_text_cancel_message.text.toString()
                    val isValid = PhoneNumberUtils.isWellFormedSmsAddress(dest)

                    if (isValid) {

                        val uri = Uri.parse("smsto:$dest")
                        val sentIntent = PendingIntent.getActivity(
                            requireActivity(),
                            0,
                            Intent(Intent.ACTION_SENDTO, uri),
                            0
                        )
                        launch {
                            if(mainViewModel.rentalToCancel == null){
                                renderNoRentalSelectedMessage()
                                return@launch
                            }
                            else{
                                smsManager?.sendTextMessage(
                                    dest, null, message, sentIntent, null
                                )
                                mainViewModel.cancelRental(mainViewModel.rentalToCancel!!.ID)
                                findNavController().navigateUp()
                            }
                        }

                    } else {
                        slide_to_act.resetSlider()
                        renderIncorrectNumberMessage()
                    }
                }
            }
        }

        val phoneOverview = getPhoneOverview(telephonyManager)
        renderPhoneOverview(phoneOverview)

        val phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                val overview = getPhoneOverview(telephonyManager)
                renderPhoneOverview(overview)
            }
        }

        telephonyManager?.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }


    private fun getPhoneOverview(telephonyMgr: TelephonyManager?): String? {
        if (telephonyMgr == null) return null
        val phonePermission =
            requireContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
        if (phonePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                READ_PHONE_STATE_PERMISSION_REQUEST
            )
            return null
        }
        val callStateStr = when (telephonyMgr.callState) {
            TelephonyManager.CALL_STATE_IDLE -> "IDLE"
            TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
            TelephonyManager.CALL_STATE_RINGING -> "RINGING"
            else -> "?"
        }
        val cellLocation = telephonyMgr.cellLocation as GsmCellLocation
        val cellLocationStr = "${cellLocation.lac} ${cellLocation.cid}"
        val simOperatorStr = telephonyMgr.simOperator
        val simOperatorNameStr = telephonyMgr.simOperatorName
        return "$callStateStr\n$cellLocationStr\n$simOperatorStr\n$simOperatorNameStr"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_PHONE_STATE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val phoneOverview = getPhoneOverview(telephonyManager)
                    renderPhoneOverview(phoneOverview)
                } else renderNoTelephonyPermissionMessage()
            } else {
                Log.d(TAG, "onRequestPermissionsResult: grantResults[] is empty")
            }

        } else if (requestCode == SEND_SMS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    renderNoSmsPermissionMessage()
                }
            } else {
                Log.d(TAG, "onRequestPermissionsResult: grantResults[] is empty")
            }
        }
    }

    private fun renderPhoneOverview(overview: String?) {
        text_phone_details.text = overview ?: "?"
    }

    private fun renderIncorrectNumberMessage() {
        Toast.makeText(
            requireContext(),
            resources.getString(R.string.incorrect_number),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderNoTelephonyPermissionMessage() {
        Toast.makeText(
            requireContext(),
            resources.getString(R.string.no_telephony_permission),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderNoSmsPermissionMessage() {
        Toast.makeText(
            requireContext(),
            resources.getString(R.string.no_sms_permission),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderNoRentalSelectedMessage(){
        snack(resources.getString(R.string.no_rental_selected))
    }
}



