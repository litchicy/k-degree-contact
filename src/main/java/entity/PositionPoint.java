package entity;

import lombok.Data;

/**
 * 位置点数据结构
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
public class PositionPoint {

    /**
     * 位置点id
     */
    int id;

    /**
     * 纬度
     */
    double latitude;

    /**
     * 经度
     */
    double longitude;

}
