package pl.ospzelazkow.glos.data

import android.content.Context
import android.net.Uri

object AliasImporter {
    private val EMAIL = Regex("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)

    suspend fun fromAssets(ctx: Context, repo: OspRepository, asset: String = "aliases.txt"): Int =
        ctx.assets.open(asset).bufferedReader().use { repo.import(parse(it.readLines())) }

    suspend fun fromUri(ctx: Context, repo: OspRepository, uri: Uri): Int =
        (ctx.contentResolver.openInputStream(uri) ?: return 0)
            .bufferedReader().use { repo.import(parse(it.readLines())) }

    private fun parse(lines: List<String>): List<AliasEntity> {
        val seen = HashSet<String>()
        return lines.map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.substringBefore(';').substringBefore(',').trim().lowercase() }
            .filter { EMAIL.matches(it) && seen.add(it) }
            .map { AliasEntity(email = it) }
    }
}