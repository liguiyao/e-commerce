package cn.lili.modules.order.order.service;

import cn.lili.modules.member.entity.dto.MemberAddressDTO;
import cn.lili.modules.order.cart.entity.dto.TradeDTO;
import cn.lili.modules.order.order.entity.dos.Order;
import cn.lili.modules.order.order.entity.dto.OrderExportDTO;
import cn.lili.modules.order.order.entity.dto.OrderMessage;
import cn.lili.modules.order.order.entity.dto.OrderSearchParams;
import cn.lili.modules.order.order.entity.vo.OrderDetailVO;
import cn.lili.modules.order.order.entity.vo.OrderSimpleVO;
import cn.lili.modules.order.order.entity.vo.PaymentLog;
import cn.lili.modules.system.entity.vo.Traces;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 子Order业务层
 *
 * @author Chopper
 * @since 2020/11/17 7:36 下午
 */
public interface OrderService extends IService<Order> {


    /**
     * 系统取消Order
     *
     * @param orderSn Order编号
     * @param reason  错误原因
     */
    void systemCancel(String orderSn, String reason);

    /**
     * 根据sn查询
     *
     * @param orderSn Order编号
     * @return Order信息
     */
    Order getBySn(String orderSn);


    /**
     * Order查询
     *
     * @param orderSearchParams 查询参数
     * @return 简短Order分页
     */
    IPage<OrderSimpleVO> queryByParams(OrderSearchParams orderSearchParams);

    /**
     * Order信息
     *
     * @param orderSearchParams 查询参数
     * @return Order信息
     */
    List<Order> queryListByParams(OrderSearchParams orderSearchParams);

    /**
     * 根据促销查询Order
     *
     * @param orderPromotionType Order类型
     * @param payStatus          支付状态
     * @param parentOrderSn      依赖Order编号
     * @param orderSn            Order编号
     * @return Order信息
     */
    List<Order> queryListByPromotion(String orderPromotionType, String payStatus, String parentOrderSn, String orderSn);

    /**
     * 根据促销查询Order
     *
     * @param orderPromotionType Order类型
     * @param payStatus          支付状态
     * @param parentOrderSn      依赖Order编号
     * @param orderSn            Order编号
     * @return Order信息
     */
    long queryCountByPromotion(String orderPromotionType, String payStatus, String parentOrderSn, String orderSn);

    /**
     * 父级拼团Order分组
     *
     * @param pintuanId 拼团id
     * @return 拼团Order信息
     */
    List<Order> queryListByPromotion(String pintuanId);


    /**
     * 查询导出Order列表
     *
     * @param orderSearchParams 查询参数
     * @return 导出Order列表
     */
    List<OrderExportDTO> queryExportOrder(OrderSearchParams orderSearchParams);


    /**
     * Order详细
     *
     * @param orderSn OrderSN
     * @return Order详细
     */
    OrderDetailVO queryDetail(String orderSn);

    /**
     * createOrder
     * 1.检查交易信息
     * 2.循环交易购物车列表，createOrder以及相关信息
     *
     * @param tradeDTO 交易DTO
     */
    void intoDB(TradeDTO tradeDTO);

    /**
     * Order付款
     * 修改Order付款信息
     * 记录Order流水
     *
     * @param orderSn       Order编号
     * @param paymentMethod 支付方法
     * @param receivableNo  第三方流水
     */
    void payOrder(String orderSn, String paymentMethod, String receivableNo);

    /**
     * Order确认成功
     *
     * @param orderSn
     */
    void afterOrderConfirm(String orderSn);

    /**
     * 取消Order
     *
     * @param orderSn OrderSN
     * @param reason  取消理由
     * @return Order
     */
    Order cancel(String orderSn, String reason);


    /**
     * 发货信息修改
     * 日志功能内部实现
     *
     * @param orderSn          Order编号
     * @param memberAddressDTO 收货地址信息
     * @return Order
     */
    Order updateConsignee(String orderSn, MemberAddressDTO memberAddressDTO);

    /**
     * Order发货
     *
     * @param orderSn       Order编号
     * @param invoiceNumber 发货单号
     * @param logisticsId   logistics公司
     * @return Order
     */
    Order delivery(String orderSn, String invoiceNumber, String logisticsId);

    /**
     * Order发货
     *
     * @param orderSn       Order编号
     * @return Order
     */
    Order shunFengDelivery(String orderSn);

    /**
     * 获取logistics踪迹
     *
     * @param orderSn Order编号
     * @return logistics踪迹
     */
    Traces getTraces(String orderSn);

    /**
     * 获取地图版 logistics踪迹
     *
     * @param orderSn Order编号
     * @return logistics踪迹
     */
    Traces getMapTraces(String orderSn);

    /**
     * Order核验
     *
     * @param verificationCode 验证码
     * @param orderSn          Order编号
     * @return Order
     */
    Order take(String orderSn, String verificationCode);


    /**
     * Order核验
     *
     * @param verificationCode 验证码
     * @return Order
     */
    Order take(String verificationCode);

    /**
     * 根据核验码获取Order信息
     *
     * @param verificationCode 验证码
     * @return Order
     */
    Order getOrderByVerificationCode(String verificationCode);

    /**
     * Order完成
     *
     * @param orderSn Order编号
     */
    void complete(String orderSn);

    /**
     * 系统定时完成Order
     *
     * @param orderSn Order编号
     */
    void systemComplete(String orderSn);

    /**
     * 通过trade 获取Order列表
     *
     * @param tradeSn 交易编号
     * @return Order列表
     */
    List<Order> getByTradeSn(String tradeSn);

    /**
     * 发送更新Order状态的信息
     *
     * @param orderMessage Order传输信息
     */
    void sendUpdateStatusMessage(OrderMessage orderMessage);

    /**
     * 根据Ordersn逻辑删除Order
     *
     * @param sn Ordersn
     */
    void deleteOrder(String sn);

    /**
     * 开具发票
     *
     * @param sn Ordersn
     * @return
     */
    Boolean invoice(String sn);

    /**
     * 自动成团Order处理
     *
     * @param pintuanId     拼团活动id
     * @param parentOrderSn 拼团Ordersn
     */
    void agglomeratePintuanOrder(String pintuanId, String parentOrderSn);

    /**
     * 获取待发货Order编号列表
     *
     * @param response      响应
     * @param logisticsName 店铺已选择logistics公司列表
     */
    void getBatchDeliverList(HttpServletResponse response, List<String> logisticsName);

    /**
     * Order批量发货
     *
     * @param files 文件
     */
    void batchDeliver(MultipartFile files);


    /**
     * 获取Order实际支付的总金额
     *
     * @param orderSn Ordersn
     * @return 金额
     */
    Double getPaymentTotal(String orderSn);

    /**
     * 查询Order支付记录
     *
     * @param page         分页
     * @param queryWrapper 查询条件
     * @return Order支付记录分页
     */
    IPage<PaymentLog> queryPaymentLogs(IPage<PaymentLog> page, Wrapper<PaymentLog> queryWrapper);

    /**
     * 检查是否开始虚拟成团
     *
     * @param pintuanId   拼团活动id
     * @param requiredNum 成团人数
     * @param fictitious  是否开启成团
     * @return 是否成功
     */
    boolean checkFictitiousOrder(String pintuanId, Integer requiredNum, Boolean fictitious);

}