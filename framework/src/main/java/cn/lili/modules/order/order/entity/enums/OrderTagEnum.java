package cn.lili.modules.order.order.entity.enums;

/**
 * 前端Order页面TAB标签枚举
 *
 * @author Chopper
 * @since 2020/11/17 7:28 下午
 */
public enum OrderTagEnum {


    /**
     * 所有Order
     */
    ALL("全部"),

    /**
     * Unpaid
     */
    WAIT_PAY("Unpaid"),

    /**
     * 待收货
     */
    WAIT_SHIP("待发货"),

    /**
     * 待收货
     */
    WAIT_ROG("待收货"),

    /**
     * Complete
     */
    COMPLETE("Complete"),

    /**
     * Cancelled
     */
    CANCELLED("Cancelled");

    private final String description;


    OrderTagEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static OrderTagEnum defaultType() {
        return ALL;
    }


}
