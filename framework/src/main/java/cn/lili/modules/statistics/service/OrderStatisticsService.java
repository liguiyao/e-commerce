package cn.lili.modules.statistics.service;

import cn.lili.common.vo.PageVO;
import cn.lili.modules.order.order.entity.dos.Order;
import cn.lili.modules.order.order.entity.vo.OrderSimpleVO;
import cn.lili.modules.statistics.entity.dto.StatisticsQueryParam;
import cn.lili.modules.statistics.entity.vo.OrderOverviewVO;
import cn.lili.modules.statistics.entity.vo.OrderStatisticsDataVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Order统计业务层
 *
 * @author Bulbasaur
 * @since 2020/12/9 11:06
 */
public interface OrderStatisticsService extends IService<Order> {

    /**
     * Order统计概览
     *
     * @param statisticsQueryParam
     * @return
     */
    OrderOverviewVO overview(StatisticsQueryParam statisticsQueryParam);

    /**
     * 获取Order总数量
     *
     * @param orderStatus Order状态
     * @return Order总数量
     */
    long orderNum(String orderStatus);

    /**
     * 图表统计
     *
     * @param statisticsQueryParam 统计查询参数
     * @return Order总数量
     */
    List<OrderStatisticsDataVO> statisticsChart(StatisticsQueryParam statisticsQueryParam);

    /**
     * 获取统计的Order
     *
     * @param statisticsQueryParam
     * @param pageVO
     * @return
     */
    IPage<OrderSimpleVO> getStatistics(StatisticsQueryParam statisticsQueryParam, PageVO pageVO);
}