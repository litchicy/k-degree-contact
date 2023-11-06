package entity;

import lombok.Data;

/**
 * 定义移动对象数据结构
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
public class MovingObject {

    /**
     * 移动对象id
     */
    private int id;

    /**
     * 移动对象的完整轨迹数据
     */
    private Trajectory trajectory;

}
