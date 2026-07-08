package com.homeofus.inventory.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.inventory.dto.CreateInventoryItemRequest;
import com.homeofus.inventory.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 家庭库存业务服务。
 *
 * @author tanchaohong
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public InventoryService(InventoryRepository inventoryRepository, IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.inventoryRepository = inventoryRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建库存项。
     *
     * @param request 创建请求
     * @return 新库存项 ID
     */
    public Map<String, Object> create(CreateInventoryItemRequest request) {
        if (Objects.isNull(request.getQuantity())) {
            request.setQuantity(BigDecimal.ZERO);
        }
        String status = resolveStatus(request);
        Long id = idGenerator.nextId();
        inventoryRepository.insert(id, DefaultFamily.FAMILY_ID, request, status, timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询库存列表。
     *
     * @return 库存列表
     */
    public List<Map<String, Object>> findItems() {
        return inventoryRepository.findItems(DefaultFamily.FAMILY_ID);
    }

    private String resolveStatus(CreateInventoryItemRequest request) {
        if (Objects.nonNull(request.getLowStockThreshold())
                && request.getQuantity().compareTo(request.getLowStockThreshold()) <= 0) {
            return "LOW";
        }
        return "NORMAL";
    }
}

