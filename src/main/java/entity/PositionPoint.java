package entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置点数据结构
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
@NoArgsConstructor
public class PositionPoint {

    /**
     * 位置点id，也就是time
     */
    private int time;

    /**
     * 纬度
     */
    private double latitude;

    /**
     * 经度
     */
    private double longitude;

    public PositionPoint(int time, double latitude, double longitude) {
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
