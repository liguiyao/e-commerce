package cn.lili.modules.statistics.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Order统计数据VO
 *
 * @author Bulbasaur
 * @since 2020/12/9 17:13
 */
@Data
public class OrderStatisticsDataVO {

    @ApiModelProperty(value = "店铺")
    private String storeName;

    @ApiModelProperty(value = "购买人")
    private String memberName;

    @ApiModelProperty(value = "Order金额")
    private Double price;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "create时间")
    private Date createTime;

    @ApiModelProperty(value = "Order编号")
    private String orderItemSn;
}
