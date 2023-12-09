package de.konqi.roborockbridge.utility

inline fun <reified T> checkType(value: Any) = value is T
inline fun <reified T> cast(value: Any): T = if(value is T) value else throw ClassCastException()