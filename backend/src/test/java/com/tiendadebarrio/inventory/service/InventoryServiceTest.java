package com.tiendadebarrio.inventory.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.inventory.dto.InventoryAdjustmentRequest;
import com.tiendadebarrio.inventory.dto.InventoryMovementResponse;
import com.tiendadebarrio.inventory.entity.InventoryMovement;
import com.tiendadebarrio.inventory.entity.InventoryMovementType;
import com.tiendadebarrio.inventory.mapper.InventoryMapper;
import com.tiendadebarrio.inventory.repository.InventoryMovementRepository;
import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ProductLotService productLotService;

    private InventoryService inventoryService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                productRepository,
                inventoryMovementRepository,
                new InventoryMapper(),
                auditService,
                productLotService);
        productId = UUID.randomUUID();
    }

    private Product activeProduct(String currentStock) {
        Product product = Product.builder()
                .id(productId)
                .barcode("7501234567890")
                .name("Producto de prueba")
                .purchasePrice(new BigDecimal("5.00"))
                .salePrice(new BigDecimal("8.00"))
                .minStock(new BigDecimal("3.000"))
                .currentStock(new BigDecimal(currentStock))
                .active(true)
                .build();
        product.setDeleted(false);
        return product;
    }

    private InventoryAdjustmentRequest request(String quantity) {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest();
        request.setProductId(productId);
        request.setQuantity(new BigDecimal(quantity));
        request.setNotes("Ajuste de prueba");
        return request;
    }

    @Test
    void adjustInIncreasesStockAndRegistersMovement() {
        Product product = activeProduct("10.000");
        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(inventoryMovementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryMovementResponse response = inventoryService.adjustIn(request("5.000"));

        assertThat(response.getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT_IN);
        assertThat(response.getPreviousStock()).isEqualByComparingTo("10.000");
        assertThat(response.getNewStock()).isEqualByComparingTo("15.000");
        assertThat(product.getCurrentStock()).isEqualByComparingTo("15.000");
        verify(productRepository).save(product);
        verify(auditService).record(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void adjustOutDecreasesStockAndRegistersMovement() {
        Product product = activeProduct("10.000");
        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(inventoryMovementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);

        InventoryMovementResponse response = inventoryService.adjustOut(request("4.000"));

        assertThat(response.getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT_OUT);
        assertThat(response.getNewStock()).isEqualByComparingTo("6.000");
        assertThat(product.getCurrentStock()).isEqualByComparingTo("6.000");

        verify(inventoryMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getQuantity()).isEqualByComparingTo("4.000");
    }

    @Test
    void adjustOutFailsWhenStockWouldBeNegative() {
        Product product = activeProduct("5.000");
        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> inventoryService.adjustOut(request("20.000")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(product.getCurrentStock()).isEqualByComparingTo("5.000");
        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    void adjustInFailsWhenProductNotFound() {
        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.adjustIn(request("5.000")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    void adjustInFailsWhenProductInactive() {
        Product product = activeProduct("10.000");
        product.setActive(false);
        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> inventoryService.adjustIn(request("5.000")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("PRODUCT_INACTIVE"));

        verify(inventoryMovementRepository, never()).save(any());
    }
}
