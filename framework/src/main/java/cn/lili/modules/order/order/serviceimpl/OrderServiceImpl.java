package cn.lili.modules.order.order.serviceimpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.lili.common.enums.PromotionTypeEnum;
import cn.lili.common.enums.ResultCode;
import cn.lili.common.event.TransactionCommitSendMQEvent;
import cn.lili.common.exception.ServiceException;
import cn.lili.common.properties.RocketmqCustomProperties;
import cn.lili.common.security.OperationalJudgment;
import cn.lili.common.security.context.UserContext;
import cn.lili.common.security.enums.UserEnums;
import cn.lili.common.utils.SnowFlake;
import cn.lili.modules.goods.entity.dto.GoodsCompleteMessage;
import cn.lili.modules.logistics.entity.enums.LogisticsEnum;
import cn.lili.modules.member.entity.dto.MemberAddressDTO;
import cn.lili.modules.member.service.StoreLogisticsService;
import cn.lili.modules.order.cart.entity.dto.TradeDTO;
import cn.lili.modules.order.cart.entity.enums.DeliveryMethodEnum;
import cn.lili.modules.order.order.aop.OrderLogPoint;
import cn.lili.modules.order.order.entity.dos.*;
import cn.lili.modules.order.order.entity.dto.OrderBatchDeliverDTO;
import cn.lili.modules.order.order.entity.dto.OrderExportDTO;
import cn.lili.modules.order.order.entity.dto.OrderMessage;
import cn.lili.modules.order.order.entity.dto.OrderSearchParams;
import cn.lili.modules.order.order.entity.enums.*;
import cn.lili.modules.order.order.entity.vo.OrderDetailVO;
import cn.lili.modules.order.order.entity.vo.OrderSimpleVO;
import cn.lili.modules.order.order.entity.vo.OrderVO;
import cn.lili.modules.order.order.entity.vo.PaymentLog;
import cn.lili.modules.order.order.mapper.OrderItemMapper;
import cn.lili.modules.order.order.mapper.OrderMapper;
import cn.lili.modules.order.order.service.*;
import cn.lili.modules.order.trade.entity.dos.OrderLog;
import cn.lili.modules.order.trade.service.OrderLogService;
import cn.lili.modules.payment.entity.enums.PaymentMethodEnum;
import cn.lili.modules.promotion.entity.dos.Pintuan;
import cn.lili.modules.promotion.service.PintuanService;
import cn.lili.modules.store.entity.dto.StoreDeliverGoodsAddressDTO;
import cn.lili.modules.store.service.StoreDetailService;
import cn.lili.modules.system.aspect.annotation.SystemLogPoint;
import cn.lili.modules.system.entity.dos.Logistics;
import cn.lili.modules.system.entity.dto.LogisticsSetting;
import cn.lili.modules.system.entity.vo.Traces;
import cn.lili.modules.system.service.LogisticsService;
import cn.lili.mybatis.util.PageUtil;
import cn.lili.rocketmq.RocketmqSendCallbackBuilder;
import cn.lili.rocketmq.tags.GoodsTagsEnum;
import cn.lili.rocketmq.tags.OrderTagsEnum;
import cn.lili.trigger.enums.DelayTypeEnums;
import cn.lili.trigger.interfaces.TimeTrigger;
import cn.lili.trigger.message.PintuanOrderMessage;
import cn.lili.trigger.model.TimeExecuteConstant;
import cn.lili.trigger.model.TimeTriggerMsg;
import cn.lili.trigger.util.DelayQueueTools;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 子Order业务层实现
 *
 * @author Chopper
 * @since 2020/11/17 7:38 下午
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final String ORDER_SN_COLUMN = "order_sn";

    /**
     * 延时任务
     */
    @Autowired
    private TimeTrigger timeTrigger;
    /**
     * Order货物数据层
     */
    @Resource
    private OrderItemMapper orderItemMapper;
    /**
     * 发票
     */
    @Autowired
    private ReceiptService receiptService;
    /**
     * Order货物
     */
    @Autowired
    private OrderItemService orderItemService;
    /**
     * logistics公司
     */
    @Autowired
    private LogisticsService logisticsService;
    /**
     * Order日志
     */
    @Autowired
    private OrderLogService orderLogService;
    /**
     * RocketMQ
     */
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    /**
     * RocketMQ配置
     */
    @Autowired
    private RocketmqCustomProperties rocketmqCustomProperties;
    /**
     * Order流水
     */
    @Autowired
    private StoreFlowService storeFlowService;
    /**
     * 拼团
     */
    @Autowired
    private PintuanService pintuanService;

    @Autowired
    private TradeService tradeService;


    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private StoreDetailService storeDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void intoDB(TradeDTO tradeDTO) {
        //检查TradeDTO信息
        checkTradeDTO(tradeDTO);
        //存放购物车，即业务中的Order
        List<Order> orders = new ArrayList<>(tradeDTO.getCartList().size());
        //存放自Order/Order日志
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderLog> orderLogs = new ArrayList<>();

        //Order集合
        List<OrderVO> orderVOS = new ArrayList<>();
        //循环购物车
        tradeDTO.getCartList().forEach(item -> {
            //当前购物车Order子项
            List<OrderItem> currentOrderItems = new ArrayList<>();
            Order order = new Order(item, tradeDTO);
            //构建orderVO对象
            OrderVO orderVO = new OrderVO();
            BeanUtil.copyProperties(order, orderVO);
            //持久化DO
            orders.add(order);
            String message = "Order[" + item.getSn() + "]create";
            //记录日志
            orderLogs.add(new OrderLog(item.getSn(), UserContext.getCurrentUser().getId(), UserContext.getCurrentUser().getRole().getRole(), UserContext.getCurrentUser().getUsername(), message));
            item.getCheckedSkuList().forEach(
                    sku -> {
                        orderItems.add(new OrderItem(sku, item, tradeDTO));
                        currentOrderItems.add(new OrderItem(sku, item, tradeDTO));
                    }
            );
            //写入子Order信息
            orderVO.setOrderItems(currentOrderItems);
            //orderVO 记录
            orderVOS.add(orderVO);
        });
        tradeDTO.setOrderVO(orderVOS);
        //批量保存Order
        this.saveBatch(orders);
        //批量保存 子Order
        orderItemService.saveBatch(orderItems);
        //批量记录Order操作日志
        orderLogService.saveBatch(orderLogs);
    }

    @Override
    public IPage<OrderSimpleVO> queryByParams(OrderSearchParams orderSearchParams) {
        QueryWrapper queryWrapper = orderSearchParams.queryWrapper();
        queryWrapper.groupBy("o.id");
        queryWrapper.orderByDesc("o.id");
        return this.baseMapper.queryByParams(PageUtil.initPage(orderSearchParams), queryWrapper);
    }

    /**
     * Order信息
     *
     * @param orderSearchParams 查询参数
     * @return Order信息
     */
    @Override
    public List<Order> queryListByParams(OrderSearchParams orderSearchParams) {
        return this.baseMapper.queryListByParams(orderSearchParams.queryWrapper());
    }

    /**
     * 根据促销查询Order
     *
     * @param orderPromotionType Order类型
     * @param payStatus          支付状态
     * @param parentOrderSn      依赖Order编号
     * @param orderSn            Order编号
     * @return Order信息
     */
    @Override
    public List<Order> queryListByPromotion(String orderPromotionType, String payStatus, String parentOrderSn, String orderSn) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        //查找团长Order和已和当前拼团Order拼团的Order
        queryWrapper.eq(Order::getOrderPromotionType, orderPromotionType)
                .eq(Order::getPayStatus, payStatus)
                .and(i -> i.eq(Order::getParentOrderSn, parentOrderSn).or(j -> j.eq(Order::getSn, orderSn)));
        return this.list(queryWrapper);
    }

    /**
     * 根据促销查询Order
     *
     * @param orderPromotionType Order类型
     * @param payStatus          支付状态
     * @param parentOrderSn      依赖Order编号
     * @param orderSn            Order编号
     * @return Order信息
     */
    @Override
    public long queryCountByPromotion(String orderPromotionType, String payStatus, String parentOrderSn, String orderSn) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        //查找团长Order和已和当前拼团Order拼团的Order
        queryWrapper.eq(Order::getOrderPromotionType, orderPromotionType)
                .eq(Order::getPayStatus, payStatus)
                .and(i -> i.eq(Order::getParentOrderSn, parentOrderSn).or(j -> j.eq(Order::getSn, orderSn)));
        return this.count(queryWrapper);
    }

    /**
     * 父级拼团Order
     *
     * @param pintuanId 拼团id
     * @return 拼团Order信息
     */
    @Override
    public List<Order> queryListByPromotion(String pintuanId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderPromotionType, PromotionTypeEnum.PINTUAN.name());
        queryWrapper.eq(Order::getPromotionId, pintuanId);
        queryWrapper.nested(i -> i.eq(Order::getPayStatus, PayStatusEnum.PAID.name()).or(j -> j.eq(Order::getOrderStatus, OrderStatusEnum.PAID.name())));
        return this.list(queryWrapper);
    }

    @Override
    public List<OrderExportDTO> queryExportOrder(OrderSearchParams orderSearchParams) {
        return this.baseMapper.queryExportOrder(orderSearchParams.queryWrapper());
    }

    @Override
    public OrderDetailVO queryDetail(String orderSn) {
        Order order = this.getBySn(orderSn);
        if (order == null) {
            throw new ServiceException(ResultCode.ORDER_NOT_EXIST);
        }
        QueryWrapper<OrderItem> orderItemWrapper = new QueryWrapper<>();
        orderItemWrapper.eq(ORDER_SN_COLUMN, orderSn);
        //查询Order项信息
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemWrapper);
        //查询Order日志信息
        List<OrderLog> orderLogs = orderLogService.getOrderLog(orderSn);
        //查询发票信息
        Receipt receipt = receiptService.getByOrderSn(orderSn);
        //查询Order和自Order，然后写入vo返回
        return new OrderDetailVO(order, orderItems, orderLogs, receipt);
    }

    @Override
    @OrderLogPoint(description = "'Order['+#orderSn+'] cancel, reason：'+#reason", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public Order cancel(String orderSn, String reason) {
        Order order = OperationalJudgment.judgment(this.getBySn(orderSn));
        //如果Order促销类型不为空&&Order是拼团Order，并且Order未成团，则抛出异常
        if (OrderPromotionTypeEnum.PINTUAN.name().equals(order.getOrderPromotionType())
                && !CharSequenceUtil.equalsAny(order.getOrderStatus(), OrderStatusEnum.UNDELIVERED.name(), OrderStatusEnum.STAY_PICKED_UP.name())) {
            throw new ServiceException(ResultCode.ORDER_CAN_NOT_CANCEL);
        }
        if (CharSequenceUtil.equalsAny(order.getOrderStatus(),
                OrderStatusEnum.UNDELIVERED.name(),
                OrderStatusEnum.UNPAID.name(),
                OrderStatusEnum.STAY_PICKED_UP.name(),
                OrderStatusEnum.PAID.name())) {

            order.setOrderStatus(OrderStatusEnum.CANCELLED.name());
            order.setCancelReason(reason);
            //修改Order
            this.updateById(order);
            //生成店铺退款流水
            this.generatorStoreRefundFlow(order);
            orderStatusMessage(order);
            return order;
        } else {
            throw new ServiceException(ResultCode.ORDER_CAN_NOT_CANCEL);
        }
    }


    @Override
    @OrderLogPoint(description = "'Order['+#orderSn+']系统取消，原因为：'+#reason", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public void systemCancel(String orderSn, String reason) {
        Order order = this.getBySn(orderSn);
        order.setOrderStatus(OrderStatusEnum.CANCELLED.name());
        order.setCancelReason(reason);
        this.updateById(order);
        //生成店铺退款流水
        this.generatorStoreRefundFlow(order);
        orderStatusMessage(order);
    }

    /**
     * 获取Order
     *
     * @param orderSn Order编号
     * @return Order详情
     */
    @Override
    public Order getBySn(String orderSn) {
        return this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getSn, orderSn));
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderSn, String paymentMethod, String receivableNo) {

        Order order = this.getBySn(orderSn);
        //如果Order已支付，就不能再次进行支付
        if (order.getPayStatus().equals(PayStatusEnum.PAID.name())) {
            log.error("Order[ {} ]检测到重复付款，请处理", orderSn);
            throw new ServiceException(ResultCode.PAY_DOUBLE_ERROR);
        }

        //修改Order状态
        order.setPaymentTime(new Date());
        order.setPaymentMethod(paymentMethod);
        order.setPayStatus(PayStatusEnum.PAID.name());
        order.setOrderStatus(OrderStatusEnum.PAID.name());
        order.setReceivableNo(receivableNo);
        order.setCanReturn(!PaymentMethodEnum.BANK_TRANSFER.name().equals(order.getPaymentMethod()));
        this.updateById(order);

        //记录店铺Order支付流水
        storeFlowService.payOrder(orderSn);

        //发送Order已付款消息
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderSn(order.getSn());
        orderMessage.setPaymentMethod(paymentMethod);
        orderMessage.setNewStatus(OrderStatusEnum.PAID);
        this.sendUpdateStatusMessage(orderMessage);

        String message = "Order付款，付款方式[" + PaymentMethodEnum.valueOf(paymentMethod).paymentName() + "]";
        OrderLog orderLog = new OrderLog(orderSn, "-1", UserEnums.SYSTEM.getRole(), "系统操作", message);
        orderLogService.save(orderLog);

    }

    @Override
    @OrderLogPoint(description = "'库存确认'", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public void afterOrderConfirm(String orderSn) {
        Order order = this.getBySn(orderSn);
        //判断是否为拼团Order，进行特殊处理
        //判断Order类型进行不同的Order确认操作
        if (OrderPromotionTypeEnum.PINTUAN.name().equals(order.getOrderPromotionType())) {
            String parentOrderSn = CharSequenceUtil.isEmpty(order.getParentOrderSn()) ? orderSn : order.getParentOrderSn();
            this.checkPintuanOrder(order.getPromotionId(), parentOrderSn);
        } else {
            //判断Order类型
            if (order.getOrderType().equals(OrderTypeEnum.NORMAL.name())) {
                normalOrderConfirm(orderSn);
            } else {
                virtualOrderConfirm(orderSn);
            }
        }
    }


    @Override
    @SystemLogPoint(description = "修改Order", customerLog = "'Order[' + #orderSn + ']收货信息修改，修改为'+#memberAddressDTO.consigneeDetail+'")
    @Transactional(rollbackFor = Exception.class)
    public Order updateConsignee(String orderSn, MemberAddressDTO memberAddressDTO) {
        Order order = OperationalJudgment.judgment(this.getBySn(orderSn));

        //要记录之前的收货地址，所以需要以代码方式进行调用 不采用注解
        String message = "Order[" + orderSn + "]收货信息修改，由[" + order.getConsigneeDetail() + "]修改为[" + memberAddressDTO.getConsigneeDetail() + "]";
        //记录Order操作日志
        BeanUtil.copyProperties(memberAddressDTO, order);
        this.updateById(order);

        OrderLog orderLog = new OrderLog(orderSn, UserContext.getCurrentUser().getId(), UserContext.getCurrentUser().getRole().getRole(), UserContext.getCurrentUser().getUsername(), message);
        orderLogService.save(orderLog);

        return order;
    }

    @Override
    @OrderLogPoint(description = "'Order['+#orderSn+']发货，发货单号['+#logisticsNo+']'", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public Order delivery(String orderSn, String logisticsNo, String logisticsId) {
        Order order = OperationalJudgment.judgment(this.getBySn(orderSn));
        //如果Orderunshipped，并且Order状态值等于待发货
        if (order.getDeliverStatus().equals(DeliverStatusEnum.UNDELIVERED.name()) && order.getOrderStatus().equals(OrderStatusEnum.UNDELIVERED.name())) {
            //获取对应logistics
            Logistics logistics = logisticsService.getById(logisticsId);
            if (logistics == null) {
                throw new ServiceException(ResultCode.ORDER_LOGISTICS_ERROR);
            }
            //写入logistics信息
            order.setLogisticsCode(logistics.getId());
            order.setLogisticsName(logistics.getName());
            order.setLogisticsNo(logisticsNo);
            order.setLogisticsTime(new Date());
            order.setDeliverStatus(DeliverStatusEnum.DELIVERED.name());
            this.updateById(order);
            //修改Order状态为已发送
            this.updateStatus(orderSn, OrderStatusEnum.DELIVERED);
            //修改Order货物可以进行售后、投诉
            orderItemService.update(new UpdateWrapper<OrderItem>().eq(ORDER_SN_COLUMN, orderSn)
                    .set("after_sale_status", OrderItemAfterSaleStatusEnum.NOT_APPLIED)
                    .set("complain_status", OrderComplaintStatusEnum.NO_APPLY));
            //发送Order状态改变消息
            OrderMessage orderMessage = new OrderMessage();
            orderMessage.setNewStatus(OrderStatusEnum.DELIVERED);
            orderMessage.setOrderSn(order.getSn());
            this.sendUpdateStatusMessage(orderMessage);
        } else {
            throw new ServiceException(ResultCode.ORDER_DELIVER_ERROR);
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order shunFengDelivery(String orderSn) {
        OrderDetailVO orderDetailVO = this.queryDetail(orderSn);
        String logisticsNo = logisticsService.sfCreateOrder(orderDetailVO);
        Logistics logistics = logisticsService.getOne(new LambdaQueryWrapper<Logistics>().eq(Logistics::getCode,"SF"));
        return delivery(orderSn,logisticsNo,logistics.getId());
    }

    @Override
    public Traces getTraces(String orderSn) {
        //获取Order信息
        Order order = this.getBySn(orderSn);
        //获取踪迹信息
        return logisticsService.getLogisticTrack(order.getLogisticsCode(), order.getLogisticsNo(), order.getConsigneeMobile());
    }

    @Override
    public Traces getMapTraces(String orderSn) {
        //获取Order信息
        Order order = this.getBySn(orderSn);
        //获取店家信息
        StoreDeliverGoodsAddressDTO storeDeliverGoodsAddressDTO = storeDetailService.getStoreDeliverGoodsAddressDto(order.getStoreId());
        String from = storeDeliverGoodsAddressDTO.getSalesConsignorAddressPath().substring(0, storeDeliverGoodsAddressDTO.getSalesConsignorAddressPath().indexOf(",") - 1);
        String to = order.getConsigneeAddressPath().substring(0, order.getConsigneeAddressPath().indexOf(",") - 1);
        //获取踪迹信息
        return logisticsService.getLogisticMapTrack(order.getLogisticsCode(), order.getLogisticsNo(), order.getConsigneeMobile(), from, to);
    }

    @Override
    @OrderLogPoint(description = "'Order['+#orderSn+']核销，核销码['+#verificationCode+']'", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public Order take(String orderSn, String verificationCode) {

        //获取Order信息
        Order order = this.getBySn(orderSn);
        //检测虚拟Order信息
        checkVerificationOrder(order, verificationCode);
        order.setOrderStatus(OrderStatusEnum.COMPLETED.name());
        //Order完成
        this.complete(orderSn);
        return order;
    }

    @Override
    public Order take(String verificationCode) {
        String storeId = OperationalJudgment.judgment(UserContext.getCurrentUser()).getStoreId();
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getVerificationCode, verificationCode).eq(Order::getStoreId, storeId));
        if (order == null) {
            throw new ServiceException(ResultCode.ORDER_NOT_EXIST);
        }
        order.setOrderStatus(OrderStatusEnum.COMPLETED.name());
        //Order完成
        this.complete(order.getSn());
        return order;
    }

    @Override
    public Order getOrderByVerificationCode(String verificationCode) {
        String storeId = Objects.requireNonNull(UserContext.getCurrentUser()).getStoreId();
        return this.getOne(new LambdaQueryWrapper<Order>()
                .in(Order::getOrderStatus, OrderStatusEnum.TAKE.name(), OrderStatusEnum.STAY_PICKED_UP.name())
                .eq(Order::getStoreId, storeId)
                .eq(Order::getVerificationCode, verificationCode));
    }

    @Override
    @OrderLogPoint(description = "'Order['+#orderSn+']完成'", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public void complete(String orderSn) {
        //是否可以查询到Order
        Order order = OperationalJudgment.judgment(this.getBySn(orderSn));
        complete(order, orderSn);
    }

    @Override
    @OrderLogPoint(description = "'Order['+#orderSn+']完成'", orderSn = "#orderSn")
    @Transactional(rollbackFor = Exception.class)
    public void systemComplete(String orderSn) {
        Order order = this.getBySn(orderSn);
        complete(order, orderSn);
    }

    /**
     * 完成Order方法封装
     *
     * @param order   Order
     * @param orderSn Order编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Order order, String orderSn) {//修改Order状态为完成
        this.updateStatus(orderSn, OrderStatusEnum.COMPLETED);

        //修改Order货物可以进行评价
        orderItemService.update(new UpdateWrapper<OrderItem>().eq(ORDER_SN_COLUMN, orderSn)
                .set("comment_status", CommentStatusEnum.UNFINISHED));
        this.update(new LambdaUpdateWrapper<Order>().eq(Order::getSn, orderSn).set(Order::getCompleteTime, new Date()));
        //发送Order状态改变消息
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setNewStatus(OrderStatusEnum.COMPLETED);
        orderMessage.setOrderSn(order.getSn());
        this.sendUpdateStatusMessage(orderMessage);

        //发送当前商品购买完成的信息（用于更新商品数据）
        List<OrderItem> orderItems = orderItemService.getByOrderSn(orderSn);
        List<GoodsCompleteMessage> goodsCompleteMessageList = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            GoodsCompleteMessage goodsCompleteMessage = new GoodsCompleteMessage();
            goodsCompleteMessage.setGoodsId(orderItem.getGoodsId());
            goodsCompleteMessage.setSkuId(orderItem.getSkuId());
            goodsCompleteMessage.setBuyNum(orderItem.getNum());
            goodsCompleteMessage.setMemberId(order.getMemberId());
            goodsCompleteMessageList.add(goodsCompleteMessage);
        }
        //发送商品购买消息
        if (!goodsCompleteMessageList.isEmpty()) {
            String destination = rocketmqCustomProperties.getGoodsTopic() + ":" + GoodsTagsEnum.BUY_GOODS_COMPLETE.name();
            //发送Order变更mq消息
            rocketMQTemplate.asyncSend(destination, JSONUtil.toJsonStr(goodsCompleteMessageList), RocketmqSendCallbackBuilder.commonCallback());
        }
    }

    @Override
    public List<Order> getByTradeSn(String tradeSn) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        return this.list(queryWrapper.eq(Order::getTradeSn, tradeSn));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendUpdateStatusMessage(OrderMessage orderMessage) {
        applicationEventPublisher.publishEvent(new TransactionCommitSendMQEvent("发送Order变更mq消息", rocketmqCustomProperties.getOrderTopic(), OrderTagsEnum.STATUS_CHANGE.name(), JSONUtil.toJsonStr(orderMessage)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(String sn) {
        Order order = this.getBySn(sn);
        if (order == null) {
            log.error("Order号为" + sn + "的Order不存在！");
            throw new ServiceException();
        }
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getSn, sn).set(Order::getDeleteFlag, true);
        this.update(updateWrapper);
        LambdaUpdateWrapper<OrderItem> orderItemLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        orderItemLambdaUpdateWrapper.eq(OrderItem::getOrderSn, sn).set(OrderItem::getDeleteFlag, true);
        this.orderItemService.update(orderItemLambdaUpdateWrapper);
    }

    @Override
    public Boolean invoice(String sn) {
        //根据Order号查询发票信息
        Receipt receipt = receiptService.getByOrderSn(sn);
        //校验发票信息是否存在
        if (receipt != null) {
            receipt.setReceiptStatus(1);
            return receiptService.updateById(receipt);
        }
        throw new ServiceException(ResultCode.USER_RECEIPT_NOT_EXIST);
    }

    /**
     * 自动成团Order处理
     *
     * @param pintuanId     拼团活动id
     * @param parentOrderSn 拼团Ordersn
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agglomeratePintuanOrder(String pintuanId, String parentOrderSn) {
        //获取拼团配置
        Pintuan pintuan = pintuanService.getById(pintuanId);
        List<Order> list = this.getPintuanOrder(pintuanId, parentOrderSn);
        if (Boolean.TRUE.equals(pintuan.getFictitious()) && pintuan.getRequiredNum() > list.size()) {
            //如果开启虚拟成团且当前Order数量不足成团数量，则认为拼团成功
            this.pintuanOrderSuccess(list);
        } else if (Boolean.FALSE.equals(pintuan.getFictitious()) && pintuan.getRequiredNum() > list.size()) {
            //如果未开启虚拟成团且当前Order数量不足成团数量，则认为拼团失败
            this.pintuanOrderFailed(list);
        }
    }

    @Override
    public void getBatchDeliverList(HttpServletResponse response, List<String> logisticsName) {
        ExcelWriter writer = ExcelUtil.getWriter();
        //Excel 头部
        ArrayList<String> rows = new ArrayList<>();
        rows.add("Order编号");
        rows.add("logistics公司");
        rows.add("logistics编号");
        writer.writeHeadRow(rows);

        //存放下拉列表  ----店铺已选择logistics公司列表
        String[] logiList = logisticsName.toArray(new String[]{});
        CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(1, 200, 1, 1);
        writer.addSelect(cellRangeAddressList, logiList);

        ServletOutputStream out = null;
        try {
            //设置公共属性，列表名称
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("批量发货导入模板", "UTF8") + ".xls");
            out = response.getOutputStream();
            writer.flush(out, true);
        } catch (Exception e) {
            log.error("获取待发货Order编号列表错误", e);
        } finally {
            writer.close();
            IoUtil.close(out);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeliver(MultipartFile files) {

        InputStream inputStream;
        List<OrderBatchDeliverDTO> orderBatchDeliverDTOList = new ArrayList<>();
        try {
            inputStream = files.getInputStream();
            //2.应用HUtool ExcelUtil获取ExcelReader指定输入流和sheet
            ExcelReader excelReader = ExcelUtil.getReader(inputStream);
            //可以加上表头验证
            //3.读取第二行到最后一行数据
            List<List<Object>> read = excelReader.read(1, excelReader.getRowCount());
            for (List<Object> objects : read) {
                OrderBatchDeliverDTO orderBatchDeliverDTO = new OrderBatchDeliverDTO();
                orderBatchDeliverDTO.setOrderSn(objects.get(0).toString());
                orderBatchDeliverDTO.setLogisticsName(objects.get(1).toString());
                orderBatchDeliverDTO.setLogisticsNo(objects.get(2).toString());
                orderBatchDeliverDTOList.add(orderBatchDeliverDTO);
            }
        } catch (Exception e) {
            throw new ServiceException(ResultCode.ORDER_BATCH_DELIVER_ERROR);
        }
        //循环检查是否符合规范
        checkBatchDeliver(orderBatchDeliverDTOList);
        //Order批量发货
        for (OrderBatchDeliverDTO orderBatchDeliverDTO : orderBatchDeliverDTOList) {
            this.delivery(orderBatchDeliverDTO.getOrderSn(), orderBatchDeliverDTO.getLogisticsNo(), orderBatchDeliverDTO.getLogisticsId());
        }
    }


    @Override
    public Double getPaymentTotal(String orderSn) {
        Order order = this.getBySn(orderSn);
        Trade trade = tradeService.getBySn(order.getTradeSn());
        //如果交易不为空，则返回交易的金额，否则返回Order金额
        if (CharSequenceUtil.isNotEmpty(trade.getPayStatus())
                && trade.getPayStatus().equals(PayStatusEnum.PAID.name())) {
            return trade.getFlowPrice();
        }
        return order.getFlowPrice();
    }

    @Override
    public IPage<PaymentLog> queryPaymentLogs(IPage<PaymentLog> page, Wrapper<PaymentLog> queryWrapper) {
        return baseMapper.queryPaymentLogs(page, queryWrapper);
    }

    /**
     * 循环检查批量发货Order列表
     *
     * @param list 待发货Order列表
     */
    private void checkBatchDeliver(List<OrderBatchDeliverDTO> list) {

        List<Logistics> logistics = logisticsService.list();
        for (OrderBatchDeliverDTO orderBatchDeliverDTO : list) {
            //查看Order号是否存在-是否是当前店铺的Order
            Order order = this.getOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getStoreId, UserContext.getCurrentUser().getStoreId())
                    .eq(Order::getSn, orderBatchDeliverDTO.getOrderSn()));
            if (order == null) {
                throw new ServiceException("Order编号：'" + orderBatchDeliverDTO.getOrderSn() + " '不存在");
            } else if (!order.getOrderStatus().equals(OrderStatusEnum.UNDELIVERED.name())) {
                throw new ServiceException("Order编号：'" + orderBatchDeliverDTO.getOrderSn() + " '不能发货");
            }
            //获取logistics公司
            logistics.forEach(item -> {
                if (item.getName().equals(orderBatchDeliverDTO.getLogisticsName())) {
                    orderBatchDeliverDTO.setLogisticsId(item.getId());
                }
            });
            if (CharSequenceUtil.isEmpty(orderBatchDeliverDTO.getLogisticsId())) {
                throw new ServiceException("logistics公司：'" + orderBatchDeliverDTO.getLogisticsName() + " '不存在");
            }
        }


    }

    /**
     * 检查是否开始虚拟成团
     *
     * @param pintuanId   拼团活动id
     * @param requiredNum 成团人数
     * @param fictitious  是否开启成团
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkFictitiousOrder(String pintuanId, Integer requiredNum, Boolean fictitious) {
        Map<String, List<Order>> collect = this.queryListByPromotion(pintuanId)
                .stream().collect(Collectors.groupingBy(Order::getParentOrderSn));

        for (Map.Entry<String, List<Order>> entry : collect.entrySet()) {
            //是否开启虚拟成团
            if (Boolean.FALSE.equals(fictitious) && CharSequenceUtil.isNotEmpty(entry.getKey()) && entry.getValue().size() < requiredNum) {
                //如果未开启虚拟成团且已参团人数小于成团人数，则自动取消Order
                String reason = "拼团活动结束Order未付款，系统自动取消Order";
                if (CharSequenceUtil.isNotEmpty(entry.getKey())) {
                    this.systemCancel(entry.getKey(), reason);
                } else {
                    for (Order order : entry.getValue()) {
                        this.systemCancel(order.getSn(), reason);
                    }
                }
            } else if (Boolean.TRUE.equals(fictitious)) {
                this.fictitiousPintuan(entry, requiredNum);
            }
        }
        return false;
    }

    /**
     * 虚拟成团
     *
     * @param entry       Order列表
     * @param requiredNum 必须参团人数
     */
    private void fictitiousPintuan(Map.Entry<String, List<Order>> entry, Integer requiredNum) {
        Map<String, List<Order>> listMap = entry.getValue().stream().collect(Collectors.groupingBy(Order::getPayStatus));
        //未付款Order
        List<Order> unpaidOrders = listMap.get(PayStatusEnum.UNPAID.name());
        //未付款Order自动取消
        if (unpaidOrders != null && !unpaidOrders.isEmpty()) {
            for (Order unpaidOrder : unpaidOrders) {
                this.systemCancel(unpaidOrder.getSn(), "拼团活动结束Order未付款，系统自动取消Order");
            }
        }
        List<Order> paidOrders = listMap.get(PayStatusEnum.PAID.name());
        //如待参团人数大于0，并已开启虚拟成团
        if (!paidOrders.isEmpty()) {
            //待参团人数
            int waitNum = requiredNum - paidOrders.size();
            //添加虚拟成团
            for (int i = 0; i < waitNum; i++) {
                Order order = new Order();
                BeanUtil.copyProperties(paidOrders.get(0), order, "id", "sn");
                order.setSn(SnowFlake.createStr("G"));
                order.setParentOrderSn(paidOrders.get(0).getParentOrderSn());
                order.setMemberId("-1");
                order.setMemberName("参团人员");
                order.setDeleteFlag(true);
                this.save(order);
                paidOrders.add(order);
            }
            for (Order paidOrder : paidOrders) {
                paidOrder.setOrderStatus(OrderStatusEnum.UNDELIVERED.name());
                this.updateById(paidOrder);
                orderStatusMessage(paidOrder);
            }
        }
    }

    /**
     * Order状态变更消息
     *
     * @param order Order信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void orderStatusMessage(Order order) {
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderSn(order.getSn());
        orderMessage.setNewStatus(OrderStatusEnum.valueOf(order.getOrderStatus()));
        this.sendUpdateStatusMessage(orderMessage);
    }

    /**
     * 生成店铺退款流水
     *
     * @param order Order信息
     */
    private void generatorStoreRefundFlow(Order order) {
        // 判断Order是否是付款
        if (!PayStatusEnum.PAID.name().equals((order.getPayStatus()))) {
            return;
        }
        List<OrderItem> items = orderItemService.getByOrderSn(order.getSn());
        List<StoreFlow> storeFlows = new ArrayList<>();
        for (OrderItem item : items) {
            StoreFlow storeFlow = new StoreFlow(order, item, FlowTypeEnum.REFUND);
            storeFlows.add(storeFlow);
        }
        storeFlowService.saveBatch(storeFlows);
    }

    /**
     * 此方法只提供内部调用，调用前应该做好权限处理
     * 修改Order状态
     *
     * @param orderSn     Order编号
     * @param orderStatus Order状态
     */
    private void updateStatus(String orderSn, OrderStatusEnum orderStatus) {
        this.baseMapper.updateStatus(orderStatus.name(), orderSn);
    }

    /**
     * 检测拼团Order内容
     * 此方法用与Order确认
     * 判断拼团是否达到人数进行下一步处理
     *
     * @param pintuanId     拼团活动ID
     * @param parentOrderSn 拼团父Order编号
     */
    private void checkPintuanOrder(String pintuanId, String parentOrderSn) {
        //获取拼团配置
        Pintuan pintuan = pintuanService.getById(pintuanId);
        List<Order> list = this.getPintuanOrder(pintuanId, parentOrderSn);
        int count = list.size();
        if (count == 1) {
            //如果为开团Order，则发布一个24小时的延时任务，时间到达后，如果未成团则自动结束（未开启虚拟成团的情况下）
            PintuanOrderMessage pintuanOrderMessage = new PintuanOrderMessage();
            //开团结束时间
            long startTime = DateUtil.offsetHour(new Date(), 24).getTime();
            pintuanOrderMessage.setOrderSn(parentOrderSn);
            pintuanOrderMessage.setPintuanId(pintuanId);
            TimeTriggerMsg timeTriggerMsg = new TimeTriggerMsg(TimeExecuteConstant.PROMOTION_EXECUTOR,
                    startTime,
                    pintuanOrderMessage,
                    DelayQueueTools.wrapperUniqueKey(DelayTypeEnums.PINTUAN_ORDER, (pintuanId + parentOrderSn)),
                    rocketmqCustomProperties.getPromotionTopic());

            this.timeTrigger.addDelay(timeTriggerMsg);
        }
        //拼团所需人数，小于等于 参团后的人数，则说明成团，所有Order成团
        if (pintuan.getRequiredNum() <= count) {
            this.pintuanOrderSuccess(list);
        }
    }

    /**
     * 根据拼团活动id和拼团Ordersn获取所有当前与当前拼团Ordersn相关的Order
     *
     * @param pintuanId     拼团活动id
     * @param parentOrderSn 拼团Ordersn
     * @return 所有当前与当前拼团Ordersn相关的Order
     */
    private List<Order> getPintuanOrder(String pintuanId, String parentOrderSn) {
        //寻找拼团的所有Order
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getPromotionId, pintuanId)
                .eq(Order::getOrderPromotionType, OrderPromotionTypeEnum.PINTUAN.name())
                .eq(Order::getPayStatus, PayStatusEnum.PAID.name());
        //拼团sn=开团Ordersn 或者 参团Order的开团Ordersn
        queryWrapper.and(i -> i.eq(Order::getSn, parentOrderSn)
                .or(j -> j.eq(Order::getParentOrderSn, parentOrderSn)));
        //参团后的Order数（人数）
        return this.list(queryWrapper);
    }

    /**
     * 根据提供的拼团Order列表更新拼团状态为拼团成功
     * 循环Order列表根据不同的Order类型进行确认Order
     *
     * @param orderList 需要更新拼团状态为成功的拼团Order列表
     */
    private void pintuanOrderSuccess(List<Order> orderList) {
        for (Order order : orderList) {
            if (order.getOrderType().equals(OrderTypeEnum.VIRTUAL.name())) {
                this.virtualOrderConfirm(order.getSn());
            } else if (order.getOrderType().equals(OrderTypeEnum.NORMAL.name())) {
                this.normalOrderConfirm(order.getSn());
            }
        }
    }

    /**
     * 根据提供的拼团Order列表更新拼团状态为拼团失败
     *
     * @param list 需要更新拼团状态为失败的拼团Order列表
     */
    private void pintuanOrderFailed(List<Order> list) {
        for (Order order : list) {
            try {
                this.systemCancel(order.getSn(), "拼团人数不足，拼团失败！");
            } catch (Exception e) {
                log.error("拼团Order取消失败", e);
            }
        }
    }


    /**
     * 检查交易信息
     *
     * @param tradeDTO 交易DTO
     */
    private void checkTradeDTO(TradeDTO tradeDTO) {
        //检测是否为拼团Order
        if (tradeDTO.getParentOrderSn() != null) {
            //判断用户不能参与自己发起的拼团活动
            Order parentOrder = this.getBySn(tradeDTO.getParentOrderSn());
            if (parentOrder.getMemberId().equals(UserContext.getCurrentUser().getId())) {
                throw new ServiceException(ResultCode.PINTUAN_JOIN_ERROR);
            }
        }
    }

    /**
     * 普通商品Order确认
     * 修改Order状态为待发货
     * 发送Order状态变更消息
     *
     * @param orderSn Order编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void normalOrderConfirm(String orderSn) {
        OrderStatusEnum orderStatusEnum = null;
        Order order = this.getBySn(orderSn);
        if (DeliveryMethodEnum.SELF_PICK_UP.name().equals(order.getDeliveryMethod())) {
            orderStatusEnum = OrderStatusEnum.STAY_PICKED_UP;
        } else if (DeliveryMethodEnum.LOGISTICS.name().equals(order.getDeliveryMethod())) {
            orderStatusEnum = OrderStatusEnum.UNDELIVERED;
        }
        //修改Order
        this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getSn, orderSn)
                .set(Order::getOrderStatus, orderStatusEnum.name()));
        //修改Order
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setNewStatus(orderStatusEnum);
        orderMessage.setOrderSn(orderSn);
        this.sendUpdateStatusMessage(orderMessage);
    }

    /**
     * 虚拟商品Order确认
     * 修改Order状态为待核验
     * 发送Order状态变更消息
     *
     * @param orderSn Order编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void virtualOrderConfirm(String orderSn) {
        //修改Order
        this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getSn, orderSn)
                .set(Order::getOrderStatus, OrderStatusEnum.TAKE.name()));
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setNewStatus(OrderStatusEnum.TAKE);
        orderMessage.setOrderSn(orderSn);
        this.sendUpdateStatusMessage(orderMessage);
    }

    /**
     * 检测虚拟Order信息
     *
     * @param order            Order
     * @param verificationCode 验证码
     */
    private void checkVerificationOrder(Order order, String verificationCode) {
        //判断查询是否可以查询到Order
        if (order == null) {
            throw new ServiceException(ResultCode.ORDER_NOT_EXIST);
        }
        //判断是否为虚拟Order 或 自提Order
        if (!order.getOrderType().equals(OrderTypeEnum.VIRTUAL.name()) && !order.getDeliveryMethod().equals(DeliveryMethodEnum.SELF_PICK_UP.name())) {
            throw new ServiceException(ResultCode.ORDER_TAKE_ERROR);
        }
        //判断虚拟Order状态 或 待自提
        if (!order.getOrderStatus().equals(OrderStatusEnum.TAKE.name()) && !order.getOrderStatus().equals(OrderStatusEnum.STAY_PICKED_UP.name())) {
            throw new ServiceException(ResultCode.ORDER_TAKE_ERROR);
        }
        //判断验证码是否正确
        if (!verificationCode.equals(order.getVerificationCode())) {
            throw new ServiceException(ResultCode.ORDER_TAKE_ERROR);
        }
    }
}