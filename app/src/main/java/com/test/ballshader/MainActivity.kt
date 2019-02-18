package com.test.ballshader

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    private var glSurfaceView: DrawGLSurfaceView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = DrawGLSurfaceView(this)
        this.setContentView(glSurfaceView)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 12) {
            if (permissions.get(0).equals(Manifest.permission.CAMERA)) {
                if (grantResults[0] ==  PackageManager.PERMISSION_GRANTED) {
                    glSurfaceView!!.permissionOpenCamera()
                } else {
                    this.finish()
                }
            }
        }
    }
}