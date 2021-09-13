package com.xy.permission.task

import android.app.Activity
import com.xy.permission.PermissionHelper

/**
 * 单个权限请求的请求包装类，使用端在构建单个权限请求是传入 具体权限 以及权限请求通过和拒绝的操作
 */
class SinglePermissionTask(var permissionString :String= "", var permissionDone: () ->Unit={
}, var permissionDenied:(activity: Activity, permission:String)->Unit = PermissionHelper.defSingleDeniedToDo )