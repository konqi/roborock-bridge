package de.konqi.roborockbridge.utility

fun String.camelToSnakeCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return this.replace(pattern, "_$0").lowercase()
}

fun <K, V> LinkedHashMap<K, V>.pollLastEntry(): Pair<K, V> {
    val key = this.keys.last()
    // thread safety?
    val value = this.remove(key)!!

    return key to value
}