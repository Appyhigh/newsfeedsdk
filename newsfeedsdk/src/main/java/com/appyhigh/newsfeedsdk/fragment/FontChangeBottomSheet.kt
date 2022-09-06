package com.appyhigh.newsfeedsdk.fragment

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class FontChangeBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(): FontChangeBottomSheet {
            val fontChangeBottomSheet = FontChangeBottomSheet()
            return fontChangeBottomSheet
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    val mFamilyNameSet = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_font, container, false)
        mFamilyNameSet.addAll(Arrays.asList(*resources.getStringArray(R.array.family_names)))

        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.family_names))
        val familyNameInput = view.findViewById<TextInputLayout>(R.id.auto_complete_family_name_input)
        val autoCompleteFamilyName = view.findViewById<AutoCompleteTextView>(R.id.auto_complete_family_name)
        autoCompleteFamilyName.setAdapter<ArrayAdapter<String>>(adapter)
        autoCompleteFamilyName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int,
                                           after: Int) {
                // No op
            }

            override fun onTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
                if (isValidFamilyName(charSequence.toString())) {
                    familyNameInput.isErrorEnabled = false
                    familyNameInput.error = ""
                } else {
                    familyNameInput.isErrorEnabled = true
                    familyNameInput.error = "Invalid Family Name"
                }
            }

            override fun afterTextChanged(editable: Editable) {
                // No op
            }
        })

        val mRequestDownloadButton = view.findViewById<Button>(R.id.button_request)
        mRequestDownloadButton.setOnClickListener(View.OnClickListener {
            val familyName = autoCompleteFamilyName.getText().toString()
            if (!isValidFamilyName(familyName)) {
                familyNameInput.isErrorEnabled = true
                familyNameInput.error = "Invalid Family Name"
                Constants.Toaster.show(requireContext(), "Invalid Family Name")
                return@OnClickListener
            }
            FeedSdk.applyFont(requireContext(), familyName)
            dismiss()
        })
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = requireActivity().getScreenHeight()
        }
    }

    private fun isValidFamilyName(familyName: String?): Boolean {
        return familyName != null && mFamilyNameSet.contains(familyName)
    }


    fun Activity.getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        } else{
            val outMetrics = windowManager.currentWindowMetrics
            val bounds = outMetrics.bounds
            bounds.height()
        }
    }
}