package com.scanfolio.ui.scan

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.repository.StockRepository
import com.scanfolio.ocr.OcrEngine
import com.scanfolio.ocr.OcrRow
import com.scanfolio.ocr.TableAnalyzer
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val mockApp = mockk<ScanfolioApp>(relaxed = true)
    private val mockOcrEngine = mockk<OcrEngine>(relaxed = true)
    private val mockTableAnalyzer = mockk<TableAnalyzer>()
    private val mockStockRepo = mockk<StockRepository>(relaxed = true)

    private lateinit var viewModel: ScanViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockApp.ocrEngine } returns mockOcrEngine
        every { mockApp.tableAnalyzer } returns mockTableAnalyzer
        every { mockApp.stockRepository } returns mockStockRepo
        every { mockApp.getBaiduOcrApiKey() } returns null
        every { mockApp.getBaiduOcrSecretKey() } returns null

        viewModel = ScanViewModel(mockApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun mockBitmap(): Bitmap = mockk(relaxed = true)

    private fun setupPreviewData(rows: List<OcrRow>, headers: List<String>) {
        every { mockTableAnalyzer.analyze(any(), any(), any()) } returns TableAnalyzer.TableResult(headers, rows)
        runTest(testDispatcher) {
            viewModel.processImageInternal(mockBitmap())
        }
    }

    // --- processImageInternal ---

    @Test
    fun `processImageInternal clears ocrResult on start`() = runTest(testDispatcher) {
        every { mockTableAnalyzer.analyze(any(), any(), any()) } throws RuntimeException("stop here")

        viewModel.processImageInternal(mockBitmap())

        assertNull(viewModel.ocrResult.value)
        assertEquals(0, viewModel.importedCount.value)
        assertTrue(viewModel.previewRows.value.isEmpty())
        assertTrue(viewModel.previewHeaders.value.isEmpty())
    }

    @Test
    fun `processImageInternal sets preview data on success`() = runTest(testDispatcher) {
        val headers = listOf("代码名称", "涨跌幅", "现价")
        val rows = listOf(
            OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%", "现价" to "12.34")),
            OcrRow("000002", "万科A", mapOf("涨跌幅" to "-1.2%", "现价" to "8.56"))
        )
        every { mockTableAnalyzer.analyze(any(), any(), any()) } returns TableAnalyzer.TableResult(headers, rows)

        viewModel.processImageInternal(mockBitmap())

        assertEquals(headers, viewModel.previewHeaders.value)
        assertEquals(2, viewModel.previewRows.value.size)
        assertEquals("000001", viewModel.previewRows.value[0].stockCode)
        assertEquals("+2.5%", viewModel.previewRows.value[0].data["涨跌幅"])
        assertEquals(false, viewModel.isProcessing.value)
    }

    @Test
    fun `processImageInternal sets error on empty result`() = runTest(testDispatcher) {
        every { mockTableAnalyzer.analyze(any(), any(), any()) } returns TableAnalyzer.TableResult(emptyList(), emptyList())

        viewModel.processImageInternal(mockBitmap())

        assertEquals("未识别到有效表格数据，请确认是同花顺股票列表截图", viewModel.error.value)
        assertEquals(false, viewModel.isProcessing.value)
    }

    @Test
    fun `processImageInternal sets isProcessing false on exception`() = runTest(testDispatcher) {
        every { mockTableAnalyzer.analyze(any(), any(), any()) } throws RuntimeException("分析失败")

        viewModel.processImageInternal(mockBitmap())

        assertEquals("识别失败: 分析失败", viewModel.error.value)
        assertEquals(false, viewModel.isProcessing.value)
    }

    // --- updatePreviewCell ---

    @Test
    fun `updatePreviewCell updates cell value`() {
        setupPreviewData(
            listOf(OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%"))),
            listOf("代码名称", "涨跌幅")
        )

        viewModel.updatePreviewCell(0, "涨跌幅", "+3.0%")

        assertEquals("+3.0%", viewModel.previewRows.value[0].data["涨跌幅"])
    }

    @Test
    fun `updatePreviewCell ignores invalid index`() {
        setupPreviewData(
            listOf(OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%"))),
            listOf("代码名称", "涨跌幅")
        )

        viewModel.updatePreviewCell(99, "涨跌幅", "+3.0%")

        assertEquals("+2.5%", viewModel.previewRows.value[0].data["涨跌幅"])
    }

    // --- removePreviewRow ---

    @Test
    fun `removePreviewRow removes row`() {
        setupPreviewData(
            listOf(
                OcrRow("000001", "平安银行", mapOf()),
                OcrRow("000002", "万科A", mapOf())
            ),
            listOf("代码名称")
        )

        viewModel.removePreviewRow(0)

        assertEquals(1, viewModel.previewRows.value.size)
        assertEquals("000002", viewModel.previewRows.value[0].stockCode)
    }

    @Test
    fun `removePreviewRow ignores invalid index`() {
        setupPreviewData(
            listOf(OcrRow("000001", "平安银行", mapOf())),
            listOf("代码名称")
        )

        viewModel.removePreviewRow(99)

        assertEquals(1, viewModel.previewRows.value.size)
    }

    // --- confirmImport ---

    @Test
    fun `confirmImport captures count before clearing`() = runTest(testDispatcher) {
        setupPreviewData(
            listOf(
                OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%")),
                OcrRow("000002", "万科A", mapOf("涨跌幅" to "-1.2%"))
            ),
            listOf("代码名称", "涨跌幅")
        )

        viewModel.confirmImport()
        advanceUntilIdle()

        assertEquals(2, viewModel.importedCount.value)
        assertTrue(viewModel.previewRows.value.isEmpty())
        assertTrue(viewModel.previewHeaders.value.isEmpty())
        assertTrue(viewModel.imported.value)
    }

    @Test
    fun `confirmImport calls mergeScreenshotData for each row`() = runTest(testDispatcher) {
        coEvery { mockStockRepo.mergeScreenshotData(any(), any(), any()) } returns 1L
        setupPreviewData(
            listOf(
                OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%")),
                OcrRow("000002", "万科A", mapOf("涨跌幅" to "-1.2%"))
            ),
            listOf("代码名称", "涨跌幅")
        )

        viewModel.confirmImport()
        advanceUntilIdle()

        coVerify(exactly = 2) { mockStockRepo.mergeScreenshotData(any(), any(), any()) }
        coVerify { mockStockRepo.mergeScreenshotData("000001", "平安银行", mapOf("涨跌幅" to "+2.5%")) }
        coVerify { mockStockRepo.mergeScreenshotData("000002", "万科A", mapOf("涨跌幅" to "-1.2%")) }
    }

    @Test
    fun `confirmImport returns early when empty`() = runTest(testDispatcher) {
        viewModel.confirmImport()
        advanceUntilIdle()

        assertEquals(false, viewModel.imported.value)
        coVerify(exactly = 0) { mockStockRepo.mergeScreenshotData(any(), any(), any()) }
    }

    // --- processImagesInternal ---

    private fun mockBatchContentResolver(): ContentResolver {
        val mockStream = mockk<InputStream>(relaxed = true)
        val cr = mockk<ContentResolver>()
        every { cr.openInputStream(any()) } returns mockStream
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), any(), any()) } returns mockk<Bitmap>(relaxed = true)
        return cr
    }

    @Test
    fun `batch processing merges duplicate stocks by code`() = runTest(testDispatcher) {
        val headers = listOf("代码名称", "涨跌幅")
        val firstBatch = listOf(
            OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%"))
        )
        val secondBatch = listOf(
            OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+3.0%"))
        )
        every { mockTableAnalyzer.analyze(any(), any(), any()) } returnsMany listOf(
            TableAnalyzer.TableResult(headers, firstBatch),
            TableAnalyzer.TableResult(headers, secondBatch)
        )

        val uris = listOf(mockk<Uri>(relaxed = true), mockk<Uri>(relaxed = true))
        val contentResolver = mockBatchContentResolver()

        viewModel.processImagesInternal(uris, contentResolver)

        assertEquals(1, viewModel.previewRows.value.size)
        assertEquals("000001", viewModel.previewRows.value[0].stockCode)
        assertEquals("+2.5%", viewModel.previewRows.value[0].data["涨跌幅"])
    }

    @Test
    fun `batch processing sets error when all images fail`() = runTest(testDispatcher) {
        every { mockTableAnalyzer.analyze(any(), any(), any()) } returns TableAnalyzer.TableResult(emptyList(), emptyList())

        val uris = listOf(mockk<Uri>(relaxed = true))
        val contentResolver = mockBatchContentResolver()

        viewModel.processImagesInternal(uris, contentResolver)

        assertEquals("未识别到有效表格数据，请确认是同花顺股票列表截图", viewModel.error.value)
        assertEquals(false, viewModel.isProcessing.value)
    }

    @Test
    fun `processImagesInternal updates processedCount as images are processed`() = runTest(testDispatcher) {
        val headers = listOf("代码名称", "涨跌幅")
        val rows = listOf(
            OcrRow("000001", "平安银行", mapOf("涨跌幅" to "+2.5%"))
        )
        every { mockTableAnalyzer.analyze(any(), any(), any()) } returns TableAnalyzer.TableResult(headers, rows)

        val uris = listOf(mockk<Uri>(relaxed = true))
        val contentResolver = mockBatchContentResolver()

        viewModel.processImagesInternal(uris, contentResolver)

        assertEquals(1, viewModel.processedCount.value)
    }
}
