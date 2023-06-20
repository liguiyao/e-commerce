package cn.lili.event.impl;

import cn.lili.event.TradeEvent;
import cn.lili.modules.order.cart.entity.dto.TradeDTO;
import cn.lili.modules.order.order.service.TradeService;
import cn.lili.modules.payment.entity.enums.PaymentMethodEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Order状态处理类
 *
 * @author Chopper
 * @since 2020-07-03 11:20
 **/
@Service
public class OrderStatusHandlerExecute implements TradeEvent {


    @Autowired
    private TradeService tradeService;

    @Override
    public void orderCreate(TradeDTO tradeDTO) {
        //如果Order需要支付金额为0，则将Order步入到下一个流程
        if (tradeDTO.getPriceDetailDTO().getFlowPrice() <= 0) {
            tradeService.payTrade(tradeDTO.getSn(), PaymentMethodEnum.BANK_TRANSFER.name(), "-1");
        }

    }
}
