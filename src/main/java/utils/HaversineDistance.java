package utils;

import entity.PositionPoint;

/**
 * 半正矢距离
 * 计算两个经纬度坐标点之间的距离
 *
 * @author cy
 * @date 2023/11/04
 */
public class HaversineDistance {

    /**
     * 地球半径（以km为单位）
     */
    private static final double EARTH_DISTANCE = 6371.0;

    /**
     * 计算两个坐标点之间的欧式距离
     *
     * @param point1 坐标点1
     * @param point2 坐标点2
     * @return double
     */
    public static double calculateHaversineDistance(PositionPoint point1, PositionPoint point2) {
        // 坐标点1的纬度转换为弧度
        double latOfPoint1 = Math.toRadians(point1.getLatitude());
        // 坐标点1的经度转化为弧度
        double lonOfPoint1 = Math.toRadians(point1.getLongitude());
        // 坐标点2的纬度转换为弧度
        double latOfPoint2 = Math.toRadians(point2.getLatitude());
        // 坐标点2的经度转换为弧度
        double lonOfPoint2 = Math.toRadians(point2.getLongitude());

        double dLat = latOfPoint2 - latOfPoint1;
        double dLon = lonOfPoint2 - lonOfPoint1;

        // HaversineDistance计算公式
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(latOfPoint1) * Math.cos(latOfPoint2) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_DISTANCE * c;
    }

    public static void main(String[] args) {
        PositionPoint point = new PositionPoint(0, 31.3815, 121.493);
        PositionPoint point2 = new PositionPoint(0, 31.3818, 121.493);
        System.out.println("calculateHaversineDistance(point, point2) = " + calculateHaversineDistance(point, point2));
    }
}
