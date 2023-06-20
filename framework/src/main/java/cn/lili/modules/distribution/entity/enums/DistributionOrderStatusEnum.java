package cn.lili.modules.distribution.entity.enums;

/**
 * 分销员Order状态
 *
 * @author pikachu
 */
public enum DistributionOrderStatusEnum {
    //未完成
    NO_COMPLETED("未完成"),
    //待结算（冻结）
    WAIT_BILL("待结算"),
    //待提现
    WAIT_CASH("待提现"),
    //已提现
    COMPLETE_CASH("提现完成"),
    //Order取消
    CANCEL("Order取消"),
    //Order取消
    REFUND("退款");

    private final String description;

    DistributionOrderStatusEnum(String description) {
        this.description = description;
    }
}
