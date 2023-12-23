package de.konqi.roborockbridge.utility

fun String.camelToSnakeCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return this.replace(pattern, "_$0").lowercase()
}