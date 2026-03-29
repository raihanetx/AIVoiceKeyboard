package com.aikeyboard.feature.keyboard.data.source

/** All keyboard layout rows. Each inner list is one row of keys. */
object KeyboardLayouts {

    val en: List<List<String>> = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
    )

    val bn: List<List<String>> = listOf(
        listOf("ৌ","ৈ","া","ী","ূ","ব","হ","গ","দ","জ"),
        listOf("ো","ে","্","ি","ু","প","র","ক","ত","চ"),
        listOf("⇧","ং","ম","ন","স","ল","শ","⌫"),
    )

    val bnShift: List<List<String>> = listOf(
        listOf("ঔ","ঐ","আ","ঈ","ঊ","ভ","ঙ","ঘ","ধ","ঝ"),
        listOf("ও","এ","অ","ই","উ","ফ","ড়","খ","থ","ছ"),
        listOf("⇧","ঃ","ণ","ঞ","ষ","য়","ঢ","⌫"),
    )

    val num: List<List<String>> = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("@","#","£","%","&","-","+","(",")"),
        listOf("=","*","\"","'",":",";"," !","?","⌫"),
    )
}
