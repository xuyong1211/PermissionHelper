package com.xy.permission.task

/**
 * 单个权限请求处理的包装类 使用端不需要关心此类
 */
class SinglePermissionWrapper(
    var childTaskSingle: SinglePermissionTask,
    var currentRequestCode: Int = 0
)
