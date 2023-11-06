package entity;

import lombok.Data;

import java.util.List;

/**
 * 轨迹数据结构
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
public class Trajectory {

    /**
     * 移动对象的轨迹
     */
    private List<PositionPoint> tra;

}
