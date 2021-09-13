package com.xy.permission.task

import android.app.Activity
import com.xy.permission.PermissionHelper

/**
 * 多个权限处理的包装类 使用段不需要关心
 * permissionsDeniedToDo为这组多个权限请求里被拒绝权限的统一处理
 * 闭包permissionDeniedTodo参数mulTaskList为这组多个权限请求里被拒绝权限的集合
 */
class MulPermissionWrapper(
    var requestCode: Int = 0,
    var taskList: List<MulPermissionTask> = listOf(),
    var permissionsDeniedToDo: (activity: Activity, mulTaskList: MutableList<MulPermissionTask>) -> Unit = PermissionHelper.defPermissionDeniedToDo
)