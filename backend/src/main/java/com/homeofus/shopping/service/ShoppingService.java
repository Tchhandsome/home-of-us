package com.homeofus.shopping.service;

import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.finance.repository.FinanceRepository;
import com.homeofus.shopping.dto.CompleteShoppingItemRequest;
import com.homeofus.shopping.dto.CreateShoppingItemRequest;
import com.homeofus.shopping.repository.ShoppingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物清单业务服务。
 *
 * @author tanchaohong
 */
@Service
public class ShoppingService {

    private final ShoppingRepository shoppingRepository;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    private final FinanceRepository financeRepository;

    private final CurrentUserProvider currentUserProvider;

    public ShoppingService(ShoppingRepository shoppingRepository, IdGenerator idGenerator, TimeProvider timeProvider,
            FinanceRepository financeRepository, CurrentUserProvider currentUserProvider) {
        this.shoppingRepository = shoppingRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.financeRepository = financeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 创建购物项。
     *
     * @param request 创建请求
     * @return 新购物项 ID
     */
    public Map<String, Object> create(CreateShoppingItemRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        shoppingRepository.insert(id, DefaultFamily.FAMILY_ID, request, currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询购物清单。
     *
     * @return 购物项列表
     */
    public List<Map<String, Object>> findItems() {
        return shoppingRepository.findItems(DefaultFamily.FAMILY_ID);
    }

    /**
     * 勾选购物项。
     *
     * @param id 购物项 ID
     * @param request 完成请求
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> check(Long id, CompleteShoppingItemRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Map<String, Object> item = shoppingRepository.findById(id, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("SHOPPING_ITEM_NOT_FOUND", "shopping.item.notFound"));
        Long buyerId = Objects.isNull(request.getBuyerId()) ? currentUser.getMemberId() : request.getBuyerId();
        Long financeRecordId = numberValue(item, "finance_record_id");
        if ("DONE".equals(textValue(item, "status")) && Objects.nonNull(financeRecordId)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("updated", 0);
            result.put("financeRecordId", financeRecordId);
            return result;
        }
        if (Objects.isNull(financeRecordId)) {
            financeRecordId = idGenerator.nextId();
            String category = StringUtils.defaultIfBlank(request.getCategory(), textValue(item, "category"));
            String safeCategory = StringUtils.defaultIfBlank(category, "OTHER");
            financeRepository.insertDirect(financeRecordId, DefaultFamily.FAMILY_ID,
                    "购物 - " + textValue(item, "name"), request.getActualAmount(), "EXPENSE", safeCategory, buyerId,
                    currentUser.getUserId(), timeProvider.today(), timeProvider.now());
        }
        int updated = shoppingRepository.complete(id, DefaultFamily.FAMILY_ID, request.getActualAmount(), buyerId,
                financeRecordId, currentUser.getUserId(), timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("financeRecordId", financeRecordId);
        return result;
    }

    /**
     * 删除购物项。
     *
     * @param id 购物项 ID
     * @return 更新结果
     */
    public Map<String, Object> delete(Long id) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = shoppingRepository.delete(id, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        if (updated == 0) {
            throw new BusinessException("SHOPPING_ITEM_NOT_FOUND", "shopping.item.notFound");
        }
        return Map.of("updated", updated);
    }

    private Long numberValue(Map<String, Object> item, String key) {
        Object value = item.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private String textValue(Map<String, Object> item, String key) {
        Object value = item.get(key);
        if (Objects.isNull(value)) {
            return "";
        }
        return String.valueOf(value);
    }
}
