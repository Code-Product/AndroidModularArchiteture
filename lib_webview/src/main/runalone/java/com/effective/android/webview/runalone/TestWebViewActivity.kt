package com.effective.android.webview.runalone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.widget.EditText
import android.widget.TextView
import com.effective.android.webview.R
import com.effective.android.webview.X5JsWebView
import com.effective.android.webview.X5Utils
import com.effective.android.webview.bean.Request
import com.effective.android.webview.bean.Result
import com.effective.android.webview.interfaces.BridgeHandler
import com.effective.android.webview.interfaces.CallBackFunction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.smtt.sdk.WebChromeClient
import java.lang.reflect.Type

class TestWebViewActivity : Activity() {

    private val TAG = "TestWebViewActivity"

    internal var RESULT_CODE = 0
    internal var mUploadMessage: ValueCallback<Uri>? = null

    private val JS_METHOD = "functionInJs"
    private val NATIVE_METHOD = "functionInNative"

    private fun getVersionCode(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionCode?.toString()
        } catch (e: Exception) {
            ""
        }
    }


    private fun setContent(content: String, nativeContent: TextView) {
        val currentContent = nativeContent.text.toString()
        nativeContent.text = currentContent + "\n" + content;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_test)
        val nativeContent = findViewById<View>(R.id.content) as TextView
        val userName = findViewById<View>(R.id.username) as EditText
        val password = findViewById<View>(R.id.password) as EditText
        val sendToWeb = findViewById<View>(R.id.send_to_web) as TextView
        val callWeb = findViewById<View>(R.id.call_web) as TextView
        val webView = findViewById<View>(R.id.webView) as X5JsWebView

        sendToWeb.setOnClickListener {
            val message = "Hello， I'm android!"
            val log = "android : $message"
            webView.send(message, object : CallBackFunction {
                override fun onCallBack(data: String?) {
                    if (!TextUtils.isEmpty(data)) {
                        val log = "web : $data"
                        Log.i(TAG, log)
                        setContent(log, nativeContent)
                    }
                }
            })
            Log.i(TAG, log)
            setContent(log, nativeContent)
        }

        webView.setDefaultHandler(object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                if (!TextUtils.isEmpty(data)) {
                    val log = "web : $data"
                    Log.i(TAG, log)
                    setContent(log, nativeContent)
                    val reponse = "Hi, I'm android!"
                    val reponseLog = "android : $reponse"
                    function.onCallBack(reponse)
                    Log.i(TAG, reponseLog)
                    setContent(reponseLog, nativeContent)
                }
            }
        })

        callWeb.setOnClickListener {
            val userName = userName.editableText.toString()
            val password = password.editableText.toString()
            val userBean = UserBean(userName, password, "android")
            val platform = "android"
            var version = getVersionCode(this)
            if (TextUtils.isEmpty(version)) {
                version = ""
            }
            val request = Request(platform, version!!, userBean)
            var requestData = X5Utils.object2Json(request)
            if(TextUtils.isEmpty(requestData)){
                requestData = ""
            }
            val log = "android request($JS_METHOD) : $requestData"
            Log.i(TAG, log)
            setContent(log, nativeContent)
            webView.callHandler(JS_METHOD, requestData, object : CallBackFunction {
                override fun onCallBack(data: String?) {
                    if (!TextUtils.isEmpty(data)) {
                        val log = "web reponse($JS_METHOD) : $data"
                        Log.i(TAG, log)
                        setContent(log, nativeContent)
                        var result = X5Utils.json2Obj<Result<String>>(data!!, object : TypeToken<Result<String>>() {}.type)
                    }
                }
            })
        }

        webView.registerHandler(NATIVE_METHOD, object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                if (!TextUtils.isEmpty(data)) {
                    var request = X5Utils.json2Obj<Request<UserBean>>(data!!, object : TypeToken<Request<UserBean>>() {}.type)
                    val requestLog = "web request($NATIVE_METHOD) : $data"
                    Log.i(TAG, requestLog)
                    setContent(requestLog!!, nativeContent)
                    val json = X5Utils.object2Json(Result(200, "success", ""))
                    function.onCallBack(json)
                    val reponseLog = "android reponse($NATIVE_METHOD) : $json"
                    Log.i(TAG, reponseLog)
                    setContent(reponseLog, nativeContent)
                }
            }
        })


        webView.setWebChromeClient(object : WebChromeClient() {

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, AcceptType: String, capture: String) {
                this.openFileChooser(uploadMsg)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, AcceptType: String) {
                this.openFileChooser(uploadMsg)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                mUploadMessage = uploadMsg
                pickFile()
            }
        })

        webView.loadUrl("file:///android_asset/demo.html")

    }

    fun pickFile() {
        val chooserIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooserIntent.type = "image/*"
        startActivityForResult(chooserIntent, RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage) {
                return
            }
            val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
            mUploadMessage!!.onReceiveValue(result)
            mUploadMessage = null
        }
    }
}
