package com.test.ballshader

import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.util.AttributeSet
import java.nio.IntBuffer
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.widget.Toast
import android.view.Surface
import java.util.*



class DrawGLSurfaceView : GLSurfaceView ,GLSurfaceView.Renderer {
    private val TAG = "DrawGLSurfaceView"
    private var mContext:Context? = null
    private var mProgram: Int = 0

    private var viewWidth:Int = 0
    private var viewHeight:Int = 0
    private var mGlAPos: Int = 0
    private var mGLATexCoor: Int = 0
    private var mGlUTex: Int = 0
    private var mGlUCameraMatrix: Int = 0

    private var mSurfaceTextureId:Int = 0
    private var vertexBuffer:FloatBuffer? = null
    private var texCoordBuffer:FloatBuffer? = null
    private var mAspectRatio: Float = 0f


    private val vert_shader = "attribute vec4 position;\n" +
            "attribute vec4 textureCoord;\n" +
            "varying vec4 textureCoordinate;\n" +
            "\n" +
            "uniform mat4 cameraTransform;\n" +
            "\n" +
            "void main() {\n" +
            "    textureCoordinate =  cameraTransform * textureCoord;\n" +
            "    gl_Position = position;\n" +
            "}\n" +
            "\n"

    private val fragment_shader = "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "varying highp vec4 textureCoordinate;\n" +
            "\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "uniform highp float u_radius;\n" +
            "uniform highp float u_refractiveIndex;\n" +
            "uniform highp float u_aspectRatio;\n" +
            "uniform highp vec2 u_center;\n" +
            "\n" +
            "void main(){\n" +
            "    highp vec2 textureCoordinateToUse = textureCoordinate.xy;\n" +
            "    textureCoordinateToUse.y = textureCoordinateToUse.y * 2.0 - 1.0;\n " +
            "    textureCoordinateToUse.y = textureCoordinateToUse.y * u_aspectRatio;\n"+
            "    textureCoordinateToUse.y = (textureCoordinateToUse.y + 1.0 ) * 0.5;\n" +
            "    highp float distanceFromCenter = distance(u_center, textureCoordinateToUse);\n" +
            "    highp float distance1 = distanceFromCenter / u_radius;\n" +
            "    highp float normalizedDepth = u_radius * sqrt(1.0 - distance1 * distance1);\n" +
            "    highp vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - u_center, normalizedDepth));" +
            "    highp vec3 refractedVector = refract(vec3(0.0, 0.0, -1.0), sphereNormal, u_refractiveIndex);" +
            "    if( distanceFromCenter > u_radius ) gl_FragColor = texture2D(inputImageTexture, textureCoordinate.xy);\n" +
            "    else { \n" +
            "    highp vec2 texCoord = vec2(-refractedVector.x, -refractedVector.y);\n" +
            "    texCoord = (texCoord + 1.0) * 0.5; \n " +
            "    gl_FragColor = texture2D(inputImageTexture, texCoord); }\n" +
            "}\n" +
            "\n"

    var squareCoords = floatArrayOf(-1.0f, 1.0f,0.0f,1.0f,
            -1.0f, -1.0f,0.0f,1.0f,
            1.0f, -1.0f,0.0f,1.0f,
            1.0f, 1.0f,0.0f,1.0f)

    var texureCoords = floatArrayOf(0.0f, 1.0f,0.0f, 1.0f,
            0.0f, 0.0f,0.0f, 1.0f,
            1.0f, 0.0f,0.0f, 1.0f,
            1.0f, 1.0f,0.0f, 1.0f)

    constructor(context: Context) : super(context) {
        mContext = context
        setEGLConfigChooser(8,8,8,8,8,0)
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context,attributeSet) {
    }

    override fun onDrawFrame(p0: GL10?) {
        draw()
    }

    private fun draw() {
        GLES20.glClearColor(1.0f,0.0f,0.0f,1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(mProgram)
        val bb = ByteBuffer.allocateDirect(squareCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer!!.put(squareCoords)
        vertexBuffer!!.position(0)

        var texbb = ByteBuffer.allocateDirect(texureCoords.size * 4)
        texbb.order(ByteOrder.nativeOrder())
        texCoordBuffer = texbb.asFloatBuffer()
        texCoordBuffer!!.put(texureCoords)
        texCoordBuffer!!.position(0)

        GLES20.glEnableVertexAttribArray(mGlAPos)
        GLES20.glEnableVertexAttribArray(mGLATexCoor)
        GLES20.glVertexAttribPointer(mGlAPos,4,GLES20.GL_FLOAT,false,0,vertexBuffer)
        GLES20.glVertexAttribPointer(mGLATexCoor, 4, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        mSurfaceTexture!!.updateTexImage()

        var floatArray = FloatArray(16)
        mSurfaceTexture!!.getTransformMatrix(floatArray)


        GLES20.glUniformMatrix4fv(mGlUCameraMatrix,1,false,floatArray,0)
        var location = GLES20.glGetUniformLocation(mProgram, "u_aspectRatio")
        GLES20.glUniform1f(location, mAspectRatio)

        location = GLES20.glGetUniformLocation(mProgram, "u_radius")
        GLES20.glUniform1f(location, 0.2f)

        location = GLES20.glGetUniformLocation(mProgram, "u_refractiveIndex")
        GLES20.glUniform1f(location, 0.7f)

        location = GLES20.glGetUniformLocation(mProgram, "u_center")
        GLES20.glUniform2f(location, 0.5f, 0.5f)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,mSurfaceTextureId)
        GLES20.glUniform1i(mGlUTex,0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN,0,4)

        GLES20.glDisableVertexAttribArray(mGlAPos)
        GLES20.glDisableVertexAttribArray(mGLATexCoor)
    }

    private fun genTextureId(width:Int ,height: Int) {
        var textureid:IntBuffer = IntBuffer.allocate(1)
        GLES20.glGenTextures(1,textureid)
        mSurfaceTextureId = textureid.get()

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES ,mSurfaceTextureId)

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glTexImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,0,GLES20.GL_RGBA,width,height,0,
                GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,0)
    }
    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        mAspectRatio = width.toFloat() / height.toFloat()
        android.util.Log.e(TAG, "sssssssssss " + mAspectRatio)
        GLES20.glViewport(0,0,width,height)
        genTextureId(width,height)
        mSurfaceTexture = SurfaceTexture(mSurfaceTextureId)
        mSurfaceTexture!!.setDefaultBufferSize(width,height)
        mSurface = Surface(mSurfaceTexture!!)
        mSurfaceTexture!!.setOnFrameAvailableListener(mFrameAvailableListener)

        initCameraAndPreview()
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        compileShader()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        super.surfaceDestroyed(holder)
        GLES20.glDeleteProgram(mProgram)
        mCameraDevice!!.close()
        mSurfaceTexture!!.release()
        var intArray = IntArray(1)
        intArray[0] = mSurfaceTextureId
        GLES20.glDeleteTextures(1,intArray,0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        val shader = GLES20.glCreateShader(type)

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val log = GLES20.glGetShaderInfoLog(shader)
        android.util.Log.d("DirectDraw", log)
        return shader
    }

    private fun compileShader() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vert_shader)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment_shader)

        mProgram = GLES20.glCreateProgram()             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(mProgram)                  // creates OpenGL ES program executables

        mGlAPos = GLES20.glGetAttribLocation(mProgram,"position")
        mGLATexCoor = GLES20.glGetAttribLocation(mProgram, "textureCoord")
        mGlUTex = GLES20.glGetUniformLocation(mProgram,"inputImageTexture")
        mGlUCameraMatrix = GLES20.glGetUniformLocation(mProgram,"cameraTransform")

        GLES20.glUseProgram(mProgram)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }
    private val mFrameAvailableListener = object : SurfaceTexture.OnFrameAvailableListener {
        override fun onFrameAvailable(p0: SurfaceTexture?) {
            requestRender()
        }

    }

    public fun permissionOpenCamera() {
        if (ActivityCompat.checkSelfPermission(mContext!!,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            mCameraManager!!.openCamera(mCameraId!!, deviceStateCallback, mHandle)
        }
    }
    private var mHandle:Handler? = null
    private var mCameraId = ""
    private var mCameraManager:CameraManager? = null
    private var mCameraDevice:CameraDevice? = null
    private fun initCameraAndPreview() {
        var handlerThread = HandlerThread("My Camera2");
        handlerThread.start()
        mHandle = Handler(handlerThread.looper)

        try {
            mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT
            mCameraManager = mContext!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (ActivityCompat.checkSelfPermission(mContext!!,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e(TAG,"permission denid")

                ActivityCompat.requestPermissions(mContext as Activity, arrayOf(android.Manifest.permission.CAMERA),12)

                return
            }
            mCameraManager!!.openCamera(mCameraId!!,deviceStateCallback,mHandle)

        } catch (e:CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            try {
                takePreview()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

        }

        override fun onDisconnected(camera: CameraDevice) {
            if (mCameraDevice != null) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Toast.makeText(mContext, "打开摄像头失败", Toast.LENGTH_SHORT).show()
        }
    }

    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mSurfaceTexture:SurfaceTexture? = null
    private var mSurface:Surface? = null
    private fun takePreview()  {
        android.util.Log.e(TAG,"takePreview")
        mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        mPreviewBuilder!!.addTarget(mSurface!!)
        mCameraDevice!!.createCaptureSession(Arrays.asList(mSurface),mSessionPreviewStateCallback,mHandle)
    }

    private val mSessionPreviewStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession?) {
            Toast.makeText(mContext, "配置失败", Toast.LENGTH_SHORT).show();
        }

        override fun onConfigured(session: CameraCaptureSession?) {
            try {
                android.util.Log.e(TAG,"onConfigured")

                mPreviewBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //打开闪光灯
                mPreviewBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                session!!.setRepeatingRequest(mPreviewBuilder!!.build(), null, mHandle);

            }catch ( e: CameraAccessException) {
                e.printStackTrace()
            }
        }

    }
}