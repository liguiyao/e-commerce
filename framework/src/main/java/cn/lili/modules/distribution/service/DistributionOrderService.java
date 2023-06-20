package cn.lili.modules.distribution.service;

import cn.hutool.core.date.DateTime;
import cn.lili.modules.distribution.entity.dos.DistributionOrder;
import cn.lili.modules.distribution.entity.vos.DistributionOrderSearchParams;
import cn.lili.modules.order.order.entity.dos.OrderItem;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * 分销Order业务层
 *
 * @author pikachu
 * @since 2020-03-15 10:46:33
 */
public interface DistributionOrderService extends IService<DistributionOrder> {

    /**
     * 获取分销Order分页
     * @param distributionOrderSearchParams 分销Order搜索参数
     * @return 分销Order分页
     */
    IPage<DistributionOrder> getDistributionOrderPage(DistributionOrderSearchParams distributionOrderSearchParams);

    /**
     * 支付Order
     * 记录分销Order
     *
     * @param orderSn Order编号
     */
    void calculationDistribution(String orderSn);

    /**
     * 取消Order
     * 记录分销Order
     *
     * @param orderSn Order编号
     */
    void cancelOrder(String orderSn);

    /**
     * Order退款
     * 记录分销Order
     *
     * @param afterSaleSn 售后单号
     */
    void refundOrder(String afterSaleSn);

    /**
     * 分销Order状态修改
     *
     * @param orderItems
     */
    void updateDistributionOrderStatus(List<OrderItem> orderItems);

    /**
     * 分销Order结算
     * @param dateTime
     * @param distributionOrderStatus
     */
    void updateRebate(DateTime dateTime, String distributionOrderStatus);
}