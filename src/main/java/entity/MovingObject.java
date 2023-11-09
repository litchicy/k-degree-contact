package entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定义移动对象数据结构
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovingObject  implements Comparable<MovingObject>{

    /**
     * 移动对象id
     */
    private int id;

    /**
     * 移动对象的完整轨迹数据
     */
    private Trajectory trajectory;

    @Override
    public int compareTo(MovingObject other) {
        return Integer.compare(this.id, other.id);
    }

}
