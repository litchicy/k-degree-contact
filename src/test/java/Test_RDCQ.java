import K_DCQ.DCQ;
import K_RDCQ.RDCQ;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import org.junit.Test;
import utils.HaversineDistance;
import utils.Initialization;
import utils.Utils;

import java.util.*;

public class Test_RDCQ {

    @Test
    public void test() {
        // 度数
        int degree = 3;
        // 距离阈值 km单位
        double thresholdOfDistance = 0.03;
        // 滑动窗口大小
        int widthOfSlidingWindow = 8;

        // 数据集名称
//        String dataset = "taxi_rename";
        // Taxi数据集中轨迹数据的采样点数量
//        int totalTimePoints = 1440;
        // Taxi中移动对象轨迹的数量 ，拥有4315
//        int numberOfTracksOfObject = 4315;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 300, 800, 1500));
        // 时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));

        String dataset = "TDrive_rename";
//        // TDrive数据集中轨迹数据的采样点数量
        int totalTimePoints = 2017;
//        // TDrive中移动对象轨迹的数量，拥有4142
        int numberOfTracksOfObject = 4142;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
//        // 时间效率结果分析的传染源id
        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1800));

        // 全部移动对象轨迹，构建R树
        List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
        // r树初始化
        RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

        for(int i = 8; i <= 16; i = i + 2) {
            widthOfSlidingWindow = i;
            System.out.println("正在执行：widthOfSlidingWindow = " + widthOfSlidingWindow);
            // 不同数据集修改存储路径
            String filePath = "D:\\dataset\\contact\\TDrive_result\\first_contacted_object—R-tree(1)\\sourceId="
                    + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";

//            // 全部移动对象轨迹，构建R树
//            List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
//            // r树初始化
//            RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

            RDCQ rdcq = new RDCQ(degree, thresholdOfDistance, widthOfSlidingWindow, totalTimePoints, rTrees,
                    allTra, infectiousSourceID);
            long startTime = System.currentTimeMillis();
            List<QueryResult> results = rdcq.startQuery_contactedObjectFirst();
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println(i + " k度密接数量：" + results.size());
            System.out.println("总共花费时间（纳秒）：" + elapsedTime);
            System.out.println();
            Utils.writeListToFile(results, filePath);
        }

    }

    @Test
    public void test2() {
        int widthOfSlidingWindow = 8;
        for(int count=0 ; count<3; count++) {
            int timeOfSample = 0 + (count * ((widthOfSlidingWindow - 1) / 2));
            System.out.println("timeOfSample = " + timeOfSample);
        }

    }

    @Test
    public void test1() {
        // 度数
        int degree = 3;
        // 距离阈值 km单位
        double thresholdOfDistance = 0.03;
        // 滑动窗口大小
        int widthOfSlidingWindow = 8;

        // 数据集名称
        String dataset = "taxi_rename";
        // Taxi数据集中轨迹数据的采样点数量
        int totalTimePoints = 1440;
        // Taxi中移动对象轨迹的数量 ，拥有4315
        int numberOfTracksOfObject = 4315;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 300, 800, 1500));
        // 时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));

//        String dataset = "TDrive_rename";
//        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
//        // TDrive中移动对象轨迹的数量，拥有4142
//        int numberOfTracksOfObject = 4142;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
//        // 时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000));

        // 全部移动对象轨迹，构建R树
        List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
        for(int i = 273; i < 281; i++) {
            PositionPoint source = allTra.get(2357).get(i);
            PositionPoint object = allTra.get(2691).get(i);
            if(HaversineDistance.calculateHaversineDistance(source,object) - thresholdOfDistance > 0) {
                System.out.println("qqq");
            }
        }

    }
}
