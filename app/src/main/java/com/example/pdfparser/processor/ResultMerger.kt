package com.example.pdfparser.processor

import java.io.File

class ResultMerger {

    /**
     * 合并两部分结果（分割时使用）
     */
    fun mergeResults(part1Dir: File, part2Dir: File, outputDir: File) {
        val imagesOut = File(outputDir, "images").apply { mkdirs() }

        // 复制 Part1 图片（不加前缀）
        copyImages(File(part1Dir, "images"), imagesOut, "")

        // 复制 Part2 图片（加 part2_ 前缀避免重名）
        copyImages(File(part2Dir, "images"), imagesOut, "part2_")

        // 合并 Markdown
        val md1 = File(part1Dir, "full.md").readText()
        val md2 = File(part2Dir, "full.md").readText()
        val merged = MarkdownUtils.mergeMarkdown(md1, md2, "part2_")

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
