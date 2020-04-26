package com.viewrouter.helper.plugin

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.Pass.UPDATE_ALL
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.viewrouter.helper.plugin.Constants.Companion
import java.awt.event.MouseEvent


class ViewRouterHelperLineMarker : LineMarkerProviderDescriptor() {

    override fun getName(): String? {
        return Companion.PLUGIN_NAME
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return when {
            isNavigationCall(element) -> {
                LineMarkerInfo<PsiElement>(element, element.textRange, IconLoader.getIcon(Companion.IMG_PATH), Pass.UPDATE_ALL, null, SEND_EVENT, GutterIconRenderer.Alignment.LEFT)
            }
            else -> {
                null
            }
        }
    }

    private fun isRouteClassCall(psiElement: PsiElement): Boolean {
        if (psiElement is PsiClass) {
            for (psiAnnotation in psiElement.annotations) {
                if (psiAnnotation.qualifiedName == Companion.ROUTE_ANNOTATION_NAME) {
                    findAttributeValue = psiAnnotation.findAttributeValue(Companion.ATTR_PATH)?.text.toString()
                    notifyNotFound(findAttributeValue)
                    return true
                }
            }
        }
        return false
    }

//    override fun navigate(e: MouseEvent?, psiElement: PsiElement?) {
//        if (psiElement is PsiMethodCallExpression) {
//            val psiExpressionList = (psiElement as PsiMethodCallExpressionImpl).argumentList
//            val targetPath = psiExpressionList.expressions[0].text.replace("\"", "")
//            val fullScope = GlobalSearchScope.allScope(psiElement.project)
//            val routeAnnotationWrapper = AnnotatedMembersSearch.search(getAnnotationWrapper(psiElement, fullScope)
//                    ?: return, fullScope).findAll()
//            val target = routeAnnotationWrapper.find {
//                it.modifierList?.annotations?.map { it.findAttributeValue(Companion.ATTR_PATH)?.text?.replace("\"", "") }?.contains(targetPath)
//                        ?: false
//            }
//            if (null != target) {
//                NavigationItem::class.java.cast(target).navigate(true)
//                return
//            }
//        }
//        notifyNotFound()
//    }

    private fun notifyNotFound() {
        Notifications.Bus.notify(Notification(Companion.NOTIFY_SERVICE_NAME, Companion.NOTIFY_TITLE, Companion.NOTIFY_NO_TARGET_TIPS, NotificationType.WARNING))
    }

    private fun notifyNotFound(content: String) {
        Notifications.Bus.notify(Notification(Companion.NOTIFY_SERVICE_NAME, Companion.NOTIFY_TITLE, content, NotificationType.WARNING))
    }

    private fun getAnnotationWrapper(psiElement: PsiElement?, scope: GlobalSearchScope): PsiClass? {
        if (null == routeAnnotationWrapper) {
            routeAnnotationWrapper = JavaPsiFacade.getInstance(psiElement?.project).findClass(Companion.ROUTE_ANNOTATION_NAME, scope)
        }

        return routeAnnotationWrapper
    }

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {}


    private fun isNavigationCall(psiElement: PsiElement): Boolean {
        if (psiElement is PsiAnnotation && psiElement.hasQualifiedName(Companion.BIND_ANNOTATION_NAME)) {
            return true
        }
        if (psiElement is PsiCallExpression) {
            val method = psiElement.resolveMethod() ?: return false
            val parent = method.parent
            var parameterName = method.parameterList.parameters[0].name
            if ((method.name.contains(Companion.FUN_START) || parameterName == Companion.ATTR_PATH) && parent is PsiClass) {
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


    private var SEND_EVENT: GutterIconNavigationHandler<PsiElement> = GutterIconNavigationHandler<PsiElement> { e: MouseEvent, psiElement: PsiElement ->
        if (psiElement is PsiMethodCallExpression) {
            val psiExpressionList = (psiElement as PsiMethodCallExpressionImpl).argumentList
            val targetPath = psiExpressionList.expressions[0].text.replace("\"", "")
            val fullScope = GlobalSearchScope.allScope(psiElement.project)
            val routeAnnotationWrapper = AnnotatedMembersSearch.search(getAnnotationWrapper(psiElement, fullScope)
                    ?: return@GutterIconNavigationHandler, fullScope).findAll()
            val target = routeAnnotationWrapper.find {
                it.modifierList?.annotations?.map { it.findAttributeValue(Companion.ATTR_PATH)?.text?.replace("\"", "") }?.contains(targetPath)
                        ?: false
            }
            if (null != target) {
                NavigationItem::class.java.cast(target).navigate(true)
                return@GutterIconNavigationHandler
            }
        }
        if (psiElement is PsiAnnotation && psiElement.hasQualifiedName(Companion.BIND_ANNOTATION_NAME)) {
            val annotationPath = psiElement.findAttributeValue(Companion.ATTR_PATH)
            if (annotationPath != null) {
                val text = annotationPath.text
                val fullScope = GlobalSearchScope.allScope(psiElement.project)
                val routeAnnotationWrapper = AnnotatedMembersSearch.search(getAnnotationWrapper(psiElement, fullScope)
                        ?: return@GutterIconNavigationHandler, fullScope).findAll()
                val target = routeAnnotationWrapper.find {
                    it.modifierList?.annotations?.map { it.findAttributeValue(Companion.ATTR_PATH)?.text?.replace("\"", "") }?. contains(text)
                            ?: false
                }
                if (null != target) {
                    NavigationItem::class.java.cast(target).navigate(true)
                    return@GutterIconNavigationHandler
                }
            }
        }

        notifyNotFound()
    }

    private var RECEIVER_EVENT: GutterIconNavigationHandler<PsiElement> = GutterIconNavigationHandler<PsiElement> { e: MouseEvent, psiElement: PsiElement ->
        if (psiElement is PsiClass) {
            val project: Project = psiElement.getProject()
            val javaPsiFacade = JavaPsiFacade.getInstance(project)
            val eventBusClass = javaPsiFacade.findClasses("ViewRouter", GlobalSearchScope.allScope(project))
            notifyNotFound("psiElement=" + psiElement.toString())
            for (eventBus in eventBusClass) {
                notifyNotFound("eventBus=" + eventBus.toString())
            }
//            val postMethod = eventBusClass!!.findMethodsByName(FUN_START, false)[0]


//            notifyNotFound(psiElement.toString())
//            val method = psiElement.resolveMethod()?: return@GutterIconNavigationHandler
//            if (method?.name.contains(Companion.FUN_START)) {
//                val text = psiElement.argumentList.expressions[0].text;
//                notifyNotFound(text+":"+findAttributeValue)
//                return@GutterIconNavigationHandler
//            }
        }
        notifyNotFound()
    }


    private fun safeEquals(obj: String?, value: String): Boolean {
        return obj != null && obj == value
    }

    private var findAttributeValue: String = ""
    private var routeAnnotationWrapper: PsiClass? = null
}