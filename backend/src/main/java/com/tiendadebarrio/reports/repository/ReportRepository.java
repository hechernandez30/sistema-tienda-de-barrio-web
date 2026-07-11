package com.tiendadebarrio.reports.repository;

import com.tiendadebarrio.purchases.entity.PurchaseStatus;
import com.tiendadebarrio.reports.dto.CashByCategoryResponse;
import com.tiendadebarrio.reports.dto.DailySalesProjection;
import com.tiendadebarrio.reports.dto.LowStockResponse;
import com.tiendadebarrio.reports.dto.PurchasesBySupplierResponse;
import com.tiendadebarrio.reports.dto.SalesByCategoryResponse;
import com.tiendadebarrio.reports.dto.SalesByPaymentMethodResponse;
import com.tiendadebarrio.reports.dto.TopProductResponse;
import com.tiendadebarrio.sales.entity.Sale;
import com.tiendadebarrio.sales.entity.SaleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de solo lectura para reportes. Usa JPQL con proyecciones DTO y una consulta
 * nativa para la agrupación por día. La raíz {@link Sale} es sólo formal: las consultas JPQL
 * pueden referenciar cualquier entidad del modelo.
 */
public interface ReportRepository extends Repository<Sale, UUID> {

    // ------------------------------------------------------------------
    // Ventas
    // ------------------------------------------------------------------

    @Query("""
            SELECT COALESCE(SUM(s.total), 0)
            FROM Sale s
            WHERE s.deleted = false
              AND s.status = :status
              AND s.saleDate BETWEEN :from AND :to
            """)
    BigDecimal sumSalesTotal(@Param("status") SaleStatus status,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(s)
            FROM Sale s
            WHERE s.deleted = false
              AND s.status = :status
              AND s.saleDate BETWEEN :from AND :to
            """)
    long countSalesByStatus(@Param("status") SaleStatus status,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    @Query("""
            SELECT new com.tiendadebarrio.reports.dto.SalesByPaymentMethodResponse(
                s.paymentMethod, COUNT(s), COALESCE(SUM(s.total), 0))
            FROM Sale s
            WHERE s.deleted = false
              AND s.status = :status
              AND s.saleDate BETWEEN :from AND :to
            GROUP BY s.paymentMethod
            ORDER BY s.paymentMethod
            """)
    List<SalesByPaymentMethodResponse> salesByPaymentMethod(@Param("status") SaleStatus status,
                                                            @Param("from") LocalDateTime from,
                                                            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT to_char(sale_date, 'YYYY-MM-DD') AS saleDay,
                   COUNT(*) AS salesCount,
                   COALESCE(SUM(total), 0) AS totalAmount
            FROM sales
            WHERE is_deleted = false
              AND status = 'COMPLETED'
              AND sale_date BETWEEN :from AND :to
            GROUP BY to_char(sale_date, 'YYYY-MM-DD')
            ORDER BY saleDay
            """, nativeQuery = true)
    List<DailySalesProjection> dailySales(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("""
            SELECT new com.tiendadebarrio.reports.dto.TopProductResponse(
                p.id, p.barcode, p.name,
                COALESCE(SUM(si.quantity), 0), COALESCE(SUM(si.lineTotal), 0))
            FROM SaleItem si
            JOIN si.product p
            JOIN si.sale s
            WHERE s.deleted = false
              AND s.status = :status
              AND s.saleDate BETWEEN :from AND :to
            GROUP BY p.id, p.barcode, p.name
            ORDER BY SUM(si.quantity) DESC
            """)
    List<TopProductResponse> topProducts(@Param("status") SaleStatus status,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         Pageable pageable);

    @Query("""
            SELECT new com.tiendadebarrio.reports.dto.SalesByCategoryResponse(
                c.id,
                CASE WHEN c IS NULL THEN 'Sin categoría' ELSE c.name END,
                COALESCE(SUM(si.quantity), 0),
                COALESCE(SUM(si.lineTotal), 0),
                COALESCE(SUM(si.quantity * p.purchasePrice), 0),
                COALESCE(SUM(si.lineTotal), 0) - COALESCE(SUM(si.quantity * p.purchasePrice), 0),
                COUNT(si))
            FROM SaleItem si
            JOIN si.product p
            LEFT JOIN p.category c
            JOIN si.sale s
            WHERE s.deleted = false
              AND s.status = :status
              AND s.saleDate BETWEEN :from AND :to
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:uncategorizedOnly = false OR c IS NULL)
            GROUP BY c.id, c.name
            ORDER BY SUM(si.lineTotal) DESC
            """)
    List<SalesByCategoryResponse> salesByCategory(@Param("status") SaleStatus status,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to,
                                                  @Param("categoryId") UUID categoryId,
                                                  @Param("uncategorizedOnly") boolean uncategorizedOnly);

    // ------------------------------------------------------------------
    // Inventario
    // ------------------------------------------------------------------

    @Query("""
            SELECT new com.tiendadebarrio.reports.dto.LowStockResponse(
                p.id, p.barcode, p.name, p.currentStock, p.minStock, c.name)
            FROM Product p
            LEFT JOIN p.category c
            WHERE p.deleted = false
              AND p.active = true
              AND p.currentStock <= p.minStock
            ORDER BY p.name
            """)
    List<LowStockResponse> lowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false")
    long countProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false AND p.active = true")
    long countActiveProducts();

    @Query("""
            SELECT COUNT(p)
            FROM Product p
            WHERE p.deleted = false
              AND p.active = true
              AND p.currentStock <= p.minStock
            """)
    long countLowStockProducts();

    @Query("""
            SELECT COALESCE(SUM(p.currentStock * p.purchasePrice), 0),
                   COALESCE(SUM(p.currentStock * p.salePrice), 0)
            FROM Product p
            WHERE p.deleted = false
            """)
    List<Object[]> inventoryValues();

    // ------------------------------------------------------------------
    // Compras
    // ------------------------------------------------------------------

    @Query("""
            SELECT COUNT(p)
            FROM Purchase p
            WHERE p.deleted = false
              AND p.purchaseDate BETWEEN :from AND :to
            """)
    long countPurchases(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(p)
            FROM Purchase p
            WHERE p.deleted = false
              AND p.status = :status
              AND p.purchaseDate BETWEEN :from AND :to
            """)
    long countPurchasesByStatus(@Param("status") PurchaseStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(p.total), 0)
            FROM Purchase p
            WHERE p.deleted = false
              AND p.status = :status
              AND p.purchaseDate BETWEEN :from AND :to
            """)
    BigDecimal sumPurchaseTotal(@Param("status") PurchaseStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    @Query("""
            SELECT new com.tiendadebarrio.reports.dto.PurchasesBySupplierResponse(
                sup.id, sup.name, COUNT(p), COALESCE(SUM(p.total), 0))
            FROM Purchase p
            JOIN p.supplier sup
            WHERE p.deleted = false
              AND p.status = :status
              AND p.purchaseDate BETWEEN :from AND :to
            GROUP BY sup.id, sup.name
            ORDER BY SUM(p.total) DESC
            """)
    List<PurchasesBySupplierResponse> purchasesBySupplier(@Param("status") PurchaseStatus status,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);

    // ------------------------------------------------------------------
    // Caja
    // ------------------------------------------------------------------

    @Query("""
            SELECT cm.movementType, cm.paymentMethod, COALESCE(SUM(cm.amount), 0)
            FROM CashMovement cm
            WHERE cm.deleted = false
              AND cm.createdAt BETWEEN :from AND :to
            GROUP BY cm.movementType, cm.paymentMethod
            """)
    List<Object[]> cashTotals(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT new com.tiendadebarrio.reports.dto.CashByCategoryResponse(
                cm.movementType, cm.category, cm.paymentMethod, COUNT(cm), COALESCE(SUM(cm.amount), 0))
            FROM CashMovement cm
            WHERE cm.deleted = false
              AND cm.createdAt BETWEEN :from AND :to
            GROUP BY cm.movementType, cm.category, cm.paymentMethod
            ORDER BY cm.movementType, cm.category, cm.paymentMethod
            """)
    List<CashByCategoryResponse> cashByCategory(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    // ------------------------------------------------------------------
    // Utilidad estimada
    // ------------------------------------------------------------------

    @Query("""
            SELECT COALESCE(SUM(si.lineTotal), 0),
                   COALESCE(SUM(si.quantity * p.purchasePrice), 0)
            FROM SaleItem si
            JOIN si.product p
            JOIN si.sale s
            WHERE s.deleted = false
              AND s.status = :status
              AND s.saleDate BETWEEN :from AND :to
            """)
    List<Object[]> estimatedProfitRaw(@Param("status") SaleStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
