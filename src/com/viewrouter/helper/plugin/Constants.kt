package com.viewrouter.helper.plugin

class Constants {
    companion object {
        const val FUN_START = "routeTo"
        const val ATTR_PATH = "path"
        const val SDK_NAME = "ViewRouter"
        const val PLUGIN_NAME = "ViewRouterHelper"
        const val IMG_PATH = "/icon/viewrouter_helper.png";
        const val ROUTE_ANNOTATION_NAME = "com.alibaba.android.arouter.facade.annotation.Route"
        const val HELP_ANNOTATION_NAME = "com.mapbar.android.baselibrary.baseview.viewmanager.ViewRouter"
        const val BIND_ANNOTATION_NAME = "com.mapbar.android.baselibrary.baseview.annotation.BindChildFragment"

        // Notify
        const val NOTIFY_SERVICE_NAME = "ViewRouterHelper Plugin Tips"
        const val NOTIFY_TITLE = "ViewRouterHelper"
        const val NOTIFY_NO_TARGET_TIPS = "No destination found or unsupported type."

    }
}