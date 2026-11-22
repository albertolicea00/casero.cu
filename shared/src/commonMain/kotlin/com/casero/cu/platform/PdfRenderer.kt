package com.casero.cu.platform

/** Text attributes the exports use. Sizes are in PDF points. */
data class PdfTextStyle(val size: Float, val bold: Boolean = false, val secondary: Boolean = false)

/** Drawing surface of one PDF document. Coordinates start at the page's top-left corner. */
interface PdfCanvas {
    fun beginPage()

    /** Height of [text] wrapped to [width]. */
    fun measure(text: String, width: Float, style: PdfTextStyle): Float

    fun drawText(text: String, x: Float, y: Float, width: Float, style: PdfTextStyle)

    /** Draws an encoded image scaled to fit the box. Returns `false` when it cannot be decoded. */
    fun drawImage(data: ByteArray, x: Float, y: Float, maxWidth: Float, maxHeight: Float): Boolean
}

/** Renders a PDF with the platform's text layout (`PdfDocument` / `UIGraphicsPDFRenderer`). */
interface PdfRenderer {
    fun render(pageWidth: Float, pageHeight: Float, draw: PdfCanvas.() -> Unit): ByteArray
}
