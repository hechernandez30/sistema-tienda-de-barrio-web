package com.tiendadebarrio.products.service;

import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.products.dto.CategoryCreateRequest;
import com.tiendadebarrio.products.dto.CategoryResponse;
import com.tiendadebarrio.products.entity.ProductCategory;
import com.tiendadebarrio.products.repository.ProductCategoryRepository;
import com.tiendadebarrio.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return productCategoryRepository.findByDeletedFalseAndActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        String name = request.getName().trim();
        if (productCategoryRepository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new ApiException(
                    "Ya existe una categoría con el nombre " + name,
                    HttpStatus.CONFLICT,
                    "CATEGORY_NAME_DUPLICATED"
            );
        }

        ProductCategory category = ProductCategory.builder()
                .name(name)
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null)
                .active(true)
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        category.setDeleted(false);

        return toResponse(productCategoryRepository.save(category));
    }

    private CategoryResponse toResponse(ProductCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .build();
    }
}
