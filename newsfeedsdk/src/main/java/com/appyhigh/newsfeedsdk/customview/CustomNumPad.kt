package com.appyhigh.newsfeedsdk.customview

import android.content.Context
import android.text.method.NumberKeyListener
import android.util.AttributeSet
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.NumberKeyboardListener
import java.lang.Exception

class CustomNumPad : LinearLayout {

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    private var numberKeyboardListener:NumberKeyboardListener?=null
    private var valueString = ""
    private var currentEt:EditText?=null

    fun setListener(listener: NumberKeyboardListener){
        numberKeyboardListener = listener
    }

    fun setEt(editText: EditText){
        currentEt = editText
    }

    fun reset(){
        valueString = "0"
    }

    fun setValue(value:String){
        this.valueString = value
    }

    private fun initView(){
        val view = inflate(context, R.layout.layout_custom_numpad, this)
        view.findViewById<TextView>(R.id.key0).setOnClickListener {
            calculate("0")
        }
        view.findViewById<TextView>(R.id.key1).setOnClickListener {
            calculate("1")
        }
        view.findViewById<TextView>(R.id.key2).setOnClickListener {
            calculate("2")
        }
        view.findViewById<TextView>(R.id.key3).setOnClickListener {
            calculate("3")
        }
        view.findViewById<TextView>(R.id.key4).setOnClickListener {
            calculate("4")
        }
        view.findViewById<TextView>(R.id.key5).setOnClickListener {
            calculate("5")
        }
        view.findViewById<TextView>(R.id.key6).setOnClickListener {
            calculate("6")
        }
        view.findViewById<TextView>(R.id.key7).setOnClickListener {
            calculate("7")
        }
        view.findViewById<TextView>(R.id.key8).setOnClickListener {
            calculate("8")
        }
        view.findViewById<TextView>(R.id.key9).setOnClickListener {
            calculate("9")
        }
        val leftAux= view.findViewById<TextView>(R.id.leftAuxBtn)
        val rightAux= view.findViewById<AppCompatImageView>(R.id.rightAuxBtn)
        leftAux.setOnClickListener {
            if(valueString.isNotEmpty()){
                calculate(".")
            }
        }
        rightAux.setOnClickListener {
            if(valueString.isNotEmpty()){
                var cursorAt = valueString.length
                if(currentEt!=null){
                    val startPos = currentEt!!.selectionStart
                    val endPos = currentEt!!.selectionEnd
                    try{
                        if(startPos==endPos){
                            valueString = valueString.removeRange(startPos-1, endPos)
                            cursorAt = startPos-1
                        } else{
                            valueString = valueString.removeRange(startPos, endPos)
                            cursorAt = startPos
                        }
                        Log.d("CustomKeyPad", "initView: "+valueString)
                    } catch (ex:Exception){
                        ex.printStackTrace()
                    }
                } else{
                    valueString = valueString.dropLast(1)
                }
                if(valueString.isEmpty()){
                    valueString = "0"
                    numberKeyboardListener?.onResult(valueString, valueString.length)
                } else {
                    if(valueString.last()=='.'){
                        valueString = valueString.dropLast(1)
                        cursorAt -= 1
                    }
                    numberKeyboardListener?.onResult(valueString, cursorAt)
                }
            }
        }
    }

    private fun calculate(value: String){
        try{
            if(valueString=="0"){
                valueString=""
            }
            val endPos = currentEt!!.selectionEnd
            val newString = valueString.substring(0, endPos) + value + valueString.substring(endPos)
            valueString = newString
            numberKeyboardListener?.onResult(valueString, endPos+1)
//            numberKeyboardListener?.onResult(valueString, -1)
        } catch (ex:Exception){
            valueString += value
            numberKeyboardListener?.onResult(valueString, valueString.length)
        }
    }


}