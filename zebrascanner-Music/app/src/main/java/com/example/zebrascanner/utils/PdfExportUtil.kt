package com.example.zebrascanner.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.zebrascanner.data.database.ShippedOrderEntity
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportUtil {
    
    fun exportOrdersToPdf(
        context: Context,
        orders: List<ShippedOrderEntity>,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Create Documents directory if it doesn't exist
            val documentsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "ZebraScanner"
            )
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ShippedOrders_$timestamp.pdf"
            val file = File(documentsDir, fileName)
            
            // Create PDF document
            val document = Document(PageSize.A4)
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()
            
            // Add title
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BaseColor.BLACK)
            val title = Paragraph("Shipped Orders Report\n\n", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            
            // Add generation date
            val dateFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.GRAY)
            val dateText = Paragraph("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n", dateFont)
            dateText.alignment = Element.ALIGN_CENTER
            document.add(dateText)
            
            // Add total count
            val countText = Paragraph("Total Orders: ${orders.size}\n\n", dateFont)
            countText.alignment = Element.ALIGN_CENTER
            document.add(countText)
            
            // Create table
            val table = PdfPTable(4) // 4 columns
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(2f, 2f, 3f, 2f))
            
            // Add table headers
            val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
            val headers = listOf("Order Number", "Tracking Number", "Message", "Shipped On")
            
            headers.forEach { headerText ->
                val cell = PdfPCell(Phrase(headerText, headerFont))
                cell.backgroundColor = BaseColor(52, 152, 219) // Blue color
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.padding = 8f
                table.addCell(cell)
            }
            
            // Add data rows
            val dataFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
            orders.forEach { order ->
                // Order Number
                val orderCell = PdfPCell(Phrase(order.orderNumber, dataFont))
                orderCell.padding = 6f
                table.addCell(orderCell)
                
                // Tracking Number
                val trackingCell = PdfPCell(Phrase(order.trackingNumber, dataFont))
                trackingCell.padding = 6f
                table.addCell(trackingCell)
                
                // Message
                val messageCell = PdfPCell(Phrase(order.message, dataFont))
                messageCell.padding = 6f
                table.addCell(messageCell)
                
                // Shipped On
                val shippedCell = PdfPCell(Phrase(formatDate(order.shippedOn), dataFont))
                shippedCell.padding = 6f
                table.addCell(shippedCell)
            }
            
            document.add(table)
            
            // Add footer
            document.add(Paragraph("\n"))
            val footerFont = Font(Font.FontFamily.HELVETICA, 8f, Font.ITALIC, BaseColor.GRAY)
            val footer = Paragraph("Zebra Scanner - Shipped Orders Export", footerFont)
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)
            
            document.close()
            
            onSuccess(file)
            
        } catch (e: Exception) {
            onError("Error creating PDF: ${e.localizedMessage}")
        }
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val parts = dateString.split("T")
            if (parts.size >= 2) {
                val datePart = parts[0]
                val timePart = parts[1].split(".")[0]
                "$datePart $timePart"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
