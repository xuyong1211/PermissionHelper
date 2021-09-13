package com.xy.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.xy.permission.PermissionHelper
import com.xy.permission.task.MulPermissionTask
import com.xy.permission.task.SinglePermissionTask

class PermissionActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        findViewById<Button>(R.id.tv_single).setOnClickListener {
            //单个权限请求
            // checkPermission 有默认参数howNotice 为权限被拒绝过后再次请求对权限使用的解释操作，可传入自定义操作
            PermissionHelper.checkPermission(this, SinglePermissionTask(Manifest.permission.CAMERA,{
                Toast.makeText(this@PermissionActivity, "相机", Toast.LENGTH_SHORT).show()
            })) //  PermissionRequestChildTask 的成员 permissionDone 为自定义的权限通过后的操作 permissionDenied 为权限被拒绝的操作 都有默认实现
        }
        findViewById<Button>(R.id.tv_mul).setOnClickListener {
            //多个权限请求
            PermissionHelper.checkPermissions(this, mutableListOf(
                MulPermissionTask(Manifest.permission.WRITE_EXTERNAL_STORAGE,{ Toast.makeText(this@PermissionActivity, "文件", Toast.LENGTH_SHORT).show() }),
                MulPermissionTask(Manifest.permission.ACCESS_FINE_LOCATION,{ Toast.makeText(this@PermissionActivity, "定位", Toast.LENGTH_SHORT).show() })
            ))

            //  注释部分是添加了 自定义提示操作  和  自定义的权限拒绝操作  改方法有默认的参数
//            PermissionHelper.checkPermissions(this, mutableListOf(MulPermissionTaskInfo().apply {
//                this.permissionString = Manifest.permission.WRITE_EXTERNAL_STORAGE
//                this.permissionDone = {Toast.makeText(this@PermissionActivity,"文件", android.widget.Toast.LENGTH_SHORT).show()}
//                this.permissionDenied = {Toast.makeText(this@PermissionActivity,"文件拒绝", android.widget.Toast.LENGTH_SHORT).show()}
//            },MulPermissionTaskInfo().apply {
//                this.permissionString = Manifest.permission.CAMERA
//                this.permissionDone = {Toast.makeText(this@PermissionActivity,"相机", android.widget.Toast.LENGTH_SHORT).show()}
//                this.permissionDenied = {Toast.makeText(this@PermissionActivity,"相机拒绝", android.widget.Toast.LENGTH_SHORT).show()}
//            }),{activity, tasks ->
//                AlertDialog.Builder(this)
//                    .setTitle("自定义的提示")
//                    .setMessage("之前被拒绝了，现在提示你申请${PermissionHelper.getPermissionStringBuild(tasks.toMutableList())}权限")
//                    .setPositiveButton("确定"){_,_->
//                        PermissionHelper.directCheckPermissions(activity,tasks.toMutableList())
//                    }
//                    .create().show()
//            },{ activity, mulTaskList ->
//                Toast.makeText(activity,"${PermissionHelper.getPermissionStringBuild(mulTaskList)}等权限已被拒绝,请到设置中手动打开",Toast.LENGTH_SHORT).show()
//            })

        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.commonOnRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }


}