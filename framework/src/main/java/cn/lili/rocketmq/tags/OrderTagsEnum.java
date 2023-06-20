package cn.lili.rocketmq.tags;

/**
 * Order操作枚举
 *
 * @author paulG
 * @since 2020/12/9
 **/
public enum OrderTagsEnum {

    /**
     * Ordercreate
     */
    ORDER_CREATE("Ordercreate"),
    /**
     * Order状态改变
     */
    STATUS_CHANGE("Order状态改变");


    private final String description;

    OrderTagsEnum(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }


}
