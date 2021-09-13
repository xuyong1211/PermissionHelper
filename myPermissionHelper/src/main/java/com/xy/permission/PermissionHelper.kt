package com.xy.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xy.permission.task.MulPermissionTask
import com.xy.permission.task.MulPermissionWrapper
import com.xy.permission.task.SinglePermissionTask
import com.xy.permission.task.SinglePermissionWrapper


object PermissionHelper {
    private val taskList = mutableListOf<SinglePermissionWrapper>()

    // 权限申请不是频繁的任务，不需要缓存
//    private val cacheTaskList = mutableListOf<PermissionRequestTask>()

    private val mulTaskList = mutableListOf<MulPermissionWrapper>()

    /**
     * 在howNotice闭包中，可以定义对话框等提示来解释权限申请原因，
     * 如果有需要在提示信息提供继续请求权限的，必须使用directRequestPermission，
     * 如果使用checkPermission会递归无限提示，如果使用系统权限申请，那么就放弃了PermissionHelper的自定义回调
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermission(
        activity: Activity,
        singlePermissionTask: SinglePermissionTask,
        howNotice: (activity: Activity, singlePermissionTask: SinglePermissionTask) -> Unit = defSingleNotice
    ) {
        val checkSelfPermission =
            ContextCompat.checkSelfPermission(activity, singlePermissionTask.permissionString)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            singlePermissionTask.permissionDone()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, singlePermissionTask.permissionString)) {
                howNotice(activity, singlePermissionTask)
            } else {
                directRequestPermission(activity, singlePermissionTask)
            }
        }
    }


    /**
     * 不关心shouldShowRequestPermissionRationale的返回值，直接请求权限
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun directRequestPermission(activity: Activity, singlePermissionTask: SinglePermissionTask) {
        val requestCode = getNewRequestCode()
        addToList(SinglePermissionWrapper(singlePermissionTask, requestCode))
        activity.requestPermissions(arrayOf(singlePermissionTask.permissionString), requestCode)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermissions(
        activity: Activity,
        tasks: MutableList<MulPermissionTask>,
        hoeNotice: (activity: Activity, tasks: List<MulPermissionTask>) -> Unit = defMulNotice,
        permissionDeniedTodo: (activity: Activity, mulTaskList: MutableList<MulPermissionTask>) -> Unit = defPermissionDeniedToDo
    ) {
        val needNoticeTask = mutableListOf<MulPermissionTask>()
        for (task in tasks) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    task.permissionString
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                task.permissionDone()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        task.permissionString
                    )
                ) {
                    needNoticeTask.add(task)
                }
            }
        }
        tasks.removeAll(needNoticeTask)
        if (tasks.isNotEmpty()) {
            directCheckPermissions(activity, tasks, permissionDeniedTodo)
        }
        if (needNoticeTask.isNotEmpty()) {
            hoeNotice(activity, needNoticeTask)
        }
    }

    /**
     * 不关心shouldShowRequestPermissionRationale的返回值，直接请求权限
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun directCheckPermissions(
        activity: Activity,
        tasks: MutableList<MulPermissionTask>,
        permissionDeniedTodo: (activity: Activity, mulTaskList: MutableList<MulPermissionTask>) -> Unit = defPermissionDeniedToDo
    ) {
        val requestCode = getNewRequestCode()
        mulTaskList.add(MulPermissionWrapper(requestCode, tasks, permissionDeniedTodo))
        val permissionArray = tasks.map {
            it.permissionString
        }.toTypedArray()
        if (permissionArray.isNotEmpty()) {
            activity.requestPermissions(permissionArray, requestCode)
        }
    }


    // 默认的单权限请求的说明
    val defSingleNotice = fun(activity: Activity, singlePermissionTask: SinglePermissionTask) {
        showNoticeDialog(activity, getPermissionString(singlePermissionTask.permissionString)) {
            directRequestPermission(activity, singlePermissionTask)
        }
    }
    val defSingleDeniedToDo = fun(activity: Activity, permission: String) {

        AlertDialog.Builder(activity).setTitle("提示")
            .setMessage("${getPermissionString(permission)}等权限已被拒绝,请到设置中手动打开")
            .setPositiveButton("好的") { _, _ ->
                goIntentSetting(activity)
            }
            .setNegativeButton("知道了") { _, _ -> }
            .create()
            .show()
    }

    /**
     * 在请求权限的activity的onRequestPermissionsResult中调用此方法
     */
    fun commonOnRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        // 单权限的回调处理
        val findTask = taskList.find { it.currentRequestCode == requestCode }
        if (findTask != null && grantResults.isNotEmpty()) {
            taskList.remove(findTask)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findTask.childTaskSingle.permissionDone()
            } else {
                findTask.childTaskSingle.permissionDenied(
                    activity,
                    findTask.childTaskSingle.permissionString
                )
            }
            return
        }

        // 多权限的回调处理
        val findMulTask = mulTaskList.find { it.requestCode == requestCode }
        if (findMulTask != null) {
            mulTaskList.remove(findMulTask)
            val deniedPermissions = mutableListOf<MulPermissionTask>()
            for (i in permissions.indices) {
                val task =
                    findMulTask.taskList.find { it.permissionString == permissions[i] } ?: continue
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    task.permissionDone()
                } else {
                    task.permissionDenied()
                    deniedPermissions.add(task)
                }
            }
            if (deniedPermissions.isNotEmpty()) {
                findMulTask.permissionsDeniedToDo(activity, deniedPermissions)
            }

        }

    }

    // 默认的多权限请求的说明
    val defMulNotice = fun(activity: Activity, tasks: List<MulPermissionTask>) {
        showNoticeDialog(activity, getPermissionStringBuild(tasks.toMutableList())) {
            directCheckPermissions(activity, tasks.toMutableList())
        }
    }
    val defPermissionDeniedToDo =
        fun(activity: Activity, mulTaskList: MutableList<MulPermissionTask>) {
            AlertDialog.Builder(activity).setTitle("提示")
                .setMessage("${getPermissionStringBuild(mulTaskList)}等权限已被拒绝,请到设置中手动打开")
                .setPositiveButton("好的") { _, _ ->
                    goIntentSetting(activity)
                }
                .setNegativeButton("知道了") { _, _ -> }
                .create()
                .show()
        }

    fun getPermissionStringBuild(mulTaskList: MutableList<MulPermissionTask>): String {
        val message = StringBuffer()
        for (mulPermissionTaskInfo in mulTaskList) {
            if (message.isNotEmpty()) {
                message.append(",")
            }
            message.append(getPermissionString(mulPermissionTaskInfo.permissionString))
        }
        return message.toString()
    }

    private fun goIntentSetting(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNoticeDialog(
        activity: Activity,
        message: String,
        requestPermission: () -> Unit
    ) {
        AlertDialog.Builder(activity).setTitle("提示")
            .setMessage("为了更好的使用体验，需要您同意开启${message}需要权限。")
            .setPositiveButton("申请") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermission()
                }
            }
            .create()
            .show()
    }

    /**
     * 获取权限的名称
     */
    fun getPermissionString(permission: String): String {
        var message = "相关权限"
        when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                message = "定位信息"
            }
            Manifest.permission.CAMERA -> {
                message = "拍照，摄像"
            }
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                message = "文件写入"
            }
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                message = "文件读取"
            }
            Manifest.permission.READ_PHONE_STATE -> {
                message = "设备信息"
            }
        }
        return message
    }

    /**
     * 获取新的requestCode
     */
    private fun getNewRequestCode(): Int {
        return maxOf(
            if (taskList.isEmpty()) {
                100
            } else {
                taskList.last().currentRequestCode
            }, if (mulTaskList.isEmpty()) {
                100
            } else {
                mulTaskList.last().requestCode
            }
        ) + 1
    }


    private fun addToList(taskSingle: SinglePermissionWrapper) {
        val contains = taskList.find { it.currentRequestCode == taskSingle.currentRequestCode }
        if (contains != null) {
            contains.childTaskSingle = taskSingle.childTaskSingle
        } else {
            taskList.add(
                SinglePermissionWrapper(
                    taskSingle.childTaskSingle,
                    taskSingle.currentRequestCode
                )
            )
        }
    }


}