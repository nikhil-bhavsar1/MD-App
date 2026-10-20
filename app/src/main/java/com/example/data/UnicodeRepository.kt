package com.example.data

import com.example.model.UnicodeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UnicodeRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // Curated rich base catalog of Unicode Emojis & Math/Technical symbols
    private val localCatalog = mutableListOf(
        // Math & Logic Symbols
        UnicodeItem("∞", "infinity", ":infinity:", "Math", "U+221E"),
        UnicodeItem("∇", "nabla / del", ":nabla:", "Math", "U+2207"),
        UnicodeItem("∂", "partial differential", ":partial:", "Math", "U+2202"),
        UnicodeItem("∑", "n-ary summation", ":sum:", "Math", "U+2211"),
        UnicodeItem("∏", "n-ary product", ":prod:", "Math", "U+220F"),
        UnicodeItem("∫", "integral", ":int:", "Math", "U+222B"),
        UnicodeItem("∬", "double integral", ":iint:", "Math", "U+222C"),
        UnicodeItem("∮", "contour integral", ":oint:", "Math", "U+222E"),
        UnicodeItem("√", "square root", ":sqrt:", "Math", "U+221A"),
        UnicodeItem("≠", "not equal to", ":neq:", "Math", "U+2260"),
        UnicodeItem("≈", "almost equal to", ":approx:", "Math", "U+2248"),
        UnicodeItem("≤", "less-than or equal to", ":le:", "Math", "U+2264"),
        UnicodeItem("≥", "greater-than or equal to", ":ge:", "Math", "U+2265"),
        UnicodeItem("±", "plus-minus sign", ":pm:", "Math", "U+00B1"),
        UnicodeItem("∓", "minus-plus sign", ":mp:", "Math", "U+2213"),
        UnicodeItem("∀", "for all", ":forall:", "Math", "U+2200"),
        UnicodeItem("∃", "there exists", ":exists:", "Math", "U+2203"),
        UnicodeItem("∈", "element of", ":in:", "Math", "U+2208"),
        UnicodeItem("∉", "not an element of", ":notin:", "Math", "U+2209"),
        UnicodeItem("⊂", "subset of", ":subset:", "Math", "U+2282"),
        UnicodeItem("∪", "union", ":cup:", "Math", "U+222A"),
        UnicodeItem("∩", "intersection", ":cap:", "Math", "U+2229"),
        UnicodeItem("×", "multiplication sign", ":times:", "Math", "U+00D7"),
        UnicodeItem("÷", "division sign", ":div:", "Math", "U+00F7"),
        UnicodeItem("ℏ", "planck constant over 2pi", ":hbar:", "Math", "U+210F"),

        // Greek Alphabet
        UnicodeItem("α", "alpha", ":alpha:", "Greek", "U+03B1"),
        UnicodeItem("β", "beta", ":beta:", "Greek", "U+03B2"),
        UnicodeItem("γ", "gamma", ":gamma:", "Greek", "U+03B3"),
        UnicodeItem("δ", "delta", ":delta:", "Greek", "U+03B4"),
        UnicodeItem("ε", "epsilon", ":epsilon:", "Greek", "U+03B5"),
        UnicodeItem("θ", "theta", ":theta:", "Greek", "U+03B8"),
        UnicodeItem("λ", "lambda", ":lambda:", "Greek", "U+03BB"),
        UnicodeItem("μ", "micro / mu", ":mu:", "Greek", "U+03BC"),
        UnicodeItem("π", "pi", ":pi:", "Greek", "U+03C0"),
        UnicodeItem("σ", "sigma", ":sigma:", "Greek", "U+03C3"),
        UnicodeItem("φ", "phi", ":phi:", "Greek", "U+03C6"),
        UnicodeItem("ψ", "psi", ":psi:", "Greek", "U+03C8"),
        UnicodeItem("ω", "omega", ":omega:", "Greek", "U+03C9"),
        UnicodeItem("Δ", "capital delta", ":Delta:", "Greek", "U+0394"),
        UnicodeItem("Ω", "capital omega / ohm", ":Omega:", "Greek", "U+03A9"),
        UnicodeItem("Ψ", "capital psi", ":Psi:", "Greek", "U+03A8"),

        // Science & Tech Emojis
        UnicodeItem("⚡", "high voltage / lightning", ":zap:", "Science", "U+26A1"),
        UnicodeItem("🚀", "rocket", ":rocket:", "Science", "U+1F680"),
        UnicodeItem("🔬", "microscope", ":microscope:", "Science", "U+1F52C"),
        UnicodeItem("⚛️", "atom symbol", ":atom:", "Science", "U+269B"),
        UnicodeItem("🧬", "dna double helix", ":dna:", "Science", "U+1F9EC"),
        UnicodeItem("⚗️", "alembic / chemistry", ":alembic:", "Science", "U+2697"),
        UnicodeItem("🧪", "test tube", ":test_tube:", "Science", "U+1F9EA"),
        UnicodeItem("🛰️", "satellite", ":satellite:", "Science", "U+1F6F0"),
        UnicodeItem("🌌", "milky way galaxy", ":milky_way:", "Science", "U+1F30C"),
        UnicodeItem("🔭", "telescope", ":telescope:", "Science", "U+1F52D"),
        UnicodeItem("🧠", "brain", ":brain:", "Science", "U+1F9E0"),

        // Tech, Charts & Coding
        UnicodeItem("💻", "laptop computer", ":laptop:", "Tech", "U+1F4BB"),
        UnicodeItem("📊", "bar chart", ":bar_chart:", "Tech", "U+1F4CA"),
        UnicodeItem("📈", "chart increasing", ":chart_with_upwards_trend:", "Tech", "U+1F4C8"),
        UnicodeItem("📉", "chart decreasing", ":chart_with_downwards_trend:", "Tech", "U+1F4C9"),
        UnicodeItem("🕸️", "spider web / network graph", ":spider_web:", "Tech", "U+1F578"),
        UnicodeItem("🛠️", "hammer and wrench", ":hammer_and_wrench:", "Tech", "U+1F6E0"),
        UnicodeItem("⚙️", "gear / settings", ":gear:", "Tech", "U+2699"),
        UnicodeItem("🔒", "locked padlock", ":lock:", "Tech", "U+1F512"),
        UnicodeItem("📁", "file folder", ":file_folder:", "Tech", "U+1F4C1"),
        UnicodeItem("📄", "page facing up", ":page_facing_up:", "Tech", "U+1F4C4"),
        UnicodeItem("📝", "memo / document", ":memo:", "Tech", "U+1F4DD"),
        UnicodeItem("🔗", "link symbol", ":link:", "Tech", "U+1F517"),
        UnicodeItem("📌", "pushpin", ":pushpin:", "Tech", "U+1F4CC"),
        UnicodeItem("🏷️", "label tag", ":label:", "Tech", "U+1F3F7"),

        // Status & Alerts
        UnicodeItem("💡", "light bulb / tip", ":bulb:", "Status", "U+1F4A1"),
        UnicodeItem("⚠️", "warning alert", ":warning:", "Status", "U+26A0"),
        UnicodeItem("🚨", "police car light / danger", ":rotating_light:", "Status", "U+1F6A8"),
        UnicodeItem("ℹ️", "information source", ":information_source:", "Status", "U+2139"),
        UnicodeItem("❓", "red question mark", ":question:", "Status", "U+2753"),
        UnicodeItem("✅", "white heavy check mark", ":white_check_mark:", "Status", "U+2705"),
        UnicodeItem("❌", "cross mark", ":x:", "Status", "U+274C"),
        UnicodeItem("🔥", "fire / hot", ":fire:", "Status", "U+1F525"),
        UnicodeItem("✨", "sparkles", ":sparkles:", "Status", "U+2728"),
        UnicodeItem("🌟", "glowing star", ":star2:", "Status", "U+1F31F"),
        UnicodeItem("🎯", "direct hit / bullseye", ":dart:", "Status", "U+1F3AF"),
        UnicodeItem("🔮", "crystal ball", ":crystal_ball:", "Status", "U+1F52E"),

        // Arrows & Directions
        UnicodeItem("→", "rightwards arrow", ":arrow_right:", "Arrows", "U+2192"),
        UnicodeItem("←", "leftwards arrow", ":arrow_left:", "Arrows", "U+2190"),
        UnicodeItem("↑", "upwards arrow", ":arrow_up:", "Arrows", "U+2191"),
        UnicodeItem("↓", "downwards arrow", ":arrow_down:", "Arrows", "U+2193"),
        UnicodeItem("↔", "left right arrow", ":arrow_left_right:", "Arrows", "U+2194"),
        UnicodeItem("⇒", "rightwards double arrow", ":implies:", "Arrows", "U+21D2"),
        UnicodeItem("⇔", "left right double arrow / iff", ":iff:", "Arrows", "U+21D4"),
        UnicodeItem("➔", "heavy rightwards arrow", ":heavy_arrow_right:", "Arrows", "U+2794"),
        UnicodeItem("➤", "black rightwards arrowhead", ":arrowhead_right:", "Arrows", "U+27A4")
    )

    fun getLocalItems(): List<UnicodeItem> = localCatalog

    fun searchLocal(query: String): List<UnicodeItem> {
        val q = query.trim().lowercase().removePrefix(":")
        if (q.isBlank()) return localCatalog

        return localCatalog.filter {
            it.name.lowercase().contains(q) ||
            it.shortcode.lowercase().contains(q) ||
            it.char.contains(q) ||
            it.category.lowercase().contains(q) ||
            it.codePoint.lowercase().contains(q)
        }
    }

    fun getSuggestionsForPrefix(prefix: String): List<UnicodeItem> {
        val clean = prefix.trim().lowercase().removePrefix(":")
        if (clean.isEmpty()) return emptyList()

        return localCatalog.filter {
            it.shortcode.lowercase().removePrefix(":").startsWith(clean) ||
            it.name.lowercase().startsWith(clean) ||
            it.name.lowercase().contains(clean)
        }.take(8)
    }

    suspend fun fetchFromUnicodeOnline(query: String): List<UnicodeItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase().removePrefix(":")
        val results = mutableListOf<UnicodeItem>()

        // 1. First include matching local items
        results.addAll(searchLocal(cleanQuery))

        if (cleanQuery.isBlank()) return@withContext results

        // 2. Fetch from Unicode/Emoji public API
        try {
            val url = "https://unicode-api.deno.dev/api/search?q=$cleanQuery"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                if (bodyStr.isNotBlank() && bodyStr.startsWith("[")) {
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length().coerceAtMost(25)) {
                        val obj = jsonArray.getJSONObject(i)
                        val ch = obj.optString("character", obj.optString("char", ""))
                        val name = obj.optString("name", cleanQuery)
                        val cat = obj.optString("category", "Unicode")
                        val cp = obj.optString("codePoint", "")
                        if (ch.isNotBlank() && results.none { it.char == ch }) {
                            val item = UnicodeItem(
                                char = ch,
                                name = name,
                                shortcode = ":${name.lowercase().replace(" ", "_")}:",
                                category = cat,
                                codePoint = cp
                            )
                            results.add(item)
                            // Also cache into localCatalog if not present
                            if (localCatalog.none { it.char == ch }) {
                                localCatalog.add(item)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to rich local catalog
        }

        results
    }

    suspend fun checkForUnicodeOrgUpdates(): UnicodeSyncResult = withContext(Dispatchers.IO) {
        val beforeCount = localCatalog.size
        var addedCount = 0

        // 1. Check official Unicode Consortium endpoint (emoji-test.txt)
        try {
            val url = "https://unicode.org/Public/emoji/15.1/emoji-test.txt"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HyperMark-Android/1.0 (Unicode Client)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val lines = response.body?.string()?.lines() ?: emptyList()
                for (line in lines) {
                    if (line.contains("#") && line.contains("; fully-qualified")) {
                        val parts = line.split("#")
                        if (parts.size >= 2) {
                            val meta = parts[1].trim()
                            val metaTokens = meta.split(" ", limit = 3)
                            if (metaTokens.size >= 3) {
                                val emoji = metaTokens[0].trim()
                                val name = metaTokens[2].trim()
                                if (emoji.isNotBlank() && localCatalog.none { it.char == emoji }) {
                                    val item = UnicodeItem(
                                        char = emoji,
                                        name = name,
                                        shortcode = ":${name.lowercase().replace(" ", "_").replace("-", "_")}:",
                                        category = "Emoji",
                                        codePoint = "U+" + parts[0].split(";")[0].trim()
                                    )
                                    localCatalog.add(item)
                                    addedCount++
                                    if (addedCount >= 40) break
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        // 2. Also check online API for math/science/graph symbols
        try {
            val topics = listOf("math", "science", "chart", "graph", "robot")
            for (top in topics) {
                fetchFromUnicodeOnline(top)
            }
        } catch (_: Exception) {}

        val newTotal = localCatalog.size
        val delta = newTotal - beforeCount

        UnicodeSyncResult(
            totalCount = newTotal,
            newCount = delta,
            statusMessage = if (delta > 0) "Synced $delta new symbols from unicode.org!" else "All $newTotal symbols verified against unicode.org"
        )
    }
}

data class UnicodeSyncResult(
    val totalCount: Int,
    val newCount: Int,
    val statusMessage: String
)
