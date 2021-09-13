package com.xy.permission.task


/**
 * 多个权限请求的请求包装类，使用端在构建单个权限请求是传入 具体权限 以及权限请求通过 和 拒绝的操作
 * 注意：这里的拒绝操作是多个权限请求里的单个请求拒绝的操作
 * PermissionHelper.checkPermissions 第三个参数permissionDeniedTodo传入的拒绝后操作是 对这一组多个被拒绝权限请求的统一处理
 * 闭包permissionDeniedTodo参数mulTaskList为这组多个权限请求里被拒绝权限的集合
 */
class MulPermissionTask(var permissionString: String = "",
                        var permissionDone: () -> Unit = fun() {},
                        var permissionDenied: () -> Unit = fun() {})