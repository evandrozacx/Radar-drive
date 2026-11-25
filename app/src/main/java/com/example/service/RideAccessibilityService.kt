package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.RadarDriverApp
import com.example.domain.RideOfferParser

class RideAccessibilityService : AccessibilityService() {

    private var lastScanTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val controller = (application as? RadarDriverApp)?.runtimeController ?: return
        controller.setAccessibilityConnected(true)
        controller.overlayManager.attachAccessibilityService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val controller = (application as? RadarDriverApp)?.runtimeController ?: return
        controller.setAccessibilityConnected(true)
        controller.overlayManager.attachAccessibilityService(this)

        val now = System.currentTimeMillis()
        if (now - lastScanTimestamp < 220L) return
        lastScanTimestamp = now

        val eventPkg = event?.packageName?.toString()

        // Coleta janelas ativas da Uber e 99 (cobre cards em dialog, bottom-sheet ou overlay sobre outro app)
        val candidateRoots = mutableListOf<Pair<String, AccessibilityNodeInfo>>()

        runCatching { windows }.getOrNull()?.forEach { window ->
            val root = window.root ?: return@forEach
            val rootPkg = root.packageName?.toString()
            if (RideOfferParser.isSupportedPackage(rootPkg)) {
                candidateRoots.add(rootPkg!! to root)
            }
        }

        val activeRoot = rootInActiveWindow
        val activePkg = activeRoot?.packageName?.toString() ?: eventPkg
        if (activeRoot != null && RideOfferParser.isSupportedPackage(activePkg)) {
            if (candidateRoots.none { it.second == activeRoot }) {
                candidateRoots.add(activePkg!! to activeRoot)
            }
        }

        val sourceNode = event?.source
        val sourcePkg = sourceNode?.packageName?.toString() ?: eventPkg
        if (sourceNode != null && RideOfferParser.isSupportedPackage(sourcePkg)) {
            candidateRoots.add(sourcePkg!! to sourceNode)
        }

        if (candidateRoots.isEmpty()) return

        var lastScannedPkg: String? = null
        var lastSampleLines: List<String> = emptyList()

        for ((pkg, rootNode) in candidateRoots) {
            val extracted = LinkedHashSet<String>()
            collectTextNodes(rootNode, extracted)
            val lines = extracted.toList()
            lastScannedPkg = pkg
            if (lines.isNotEmpty()) {
                lastSampleLines = lines.take(8)
            }

            val offer = RideOfferParser.parse(pkg, lines)
            if (offer != null) {
                controller.updateDiagnostics(
                    packageName = pkg,
                    statusMessage = "Chamada ${offer.platform.label} detectada: R$ ${
                        String.format(java.util.Locale("pt", "BR"), "%.2f", offer.fare)
                    } • ${offer.totalKm} km • ${offer.totalMinutes} min",
                    sampleLines = lines.take(8)
                )
                controller.presentRideOffer(offer)
                return
            }
        }

        if (lastScannedPkg != null) {
            controller.updateDiagnostics(
                packageName = lastScannedPkg,
                statusMessage = "Tela lida (${lastSampleLines.size} textos), aguardando cartão de corrida",
                sampleLines = lastSampleLines
            )
        }
    }

    override fun onInterrupt() {
        val controller = (application as? RadarDriverApp)?.runtimeController ?: return
        controller.setAccessibilityConnected(false)
        controller.overlayManager.attachAccessibilityService(null)
    }

    override fun onDestroy() {
        val controller = (application as? RadarDriverApp)?.runtimeController
        controller?.setAccessibilityConnected(false)
        controller?.overlayManager?.attachAccessibilityService(null)
        super.onDestroy()
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo?, out: LinkedHashSet<String>) {
        if (node == null || out.size >= 400) return
        val text = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val desc = node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        if (text != null) {
            out.add(text)
        }
        if (desc != null && desc != text) {
            out.add(desc)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextNodes(child, out)
        }
    }
}
