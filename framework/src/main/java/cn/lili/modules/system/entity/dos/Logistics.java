package cn.lili.modules.system.entity.dos;

import cn.lili.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * logistics公司设置
 *
 * @author Chopper
 * @since 2020/11/17 8:01 下午
 */
@Data
@TableName("li_logistics")
@ApiModel(value = "logistics公司")
public class Logistics extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "logistics公司名称必填")
    @ApiModelProperty(value = "logistics公司名称")
    private String name;

    @NotEmpty(message = "logistics公司code必填")
    @ApiModelProperty(value = "logistics公司code")
    private String code;

    @ApiModelProperty(value = "支持电子面单")
    private String standBy;

    @ApiModelProperty(value = "logistics公司电子面单表单")
    private String formItems;

    @ApiModelProperty(value = "禁用状态 OPEN：开启，CLOSE：禁用")
    private String disabled;
}