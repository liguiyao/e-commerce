package cn.lili.event;

import cn.lili.modules.order.cart.entity.dto.TradeDTO;

/**
 * Ordercreate消息
 *
 * @author Chopper
 * @since 2021/2/2 15:15
 */
public interface TradeEvent {

    /**
     * Ordercreate
     *
     * @param tradeDTO 交易
     */
    void orderCreate(TradeDTO tradeDTO);

}
