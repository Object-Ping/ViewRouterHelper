package com.viewrouter.helper.plugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.viewrouter.helper.plugin.Constants.Companion
import java.awt.event.MouseEvent


class ViewRouterHelperLineMarker : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiElement> {

    override fun getName(): String? {
        return Companion.PLUGIN_NAME
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return if (isNavigationCall(element)) {
            LineMarkerInfo<PsiElement>(element, element.textRange, IconLoader.getIcon(Companion.IMG_PATH), null, this, GutterIconRenderer.Alignment.LEFT)
        } else {
            null
        }
    }

    override fun navigate(e: MouseEvent?, psiElement: PsiElement?) {
        if (psiElement is PsiMethodCallExpression) {
            val psiExpressionList = (psiElement as PsiMethodCallExpressionImpl).argumentList
            val targetPath = psiExpressionList.expressions[0].text.replace("\"", "")
            val fullScope = GlobalSearchScope.allScope(psiElement.project)
            val routeAnnotationWrapper = AnnotatedMembersSearch.search(getAnnotationWrapper(psiElement, fullScope)
                    ?: return, fullScope).findAll()
            val target = routeAnnotationWrapper.find {
                it.modifierList?.annotations?.map { it.findAttributeValue(Companion.ATTR_PATH)?.text?.replace("\"", "") }?.contains(targetPath)
                        ?: false
            }
            if (null != target) {
                NavigationItem::class.java.cast(target).navigate(true)
                return
            }
        }
        notifyNotFound()
    }

    private fun notifyNotFound() {
        Notifications.Bus.notify(Notification(Companion.NOTIFY_SERVICE_NAME, Companion.NOTIFY_TITLE, Companion.NOTIFY_NO_TARGET_TIPS, NotificationType.WARNING))
    }


    private fun getAnnotationWrapper(psiElement: PsiElement?, scope: GlobalSearchScope): PsiClass? {
        if (null == routeAnnotationWrapper) {
            routeAnnotationWrapper = JavaPsiFacade.getInstance(psiElement?.project).findClass(Companion.ROUTE_ANNOTATION_NAME, scope)
        }

        return routeAnnotationWrapper
    }

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {}


    private fun isNavigationCall(psiElement: PsiElement): Boolean {
        if (psiElement is PsiCallExpression) {
            val method = psiElement.resolveMethod() ?: return false
            val parent = method.parent
            if (method.name.contains(Companion.FUN_START) && parent is PsiClass) {
                if (isClassOfARouter(parent)) {
                    return true
                }
            }
        }
        return false
    }


    private fun isClassOfARouter(psiClass: PsiClass): Boolean {

        if (psiClass.name.equals(Companion.SDK_NAME)) {
            return true
        }

        psiClass.supers.find { it.name == Companion.SDK_NAME } ?: return false

        return true
    }

    private fun safeEquals(obj: String?, value: String): Boolean {
        return obj != null && obj == value
    }

    private var routeAnnotationWrapper: PsiClass? = null
}