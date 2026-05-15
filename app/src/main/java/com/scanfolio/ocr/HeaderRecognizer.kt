package com.scanfolio.ocr

object HeaderRecognizer {

    fun matchHeaders(
        recognizedHeaders: List<String>,
        knownColumns: List<String>
    ): List<String> {
        return recognizedHeaders.map { header ->
            knownColumns.firstOrNull { known ->
                header.contains(known) || known.contains(header)
            } ?: header
        }
    }
}
