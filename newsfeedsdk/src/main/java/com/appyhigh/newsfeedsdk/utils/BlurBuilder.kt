package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import com.appyhigh.newsfeedsdk.encryption.LogDetail


object BlurBuilder {
    private const val BITMAP_SCALE = 0.4f
    private const val BLUR_RADIUS = 12.5f
    fun blur(v: View): Bitmap? {
        return blur(v.getContext(), getScreenshot(v))
    }

    fun blur(ctx: Context?, image: Bitmap): Bitmap? {
        var outputBitmap:Bitmap?=null
        try{
            val width = Math.round(image.width * BITMAP_SCALE)
            val height = Math.round(image.height * BITMAP_SCALE)
            val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
            outputBitmap = Bitmap.createBitmap(inputBitmap)
            val rs = RenderScript.create(ctx)
            val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            val tmpIn: Allocation = Allocation.createFromBitmap(rs, inputBitmap)
            val tmpOut: Allocation = Allocation.createFromBitmap(rs, outputBitmap)
            theIntrinsic.setRadius(BLUR_RADIUS)
            theIntrinsic.setInput(tmpIn)
            theIntrinsic.forEach(tmpOut)
            tmpOut.copyTo(outputBitmap)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        return outputBitmap
    }

    private fun getScreenshot(v: View): Bitmap {
        val b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.draw(c)
        return b
    }
}