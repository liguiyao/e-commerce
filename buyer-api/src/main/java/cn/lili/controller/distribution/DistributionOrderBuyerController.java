package cn.lili.controller.distribution;

import cn.lili.common.enums.ResultUtil;
import cn.lili.common.vo.ResultMessage;
import cn.lili.modules.distribution.entity.dos.DistributionOrder;
import cn.lili.modules.distribution.entity.vos.DistributionOrderSearchParams;
import cn.lili.modules.distribution.service.DistributionOrderService;
import cn.lili.modules.distribution.service.DistributionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 买家端,分销商品佣金提现接口
 *
 * @author pikachu
 * @since 2020/11/16 10:03 下午
 */
@RestController
@Api(tags = "买家端,分销Order接口")
@RequestMapping("/buyer/distribution/order")
public class DistributionOrderBuyerController {

    /**
     * 分销Order
     */
    @Autowired
    private DistributionOrderService distributionOrderService;
    /**
     * 分销员
     */
    @Autowired
    private DistributionService distributionService;


    @ApiOperation(value = "分销员Order")
    @GetMapping
    public ResultMessage<IPage<DistributionOrder>> casHistory(DistributionOrderSearchParams distributionOrderSearchParams) {
        //获取当前登录的分销员
        distributionOrderSearchParams.setDistributionId(distributionService.getDistribution().getId());
        return ResultUtil.data(distributionOrderService.getDistributionOrderPage(distributionOrderSearchParams));
    }


}
