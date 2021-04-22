package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var torchStatus = false
    private var recognitionMode = "labels"
    private var savingMode = false

    private lateinit var settingsButton : ImageButton
    private lateinit var flashButton : ImageButton
    private lateinit var voiceButton : ImageButton
    private lateinit var cameraCaptureButton : ImageButton
    private lateinit var viewFinder : PreviewView
    var statusButton = false

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flashButton = camera_container.findViewById(R.id.flash_button)
        settingsButton = camera_container.findViewById(R.id.settings_button)
        cameraCaptureButton = camera_container.findViewById(R.id.camera_capture_button)
        voiceButton = camera_container.findViewById(R.id.voice_button)
        viewFinder = camera_container.findViewById(R.id.viewFinder)

        setupSharedPreferences()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        cameraCaptureButton.setOnClickListener {
            takePhoto() }

        flashButton.setOnClickListener { torchMode() }

        settingsButton.setOnClickListener {
            val startSettingsActivity = Intent(context, SettingsActivity::class.java)
            startActivity(startSettingsActivity)
        }

        outputDirectory = context?.let { getOutputDirectory(it) }!!

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(context,
                    "Permissions not granted bi the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun torchMode() {
        val camera = camera ?: return

        if (camera.cameraInfo.hasFlashUnit()) {
            if (!torchStatus) {
                if (camera.cameraInfo.torchState.value == TorchState.OFF) {
                    camera.cameraControl.enableTorch(true)
                } else
                    camera.cameraControl.enableTorch(false)
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        var resultImage : InputImage

        if (torchStatus)
            imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO
        else
            imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        if (savingMode) {
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        resultImage = InputImage.fromFilePath(context, Uri.fromFile(photoFile))
                        startAnalyzer(resultImage)
                        val msg = "Photo capture succeeded"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        statusButton = true
                    }
                })
        } else {
            imageCapture.takePicture(ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                @SuppressLint("UnsafeExperimentalUsageError")
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    resultImage = InputImage.fromMediaImage(imageProxy.image, imageProxy.imageInfo.rotationDegrees)
                    startAnalyzer(resultImage)
                    val msg = "Photo capture succeeded"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    statusButton = true
                    imageProxy.close()
                }
            })
        }
    }

    private fun startAnalyzer(resultImage : InputImage) {
        when (recognitionMode) {
            "labels" -> context?.let {
                ImageLabeler(
                    it,
                    "label"
                ).analyze(resultImage)
            }
            "faces" -> context?.let {
                FaceRecognizer(
                    it,
                    "face"
                ).analyze(resultImage)
            }
            "texts" -> context?.let {
                TextRecognizer(
                    it,
                    "text"
                ).analyze(resultImage)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture  = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))


        imageCapture = if (torchStatus) {
            ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_AUTO).build()
        } else
            ImageCapture.Builder().build()

        val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scale = camera?.cameraInfo?.zoomState?.value!!.zoomRatio * detector.scaleFactor
                    camera?.cameraControl?.setZoomRatio(scale)
                    return true
                }
            })

        viewFinder.afterMeasured {
            viewFinder.setOnTouchListener { _, event ->
                return@setOnTouchListener when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                            viewFinder.width.toFloat(),  viewFinder.height.toFloat()
                        )
                        val autoFocusPoint = factory.createPoint(event.x, event.y)
                        try {
                            camera?.cameraControl?.startFocusAndMetering(
                                FocusMeteringAction.Builder(
                                    autoFocusPoint,
                                    FocusMeteringAction.FLAG_AF
                                ).apply {
                                    disableAutoCancel()
                                }.build()
                            )
                        } catch (e: CameraInfoUnavailableException) {
                            Log.d("ERROR", "cannot access camera", e)
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        scaleGestureDetector.onTouchEvent(event)
                    }
                    else -> false
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        context?.let { it1 ->
            ContextCompat.checkSelfPermission(
                it1, it)
        } == PackageManager.PERMISSION_GRANTED
    }

    private fun setupSharedPreferences() {
        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(context)
        val ifEnabled = sharedPreference.getBoolean(getString(R.string.pref_key_torch), false)
        if (ifEnabled) {
            flashButton.visibility = View.INVISIBLE
            torchStatus = true
        } else {
            torchStatus = false
            flashButton.visibility = View.VISIBLE
        }
        val mode = sharedPreference.getString("default_mode", getString(R.string.label_image))
        if (mode != null) {
            when (mode) {
                getString(R.string.label_image) -> recognitionMode = "labels"
                getString(R.string.faces_recognize) -> recognitionMode = "faces"
                getString(R.string.text_recognize) -> recognitionMode = "texts"
            }
        }
        savingMode = sharedPreference.getBoolean(getString(R.string.pref_key_saving), false)
        sharedPreference.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        private const val TAG = "CameraCBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    override fun onResume() {
        super.onResume()
        setupSharedPreferences()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences != null) {
            if (key.equals(getString(R.string.pref_key_torch))) {
                    if (sharedPreferences.getBoolean(key, false)) {
                        torchStatus = true
                        flashButton.visibility = View.INVISIBLE
                    }
                    else {
                        torchStatus = false
                        flashButton.visibility = View.VISIBLE
                    }
            }
            else if (key.equals(getString(R.string.pref_key_mode))) {
                    val mode = sharedPreferences.getString(key, getString(R.string.label_image))
                    if (mode != null) {
                        when (mode) {
                            getString(R.string.label_image) -> recognitionMode = "labels"
                            getString(R.string.faces_recognize) -> recognitionMode = "faces"
                            getString(R.string.text_recognize) -> recognitionMode = "texts"
                        }
                    }
            }
            else if (key.equals(getString(R.string.pref_key_saving)))
                    savingMode = sharedPreferences.getBoolean(key,false)
        }
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }
}