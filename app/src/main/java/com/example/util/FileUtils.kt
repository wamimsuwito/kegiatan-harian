package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPdfsDir(context: Context): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getPhotosDir(context)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun saveImageFromUri(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            // Scale down if larger than 1280px to save storage while keeping high quality
            val maxDimension = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio > 1f) {
                    targetW = maxDimension
                    targetH = (maxDimension / ratio).toInt()
                } else {
                    targetH = maxDimension
                    targetW = (maxDimension * ratio).toInt()
                }
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val targetFile = createTempImageFile(context)
            val outputStream = FileOutputStream(targetFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveSignatureBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.filesDir, "signature.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    fun getPdfOutputFile(context: Context, date: String, location: String): File {
        val cleanLoc = sanitizeFileName(location.take(30))
        val baseName = "Laporan_Kegiatan_${date}_${cleanLoc}"
        val dir = getPdfsDir(context)
        var file = File(dir, "$baseName.pdf")
        var counter = 1
        while (file.exists()) {
            file = File(dir, "${baseName}_$counter.pdf")
            counter++
        }
        return file
    }

    fun createBackupZip(context: Context, targetZipFile: File): Boolean {
        return try {
            val zipOut = ZipOutputStream(FileOutputStream(targetZipFile))

            // 1. Database file
            val dbFile = context.getDatabasePath("laporan_kegiatan_db")
            if (dbFile.exists()) {
                addFileToZip(dbFile, "db/laporan_kegiatan_db", zipOut)
            }
            val dbWal = File(dbFile.parentFile, "laporan_kegiatan_db-wal")
            if (dbWal.exists()) addFileToZip(dbWal, "db/laporan_kegiatan_db-wal", zipOut)
            val dbShm = File(dbFile.parentFile, "laporan_kegiatan_db-shm")
            if (dbShm.exists()) addFileToZip(dbShm, "db/laporan_kegiatan_db-shm", zipOut)

            // 2. Photos directory
            val photosDir = getPhotosDir(context)
            photosDir.listFiles()?.forEach { photo ->
                if (photo.isFile) {
                    addFileToZip(photo, "photos/${photo.name}", zipOut)
                }
            }

            // 3. Signature
            val sigFile = File(context.filesDir, "signature.png")
            if (sigFile.exists()) {
                addFileToZip(sigFile, "signature.png", zipOut)
            }

            zipOut.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, entryName: String, zipOut: ZipOutputStream) {
        val fis = FileInputStream(file)
        val zipEntry = ZipEntry(entryName)
        zipOut.putNextEntry(zipEntry)
        val buffer = ByteArray(4096)
        var length: Int
        while (fis.read(buffer).also { length = it } >= 0) {
            zipOut.write(buffer, 0, length)
        }
        fis.close()
        zipOut.closeEntry()
    }

    fun restoreBackupZip(context: Context, zipInputStream: InputStream): Boolean {
        return try {
            val zis = ZipInputStream(zipInputStream)
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(4096)

            while (entry != null) {
                val entryName = entry.name
                val targetFile: File = when {
                    entryName.startsWith("db/") -> {
                        val fileName = entryName.substringAfter("db/")
                        File(context.getDatabasePath("laporan_kegiatan_db").parentFile, fileName)
                    }
                    entryName.startsWith("photos/") -> {
                        val fileName = entryName.substringAfter("photos/")
                        File(getPhotosDir(context), fileName)
                    }
                    entryName == "signature.png" -> {
                        File(context.filesDir, "signature.png")
                    }
                    else -> null
                } ?: run {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    return@run null
                } ?: continue

                targetFile.parentFile?.mkdirs()
                val fos = FileOutputStream(targetFile)
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
