package cn.lili.modules.order.order.entity.dto;

import cn.lili.modules.order.order.entity.enums.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信息队列传输Order信息实体
 *
 * @author paulG
 * @since 2020/12/9
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage {


    /**
     * Order号
     */
    private String orderSn;

    /**
     * 新状态
     *
     * @see OrderStatusEnum
     */
    private OrderStatusEnum newStatus;

    /**
     * 支付方式
     */
    private String paymentMethod;

}
