package cn.lili.event;

import cn.lili.modules.order.order.entity.dto.OrderMessage;

/**
 * Order状态改变事件
 *
 * @author Chopper
 * @since 2020/11/17 7:13 下午
 */
public interface OrderStatusChangeEvent {

    /**
     * Order改变
     * @param orderMessage Order消息
     */
    void orderChange(OrderMessage orderMessage);
}
