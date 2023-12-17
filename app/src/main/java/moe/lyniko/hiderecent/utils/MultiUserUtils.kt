package moe.lyniko.hiderecent.utils

import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

fun getIdByUserHandle(userHandle: UserHandle): Int {
    return HiddenApiBypass.invoke(UserHandle::class.java, userHandle, "getIdentifier"/*, args*/) as Int
}

fun getUserProfiles(context: Context): List<UserHandle> {
    // https://stackoverflow.com/questions/14749504/android-usermanager-check-if-user-is-owner-admin
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    return userManager.userProfiles
}
