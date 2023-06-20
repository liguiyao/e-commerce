package cn.lili.modules.order.order.entity.enums;

/**
 * Order状态枚举
 *
 * @author Chopper
 * @since 2020/11/17 7:28 下午
 */
public enum PayStatusEnum {

    /**
     * 支付状态
     */
    UNPAID("Unpaid"),
    PAID("已付款"),
    CANCEL("Cancelled");

    private final String description;

    PayStatusEnum(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }


}
