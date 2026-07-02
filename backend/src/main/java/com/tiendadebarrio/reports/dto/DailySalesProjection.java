package com.tiendadebarrio.reports.dto;

import java.math.BigDecimal;

/**
 * Proyección nativa para agrupar ventas por día. El día se devuelve como texto (YYYY-MM-DD)
 * para evitar problemas de conversión de tipos de fecha entre JDBC y Java.
 */
public interface DailySalesProjection {

    String getSaleDay();

    Long getSalesCount();

    BigDecimal getTotalAmount();
}
