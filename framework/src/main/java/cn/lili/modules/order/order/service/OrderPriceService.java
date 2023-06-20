package cn.lili.modules.order.order.service;

import cn.lili.modules.order.order.entity.dos.Order;

/**
 * Order价格
 *
 * @author Chopper
 * @since 2020/11/17 7:36 下午
 */
public interface OrderPriceService {

    /**
     * 价格修改
     * 日志功能内部实现
     *
     * @param orderSn    Order编号
     * @param orderPrice Order价格
     * @return Order
     */
    Order updatePrice(String orderSn, Double orderPrice);

    /**
     * 管理员Order付款
     *
     * @param orderSn Order编号
     */
    void adminPayOrder(String orderSn);
}
