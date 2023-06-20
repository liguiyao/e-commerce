package cn.lili.modules.order.order.serviceimpl;

import cn.hutool.json.JSONUtil;
import cn.lili.common.enums.ResultCode;
import cn.lili.common.exception.ServiceException;
import cn.lili.common.security.OperationalJudgment;
import cn.lili.common.utils.CurrencyUtil;
import cn.lili.modules.order.order.aop.OrderLogPoint;
import cn.lili.modules.order.order.entity.dos.Order;
import cn.lili.modules.order.order.entity.dos.OrderItem;
import cn.lili.modules.order.order.entity.dto.PriceDetailDTO;
import cn.lili.modules.order.order.entity.enums.PayStatusEnum;
import cn.lili.modules.order.order.mapper.TradeMapper;
import cn.lili.modules.order.order.service.OrderItemService;
import cn.lili.modules.order.order.service.OrderPriceService;
import cn.lili.modules.order.order.service.OrderService;
import cn.lili.modules.payment.kit.plugin.bank.BankTransferPlugin;
import cn.lili.modules.system.aspect.annotation.SystemLogPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Order价格业务层实现
 *
 * @author Chopper
 * @since 2020/11/17 7:36 下午
 */
@Slf4j
@Service
public class OrderPriceServiceImpl implements OrderPriceService {

    /**
     * 线下收款
     */
    @Autowired
    private BankTransferPlugin bankTransferPlugin;
    /**
     * Order货物
     */
    @Autowired
    private OrderItemService orderItemService;
    /**
     * 交易数据层
     */
    @Resource
    private TradeMapper tradeMapper;
    /**
     * Order
     */
    @Autowired
    private OrderService orderService;

    @Override
    @SystemLogPoint(description = "修改Order价格", customerLog = "'Order编号:'+#orderSn +'，价格修改为：'+#orderPrice")
    @OrderLogPoint(description = "'Order['+#orderSn+']修改价格，修改后价格为['+#orderPrice+']'", orderSn = "#orderSn")
    public Order updatePrice(String orderSn, Double orderPrice) {

        //修改Order金额
        Order order = updateOrderPrice(orderSn, orderPrice);

        //修改交易金额
        tradeMapper.updateTradePrice(order.getTradeSn());
        return order;
    }

    @Override
    @OrderLogPoint(description = "'管理员操作Order['+#orderSn+']付款'", orderSn = "#orderSn")
    public void adminPayOrder(String orderSn) {
        Order order = OperationalJudgment.judgment(orderService.getBySn(orderSn));
        //如果Order已付款，则抛出异常
        if (order.getPayStatus().equals(PayStatusEnum.PAID.name())) {
            throw new ServiceException(ResultCode.PAY_DOUBLE_ERROR);
        }

        bankTransferPlugin.callBack(order);
    }


    /**
     * 修改Order价格
     * 1.判定Order是否支付
     * 2.记录Order原始价格信息
     * 3.计算修改的Order金额
     * 4.修改Order价格
     * 5.保存Order信息
     *
     * @param orderSn    Order编号
     * @param orderPrice 修改Order金额
     */
    private Order updateOrderPrice(String orderSn, Double orderPrice) {
        Order order = OperationalJudgment.judgment(orderService.getBySn(orderSn));
        //判定是否支付
        if (order.getPayStatus().equals(PayStatusEnum.PAID.name())) {
            throw new ServiceException(ResultCode.ORDER_UPDATE_PRICE_ERROR);
        }

        //获取Order价格信息
        PriceDetailDTO orderPriceDetailDTO = order.getPriceDetailDTO();

        //修改Order价格
        order.setUpdatePrice(CurrencyUtil.sub(orderPrice, orderPriceDetailDTO.getOriginalPrice()));

        //Order修改金额=使用Order原始金额-修改后金额
        orderPriceDetailDTO.setUpdatePrice(CurrencyUtil.sub(orderPrice, orderPriceDetailDTO.getOriginalPrice()));
        order.setFlowPrice(orderPriceDetailDTO.getFlowPrice());
        //修改Order
        order.setPriceDetailDTO(orderPriceDetailDTO);
        orderService.updateById(order);

        //修改子Order
        updateOrderItemPrice(order);

        return order;
    }

    /**
     * 修改Order货物金额
     * 1.计算Order货物金额在Order金额中的百分比
     * 2.Order货物金额=Order修改后金额*Order货物百分比
     * 3.Order货物修改价格=Order货物原始价格-Order货物修改后金额
     * 4.修改平台佣金
     * 5.Order实际金额=修改后Order金额-平台佣金-分销提佣
     *
     * @param order Order
     */
    private void updateOrderItemPrice(Order order) {
        List<OrderItem> orderItems = orderItemService.getByOrderSn(order.getSn());

        //获取总数，入欧最后一个则将其他orderitem的修改金额累加，然后进行扣减
        Integer index = orderItems.size();
        Double countUpdatePrice = 0D;
        for (OrderItem orderItem : orderItems) {

            //获取Order货物价格信息
            PriceDetailDTO priceDetailDTO = orderItem.getPriceDetailDTO();

            index--;
            //如果是最后一个
            if (index == 0) {
                //记录修改金额
                priceDetailDTO.setUpdatePrice(CurrencyUtil.sub(order.getUpdatePrice(), countUpdatePrice));
                //修改Order货物金额
                orderItem.setFlowPrice(priceDetailDTO.getFlowPrice());
                orderItem.setUnitPrice(CurrencyUtil.div(priceDetailDTO.getFlowPrice(), orderItem.getNum()));
                orderItem.setPriceDetail(JSONUtil.toJsonStr(priceDetailDTO));

            } else {

                //SKU占总Order 金额的百分比
                Double priceFluctuationRatio = CurrencyUtil.div(priceDetailDTO.getOriginalPrice(), order.getPriceDetailDTO().getOriginalPrice(), 4);

                //记录修改金额
                priceDetailDTO.setUpdatePrice(CurrencyUtil.mul(order.getUpdatePrice(), priceFluctuationRatio));

                //修改Order货物金额
                orderItem.setFlowPrice(priceDetailDTO.getFlowPrice());
                orderItem.setUnitPrice(CurrencyUtil.div(priceDetailDTO.getFlowPrice(), orderItem.getNum()));
                orderItem.setPriceDetail(JSONUtil.toJsonStr(priceDetailDTO));
                countUpdatePrice = CurrencyUtil.add(countUpdatePrice, priceDetailDTO.getUpdatePrice());
            }
        }
        orderItemService.updateBatchById(orderItems);

    }

}
