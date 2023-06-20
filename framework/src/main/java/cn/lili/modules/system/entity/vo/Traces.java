package cn.lili.modules.system.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * logistics信息
 *
 * @author Chopper
 * @since 2021/1/18 3:28 下午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Traces {

    /**
     * logistics公司
     */
    private String shipper;

    /**
     * logistics单号
     */
    private String logisticCode;

    /**
     * logistics详细信息
     */
    private List<Map> traces;
}
