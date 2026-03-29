package com.aikeyboard.feature.keyboard.data.source

/** Static English word list and next-word prediction map. */
object Dictionary {

    val words: Set<String> = setOf(
        "the","quick","brown","fox","jumps","over","lazy","dog","hello","world",
        "apple","application","android","pixel","premium","good","morning","night",
        "how","are","you","what","where","when","why","who","awesome","amazing",
        "keyboard","i","am","have","will","best","way","only","is","a","to","it",
        "its","on","in","at","this","that","we","us","they","them","he","she",
        "his","her","and","or","but","so","because","yes","no","my","mine","car",
        "cat","can","boy","girl","time","person","year","day","thing","man","woman",
        "life","child","work","new","first","last","long","great","little","own",
        "other","old","right","big","high","different","small","large","next","early",
        "young","important","few","public","bad","same","able","do","say","go","get",
        "make","know","think","take","see","come","want","look","use","find","give",
        "tell","may","should","call","try","ask","need","feel","become","leave","put",
        "mean","keep","let","begin","seem","help","talk","turn","start","show","hear",
        "play","run","move","like","live","believe","hold","bring","happen","write",
        "provide","sit","stand","lose","pay","meet","include","continue","set","learn",
        "change","lead","understand","watch","follow","stop","create","speak","read",
        "allow","add","spend","grow","open","walk","win","offer","remember","love",
        "consider","appear","buy","wait","serve","die","send","expect","build","stay",
        "fall","cut","reach","kill","remain",
    )

    /** Context-aware next-word predictions keyed by the previous word. */
    val nextWordMap: Map<String, List<String>> = mapOf(
        "i"     to listOf("am", "have", "will"),
        "the"   to listOf("best", "way", "only"),
        "hello" to listOf("world", "there", "friend"),
        "how"   to listOf("are", "is", "to"),
        "good"  to listOf("morning", "night", "luck"),
        "you"   to listOf("are", "can", "will"),
        "we"    to listOf("are", "will", "have"),
        "they"  to listOf("are", "will", "have"),
        "what"  to listOf("is", "are", "do"),
    )

    /** Fallback suggestions shown when no context is available. */
    val generic = listOf("i", "the", "to")
}
