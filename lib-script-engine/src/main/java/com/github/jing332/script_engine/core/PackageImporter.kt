package com.github.jing332.script_engine.core

object PackageImporter {
    val default by lazy {
        appendImportPackage(
            "",
            listOf(
                "com.github.jing332.script_engine.core.type.ws",
                "com.github.jing332.script_engine.core.type.ui",
                "android.view",
                "android.widget",
            )
        )
    }

    private fun appendImportPackage(s: String, packages: List<String>): String {
        val strBuilder = StringBuilder(s)
        if (!strBuilder.endsWith(";"))
            strBuilder.append(";")

        return strBuilder.toString() + packages.joinToString(separator = ";") { "importPackage($it)" }
    }
}