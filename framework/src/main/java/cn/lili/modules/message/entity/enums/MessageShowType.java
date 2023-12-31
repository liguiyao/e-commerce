package cn.lili.modules.message.entity.enums;

/**
 * 消息展示类型
 *
 * @author pikachu
 * @since 2020/12/8 9:46
 */
public enum MessageShowType {

    //Order
    ORDER("Order"),
    //售后单
    AFTER_SALE("售后Order"),
    //站内信
    NOTICE("站内信");

    private final String description;

    MessageShowType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


}
