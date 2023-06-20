package cn.lili.modules.order.trade.service;

import cn.lili.modules.order.trade.entity.dos.OrderLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Order日志业务层
 *
 * @author Chopper
 * @since 2020-02-25 14:10:16
 */
public interface OrderLogService extends IService<OrderLog> {

    /**
     * 根据Order编号获取Order日志列表
     * @param orderSn Order编号
     * @return Order日志列表
     */
    List<OrderLog> getOrderLog(String orderSn);
}