package com.tiendadebarrio.reports.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.cash.entity.CashMovementType;
import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.purchases.entity.PurchaseStatus;
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
import com.tiendadebarrio.reports.repository.ReportRepository;
import com.tiendadebarrio.sales.entity.SaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String AUDIT_ACTION = "READ";
    private static final String AUDIT_MODULE = "REPORTS";
    private static final int MONEY_SCALE = 2;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final ReportRepository reportRepository;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Ventas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);

        BigDecimal totalSales = money(reportRepository.sumSalesTotal(SaleStatus.COMPLETED, range.start(), range.end()));
        long salesCount = reportRepository.countSalesByStatus(SaleStatus.COMPLETED, range.start(), range.end());
        long cancelledCount = reportRepository.countSalesByStatus(SaleStatus.CANCELLED, range.start(), range.end());

        BigDecimal average = salesCount > 0
                ? totalSales.divide(BigDecimal.valueOf(salesCount), MONEY_SCALE, RoundingMode.HALF_UP)
                : zero();

        SalesSummaryResponse response = SalesSummaryResponse.builder()
                .fromDate(range.fromDate())
                .toDate(range.toDate())
                .totalSales(totalSales)
                .salesCount(salesCount)
                .cancelledSalesCount(cancelledCount)
                .averageSaleAmount(average)
                .build();

        auditReport("SalesSummary", range);
        return response;
    }

    @Transactional(readOnly = true)
    public List<SalesByPaymentMethodResponse> salesByPaymentMethod(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        return reportRepository.salesByPaymentMethod(SaleStatus.COMPLETED, range.start(), range.end());
    }

    @Transactional(readOnly = true)
    public List<DailySalesResponse> dailySales(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        return reportRepository.dailySales(range.start(), range.end()).stream()
                .map(p -> DailySalesResponse.builder()
                        .date(LocalDate.parse(p.getSaleDay()))
                        .salesCount(p.getSalesCount())
                        .totalAmount(money(p.getTotalAmount()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(LocalDate from, LocalDate to, Integer limit) {
        Range range = resolveRange(from, to);
        int effectiveLimit = normalizeLimit(limit);
        return reportRepository.topProducts(
                SaleStatus.COMPLETED, range.start(), range.end(), PageRequest.of(0, effectiveLimit));
    }

    @Transactional(readOnly = true)
    public List<SalesByCategoryResponse> salesByCategory(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        return reportRepository.salesByCategory(SaleStatus.COMPLETED, range.start(), range.end())
                .stream()
                .map(row -> new SalesByCategoryResponse(
                        row.getCategoryId(),
                        row.getCategoryName(),
                        row.getQuantitySold(),
                        money(row.getTotalAmount()),
                        row.getLineCount()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Inventario
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock() {
        return reportRepository.lowStock();
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse inventorySummary() {
        Object[] values = firstRow(reportRepository.inventoryValues());
        BigDecimal totalCost = values != null ? money((BigDecimal) values[0]) : zero();
        BigDecimal totalSaleValue = values != null ? money((BigDecimal) values[1]) : zero();

        return InventorySummaryResponse.builder()
                .totalProducts(reportRepository.countProducts())
                .activeProducts(reportRepository.countActiveProducts())
                .lowStockProducts(reportRepository.countLowStockProducts())
                .totalInventoryCost(totalCost)
                .totalInventorySaleValue(totalSaleValue)
                .build();
    }

    // ------------------------------------------------------------------
    // Compras
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PurchasesSummaryResponse purchasesSummary(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);

        return PurchasesSummaryResponse.builder()
                .fromDate(range.fromDate())
                .toDate(range.toDate())
                .purchaseCount(reportRepository.countPurchases(range.start(), range.end()))
                .confirmedPurchaseCount(
                        reportRepository.countPurchasesByStatus(PurchaseStatus.CONFIRMED, range.start(), range.end()))
                .cancelledPurchaseCount(
                        reportRepository.countPurchasesByStatus(PurchaseStatus.CANCELLED, range.start(), range.end()))
                .totalPurchasedAmount(
                        money(reportRepository.sumPurchaseTotal(PurchaseStatus.CONFIRMED, range.start(), range.end())))
                .build();
    }

    @Transactional(readOnly = true)
    public List<PurchasesBySupplierResponse> purchasesBySupplier(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        return reportRepository.purchasesBySupplier(PurchaseStatus.CONFIRMED, range.start(), range.end());
    }

    // ------------------------------------------------------------------
    // Caja
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CashSummaryResponse cashSummary(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        List<Object[]> rows = reportRepository.cashTotals(range.start(), range.end());

        BigDecimal cashIncome = zero();
        BigDecimal transferIncome = zero();
        BigDecimal cardIncome = zero();
        BigDecimal totalIncome = zero();
        BigDecimal totalExpenses = zero();

        for (Object[] row : rows) {
            CashMovementType type = (CashMovementType) row[0];
            PaymentMethod method = (PaymentMethod) row[1];
            BigDecimal amount = money((BigDecimal) row[2]);

            if (type == CashMovementType.INCOME) {
                totalIncome = totalIncome.add(amount);
                switch (method) {
                    case CASH -> cashIncome = cashIncome.add(amount);
                    case TRANSFER -> transferIncome = transferIncome.add(amount);
                    case CARD -> cardIncome = cardIncome.add(amount);
                }
            } else if (type == CashMovementType.EXPENSE) {
                totalExpenses = totalExpenses.add(amount);
            }
        }

        CashSummaryResponse response = CashSummaryResponse.builder()
                .fromDate(range.fromDate())
                .toDate(range.toDate())
                .totalCashIncome(cashIncome)
                .totalTransferIncome(transferIncome)
                .totalCardIncome(cardIncome)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netAmount(totalIncome.subtract(totalExpenses))
                .build();

        auditReport("CashSummary", range);
        return response;
    }

    @Transactional(readOnly = true)
    public List<CashByCategoryResponse> cashByCategory(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        return reportRepository.cashByCategory(range.start(), range.end());
    }

    // ------------------------------------------------------------------
    // Utilidad estimada
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public EstimatedProfitResponse estimatedProfit(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        Object[] raw = firstRow(reportRepository.estimatedProfitRaw(SaleStatus.COMPLETED, range.start(), range.end()));

        BigDecimal totalSales = raw != null ? money((BigDecimal) raw[0]) : zero();
        BigDecimal estimatedCost = raw != null ? money((BigDecimal) raw[1]) : zero();

        EstimatedProfitResponse response = EstimatedProfitResponse.builder()
                .fromDate(range.fromDate())
                .toDate(range.toDate())
                .totalSales(totalSales)
                .estimatedCost(estimatedCost)
                .estimatedGrossProfit(totalSales.subtract(estimatedCost))
                .build();

        auditReport("EstimatedProfit", range);
        return response;
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Object[] firstRow(List<Object[]> rows) {
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? zero() : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE);
    }

    private Range resolveRange(LocalDate from, LocalDate to) {
        LocalDate fromDate = from != null ? from : LocalDate.now();
        LocalDate toDate = to != null ? to : LocalDate.now();
        return new Range(fromDate, toDate, fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));
    }

    private void auditReport(String reportName, Range range) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("report", reportName);
        details.put("from", range.fromDate().toString());
        details.put("to", range.toDate().toString());
        auditService.record(AUDIT_ACTION, AUDIT_MODULE, reportName, null, null, details);
    }

    /**
     * Rango de fechas resuelto: fromDate/toDate para exponer en el DTO y start/end (inclusivo)
     * para filtrar por timestamp.
     */
    private record Range(LocalDate fromDate, LocalDate toDate, LocalDateTime start, LocalDateTime end) {
    }
}
