package cn.lili.modules.logistics.entity.dto;

import cn.lili.modules.order.order.entity.dos.Order;
import cn.lili.modules.order.order.entity.dos.OrderItem;
import cn.lili.modules.store.entity.dos.StoreLogistics;
import cn.lili.modules.store.entity.dto.StoreDeliverGoodsAddressDTO;
import cn.lili.modules.system.entity.dos.Logistics;
import lombok.Data;

import java.util.List;

/**
 * 电子面单DTO
 *
 * @author Bulbasaur
 * @since 2023-02-16
 */
@Data
public class LabelOrderDTO {

    //Order
    Order order;
    //Order货物
    List<OrderItem> orderItems;
    //logistics公司
    Logistics logistics;
    //店铺logistics公司配置
    StoreLogistics storeLogistics;
    //店铺发件地址
    StoreDeliverGoodsAddressDTO storeDeliverGoodsAddressDTO;
}
