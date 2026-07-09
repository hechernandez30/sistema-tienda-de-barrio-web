package com.tiendadebarrio.reports.controller;

import com.tiendadebarrio.reports.dto.CashByCategoryResponse;
import com.tiendadebarrio.reports.dto.CashSummaryResponse;
import com.tiendadebarrio.reports.dto.DailySalesResponse;
import com.tiendadebarrio.reports.dto.EstimatedProfitResponse;
import com.tiendadebarrio.reports.dto.InventorySummaryResponse;
import com.tiendadebarrio.reports.dto.LowStockResponse;
import com.tiendadebarrio.reports.dto.PurchasesBySupplierResponse;
import com.tiendadebarrio.reports.dto.PurchasesSummaryResponse;
import com.tiendadebarrio.reports.dto.SalesByCategoryResponse;
import com.tiendadebarrio.reports.dto.SalesByPaymentMethodResponse;
import com.tiendadebarrio.reports.dto.SalesSummaryResponse;
import com.tiendadebarrio.reports.dto.TopProductResponse;
import com.tiendadebarrio.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REPORTES')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales-summary")
    public ResponseEntity<SalesSummaryResponse> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.salesSummary(from, to));
    }

    @GetMapping("/sales-by-payment-method")
    public ResponseEntity<List<SalesByPaymentMethodResponse>> salesByPaymentMethod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.salesByPaymentMethod(from, to));
    }

    @GetMapping("/daily-sales")
    public ResponseEntity<List<DailySalesResponse>> dailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.dailySales(from, to));
    }

    @GetMapping("/sales-by-category")
    public ResponseEntity<List<SalesByCategoryResponse>> salesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.salesByCategory(from, to));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductResponse>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(reportService.topProducts(from, to, limit));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockResponse>> lowStock() {
        return ResponseEntity.ok(reportService.lowStock());
    }

    @GetMapping("/inventory-summary")
    public ResponseEntity<InventorySummaryResponse> inventorySummary() {
        return ResponseEntity.ok(reportService.inventorySummary());
    }

    @GetMapping("/purchases-summary")
    public ResponseEntity<PurchasesSummaryResponse> purchasesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.purchasesSummary(from, to));
    }

    @GetMapping("/purchases-by-supplier")
    public ResponseEntity<List<PurchasesBySupplierResponse>> purchasesBySupplier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.purchasesBySupplier(from, to));
    }

    @GetMapping("/cash-summary")
    public ResponseEntity<CashSummaryResponse> cashSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.cashSummary(from, to));
    }

    @GetMapping("/cash-by-category")
    public ResponseEntity<List<CashByCategoryResponse>> cashByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.cashByCategory(from, to));
    }

    @GetMapping("/estimated-profit")
    public ResponseEntity<EstimatedProfitResponse> estimatedProfit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.estimatedProfit(from, to));
    }
}
