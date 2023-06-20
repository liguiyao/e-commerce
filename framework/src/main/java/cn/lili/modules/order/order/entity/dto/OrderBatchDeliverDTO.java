package cn.lili.modules.order.order.entity.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Order批量发货DTO
 * @author Bulbasaur
 * @since 2021/5/26 4:21 下午
 *
 */
@Data
public class OrderBatchDeliverDTO {

    @ApiModelProperty(value = "OrderSN")
    private String orderSn;

    @ApiModelProperty(value = "logistics公司ID")
    private String logisticsId;

    @ApiModelProperty(value = "logistics公司名称")
    private String logisticsName;

    @ApiModelProperty(value = "发货单号")
    private String logisticsNo;

}
