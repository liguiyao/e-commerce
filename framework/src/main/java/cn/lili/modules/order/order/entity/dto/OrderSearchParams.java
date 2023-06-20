package cn.lili.modules.order.order.entity.dto;

import cn.hutool.core.text.CharSequenceUtil;
import cn.lili.common.security.AuthUser;
import cn.lili.common.security.context.UserContext;
import cn.lili.common.security.enums.UserEnums;
import cn.lili.common.utils.DateUtil;
import cn.lili.common.vo.PageVO;
import cn.lili.modules.order.order.entity.enums.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Order查询参数
 *
 * @author Chopper
 * @since 2020/11/17 4:33 下午
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderSearchParams extends PageVO {

    private static final long serialVersionUID = -6380573339089959194L;

    @ApiModelProperty(value = "商品名称")
    private String goodsName;

    @ApiModelProperty(value = "Order编号")
    private String orderSn;

    @ApiModelProperty(value = "页面标签",
            example = "ALL:全部," +
                    "WAIT_PAY:Unpaid," +
                    "WAIT_ROG:待收货," +
                    "CANCELLED:Cancelled," +
                    "COMPLETE:Complete")
    private String tag;

    @ApiModelProperty(value = "商家ID")
    private String storeId;

    @ApiModelProperty(value = "会员ID")
    private String memberId;

    @ApiModelProperty(value = "收货人")
    private String shipName;

    @ApiModelProperty(value = "买家昵称")
    private String buyerName;

    @ApiModelProperty(value = "Order状态")
    private String orderStatus;

    @ApiModelProperty(value = "付款状态")
    private String payStatus;

    @ApiModelProperty(value = "关键字 商品名称/买家名称/店铺名称")
    private String keywords;

    @ApiModelProperty(value = "付款方式")
    private String paymentType;

    /**
     * @see OrderTypeEnum
     * @see cn.lili.modules.order.order.entity.enums.OrderPromotionTypeEnum
     */
    @ApiModelProperty(value = "Order类型", allowableValues = "NORMAL,VIRTUAL,GIFT,PINTUAN,POINT")
    private String orderType;

    @ApiModelProperty(value = "支付方式")
    private String paymentMethod;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "支付时间")
    private Date paymentTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "下单开始时间")
    private Date startDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "下单结束时间")
    private Date endDate;

    @ApiModelProperty(value = "Order来源")
    private String clientType;

    /**
     * @see CommentStatusEnum
     */
    @ApiModelProperty(value = "评论状态:未评论(UNFINISHED),待追评(WAIT_CHASE),评论完成(FINISHED)，")
    private String commentStatus;

    @ApiModelProperty(value = "是否为其他Order下的Order，如果是则为依赖Order的sn，否则为空")
    private String parentOrderSn;

    @ApiModelProperty(value = "是否为某Order类型的Order，如果是则为Order类型的id，否则为空")
    private String promotionId;

    @ApiModelProperty(value = "总价格,可以为范围，如10_1000")
    private String flowPrice;

    /**
     * @see OrderPromotionTypeEnum
     */
    @ApiModelProperty(value = "Order促销类型")
    private String orderPromotionType;

    public <T> QueryWrapper<T> queryWrapper() {
        AuthUser currentUser = UserContext.getCurrentUser();
        QueryWrapper<T> wrapper = new QueryWrapper<>();

        //关键字查询
        if (CharSequenceUtil.isNotEmpty(keywords)) {
            wrapper.and(keyWrapper -> keyWrapper.like("o.sn", keywords).or().like("oi.goods_name", keywords));
        }
        if (currentUser != null) {
            //按卖家查询
            wrapper.eq(CharSequenceUtil.equals(currentUser.getRole().name(), UserEnums.STORE.name()), "o.store_id", currentUser.getStoreId());

            //店铺查询
            wrapper.eq(CharSequenceUtil.equals(currentUser.getRole().name(), UserEnums.MANAGER.name())
                    && CharSequenceUtil.isNotEmpty(storeId), "o.store_id", storeId);

            //按买家查询
            wrapper.eq(CharSequenceUtil.equals(currentUser.getRole().name(), UserEnums.MEMBER.name()) && memberId == null, "o.member_id", currentUser.getId());

        }
        //按照买家查询
        wrapper.like(CharSequenceUtil.isNotEmpty(memberId), "o.member_id", memberId);

        //按Order编号查询
        wrapper.like(CharSequenceUtil.isNotEmpty(orderSn), "o.sn", orderSn);

        //按时间查询
        wrapper.ge(startDate != null, "o.create_time", startDate);

        wrapper.le(endDate != null, "o.create_time", DateUtil.endOfDate(endDate));
        //按购买人用户名
        wrapper.like(CharSequenceUtil.isNotEmpty(buyerName), "o.member_name", buyerName);

        //按Order类型
        wrapper.eq(CharSequenceUtil.isNotEmpty(orderType), "o.order_type", orderType);

        //logistics查询
        wrapper.like(CharSequenceUtil.isNotEmpty(shipName), "o.consignee_name", shipName);

        //按商品名称查询
        wrapper.like(CharSequenceUtil.isNotEmpty(goodsName), "oi.goods_name", goodsName);

        //付款方式
        wrapper.like(CharSequenceUtil.isNotEmpty(paymentType), "o.payment_type", paymentType);

        //按支付方式
        wrapper.eq(CharSequenceUtil.isNotEmpty(paymentMethod), "o.payment_method", paymentMethod);

        //Order状态
        wrapper.eq(CharSequenceUtil.isNotEmpty(orderStatus), "o.order_status", orderStatus);

        //付款状态
        wrapper.eq(CharSequenceUtil.isNotEmpty(payStatus), "o.pay_status", payStatus);

        //Order来源
        wrapper.like(CharSequenceUtil.isNotEmpty(clientType), "o.client_type", clientType);

        //按评价状态
        wrapper.eq(CharSequenceUtil.isNotEmpty(commentStatus), "oi.comment_status", commentStatus);

        //按标签查询
        if (CharSequenceUtil.isNotEmpty(tag)) {
            String orderStatusColumn = "o.order_status";
            OrderTagEnum tagEnum = OrderTagEnum.valueOf(tag);
            switch (tagEnum) {
                //Unpaid
                case WAIT_PAY:
                    wrapper.eq(orderStatusColumn, OrderStatusEnum.UNPAID.name());
                    break;
                //待发货
                case WAIT_SHIP:
                    wrapper.eq(orderStatusColumn, OrderStatusEnum.UNDELIVERED.name());
                    break;
                //待收货
                case WAIT_ROG:
                    wrapper.eq(orderStatusColumn, OrderStatusEnum.DELIVERED.name());
                    break;
                //Cancelled
                case CANCELLED:
                    wrapper.eq(orderStatusColumn, OrderStatusEnum.CANCELLED.name());
                    break;
                //Complete
                case COMPLETE:
                    wrapper.eq(orderStatusColumn, OrderStatusEnum.COMPLETED.name());
                    break;
                default:
                    break;
            }
        }

        // 依赖Order
        wrapper.eq(parentOrderSn != null, "o.parent_order_sn", parentOrderSn);
        // 促销活动id
        wrapper.eq(CharSequenceUtil.isNotEmpty(promotionId), "o.promotion_id", promotionId);

        wrapper.eq(CharSequenceUtil.isNotEmpty(orderPromotionType), "o.order_promotion_type", orderPromotionType);

        if (CharSequenceUtil.isNotEmpty(flowPrice)) {
            String[] s = flowPrice.split("_");
            if (s.length > 1) {
                wrapper.between("o.flow_price", s[0], s[1]);
            } else {
                wrapper.ge("o.flow_price", s[0]);
            }
        }
        wrapper.eq("o.delete_flag", false);
        return wrapper;
    }

}
