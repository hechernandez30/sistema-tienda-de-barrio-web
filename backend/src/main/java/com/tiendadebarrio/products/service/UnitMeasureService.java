package com.tiendadebarrio.products.service;

import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.products.dto.UnitMeasureCreateRequest;
import com.tiendadebarrio.products.dto.UnitMeasureResponse;
import com.tiendadebarrio.products.entity.UnitMeasure;
import com.tiendadebarrio.products.repository.UnitMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitMeasureService {

    private final UnitMeasureRepository unitMeasureRepository;

    @Transactional(readOnly = true)
    public List<UnitMeasureResponse> list() {
        return unitMeasureRepository.findByDeletedFalseAndActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UnitMeasureResponse create(UnitMeasureCreateRequest request) {
        String code = request.getCode().trim();
        if (unitMeasureRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            throw new ApiException(
                    "Ya existe una unidad de medida con el código " + code,
                    HttpStatus.CONFLICT,
                    "UNIT_MEASURE_CODE_DUPLICATED"
            );
        }

        UnitMeasure unitMeasure = UnitMeasure.builder()
                .code(code)
                .name(request.getName().trim())
                .active(true)
                .build();
        unitMeasure.setDeleted(false);

        return toResponse(unitMeasureRepository.save(unitMeasure));
    }

    private UnitMeasureResponse toResponse(UnitMeasure unitMeasure) {
        return UnitMeasureResponse.builder()
                .id(unitMeasure.getId())
                .code(unitMeasure.getCode())
                .name(unitMeasure.getName())
                .active(unitMeasure.isActive())
                .build();
    }
}
