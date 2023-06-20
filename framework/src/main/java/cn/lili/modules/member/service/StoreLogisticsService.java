package cn.lili.modules.member.service;

import cn.lili.modules.store.entity.dos.StoreLogistics;
import cn.lili.modules.store.entity.dto.StoreLogisticsCustomerDTO;
import cn.lili.modules.system.entity.vo.StoreLogisticsVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 店铺-logistics公司业务层
 *
 * @author Chopper
 * @since 2020/11/17 8:02 下午
 */
public interface StoreLogisticsService extends IService<StoreLogistics> {

    /**
     * 获取当前店铺的logistics公司列表
     *
     * @param storeId 店铺id
     * @return logistics公司列表
     */
    List<StoreLogisticsVO> getStoreLogistics(String storeId);

    /**
     * 获取当前店铺已选择的logistics公司列表
     *
     * @param storeId 店铺id
     * @return logistics公司列表
     */
    List<StoreLogisticsVO> getStoreSelectedLogistics(String storeId);

    /**
     * 获取当前店铺已选择的logistics公司名称列表
     *
     * @param storeId 店铺id
     * @return logistics公司列表
     */
    List<String> getStoreSelectedLogisticsName(String storeId);

    /**
     * 添加店铺-logistics公司
     *
     * @param logisticsId logistics公司设置id
     * @param storeId 店铺id
     * @return 店铺logistics公司
     */
    StoreLogistics add(String logisticsId, String storeId, StoreLogisticsCustomerDTO storeLogisticsCustomerDTO);

    /**
     * 获取当前店铺已选择的logistics公司并使用电子面单列表
     *
     * @param storeId 店铺id
     * @return logistics公司列表
     */
    List<StoreLogisticsVO> getStoreSelectedLogisticsUseFaceSheet(String storeId);


    /**
     * 修改店铺-logistics公司电子面单参数
     * @param logisticsId logistics公司设置id
     * @param storeId 店铺id
     * @return 店铺logistics公司
     */
    StoreLogistics update(String logisticsId, String storeId, StoreLogisticsCustomerDTO storeLogisticsCustomerDTO);


    /**
     * 获取店铺logistics信息回填
     * @param logisticsId logisticsid
     * @return 店铺logistics信息
     */
    StoreLogistics getStoreLogisticsInfo(String logisticsId);

    /**
     * 获取当前店铺已开启的logistics公司列表
     *
     * @param storeId 店铺id
     * @return logistics公司列表
     */
    List<StoreLogisticsVO> getOpenStoreLogistics(String storeId);

    /**
     * 获取当前店铺未开启的logistics公司列表
     *
     * @param storeId 店铺id
     * @return logistics公司列表
     */
    List<StoreLogisticsVO> getCloseStoreLogistics(String storeId);

}