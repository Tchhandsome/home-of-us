package com.homeofus.privatezone.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.privatezone.dto.CreatePrivateMessageRequest;
import com.homeofus.privatezone.repository.PrivateMessageRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 私密留言业务服务。
 *
 * @author tanchaohong
 */
@Service
public class PrivateMessageService {

    private static final String VISIBILITY_PRIVATE = "PRIVATE";

    private static final String VISIBILITY_TO_PARTNER = "TO_PARTNER";

    private static final String VISIBILITY_SHARED = "SHARED";

    private final PrivateMessageRepository privateMessageRepository;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public PrivateMessageService(PrivateMessageRepository privateMessageRepository,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.privateMessageRepository = privateMessageRepository;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建私密留言。
     *
     * @param request 创建请求
     * @return 新留言 ID
     */
    public Map<String, Object> create(CreatePrivateMessageRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String visibility = normalizeVisibility(request.getVisibility());
        Long receiverMemberId = request.getReceiverMemberId();
        if (VISIBILITY_TO_PARTNER.equals(visibility) && Objects.isNull(receiverMemberId)) {
            throw new BusinessException("PRIVATE_MESSAGE_RECEIVER_REQUIRED", "privateMessage.receiver.required");
        }
        if (VISIBILITY_PRIVATE.equals(visibility)) {
            receiverMemberId = null;
        }
        Long id = idGenerator.nextId();
        privateMessageRepository.insert(id, DefaultFamily.FAMILY_ID, currentUser.getMemberId(), receiverMemberId,
                visibility, request.getContent(), currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询当前用户可见留言。
     *
     * @return 留言列表
     */
    public List<Map<String, Object>> findVisibleMessages() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        return privateMessageRepository.findVisibleMessages(DefaultFamily.FAMILY_ID, currentUser.getMemberId());
    }

    /**
     * 标记已读。
     *
     * @param id 留言 ID
     * @return 更新结果
     */
    public Map<String, Object> markRead(Long id) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = privateMessageRepository.markRead(id, DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    private String normalizeVisibility(String visibility) {
        String safeVisibility = StringUtils.defaultIfBlank(visibility, VISIBILITY_PRIVATE).toUpperCase();
        if (VISIBILITY_TO_PARTNER.equals(safeVisibility) || VISIBILITY_SHARED.equals(safeVisibility)
                || VISIBILITY_PRIVATE.equals(safeVisibility)) {
            return safeVisibility;
        }
        return VISIBILITY_PRIVATE;
    }
}
