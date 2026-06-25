package com.example.pdfparser.util

import java.io.File

object MarkdownUtils {

    /**
     * 修正图片路径为相对路径 ./images/xxx.png
     * 只修正解压后实际存在的图片引用
     */
    fun fixImagePaths(mdContent: String, imagesDir: File): String {
        if (!imagesDir.exists()) return mdContent

        val availableImages = imagesDir.listFiles()
            ?.map { it.name }
            ?.toSet() ?: return mdContent

        return mdContent.replace(
            Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
        ) { match ->
            val alt = match.groupValues[1]
            val originalPath = match.groupValues[2]
            val fileName = originalPath
                .substringAfterLast("/")
                .substringAfterLast("\\")

            if (fileName in availableImages) {
                "![${alt}](./images/${fileName})"
            } else {
                match.value
            }
        }
    }

    /**
     * 合并两个 Markdown 文件
     */
    fun mergeMarkdown(
        md1: String,
        md2: String,
        part2ImagePrefix: String = "part2_"
    ): String {
        val md2Fixed = md2.replace(
            Regex("""!\[([^\]]*)\]\(\.\/images\/([^)]+)\)""")
        ) { match ->
            val alt = match.groupValues[1]
            val imgName = match.groupValues[2]
            "![${alt}](./images/${part2ImagePrefix}${imgName})"
        }

        return "$md1\n\n---\n\n$md2Fixed"
    }

    /**
     * 合并两部分结果
     */
    fun mergeResults(part1Dir: File, part2Dir: File, outputDir: File) {
        val imagesOut = File(outputDir, "images").apply { mkdirs() }

        copyImages(File(part1Dir, "images"), imagesOut, "")
        copyImages(File(part2Dir, "images"), imagesOut, "part2_")

        val md1 = File(part1Dir, "full.md").readText()
        val md2 = File(part2Dir, "full.md").readText()
        val merged = mergeMarkdown(md1, md2, "part2_")

        File(outputDir, "full.md").writeText(merged)
    }

    private fun copyImages(source: File, target: File, prefix: String) {
        if (!source.exists()) return
        source.listFiles()?.forEach { file ->
            val newFile = File(target, "${prefix}${file.name}")
            file.copyTo(newFile, overwrite = true)
        }
    }
}
