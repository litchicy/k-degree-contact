import K_DCQ.DCQ;
import K_RDCQ.RDCQ;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import org.junit.Test;
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
        String dataset = "taxi_rename";
        // Taxi数据集中轨迹数据的采样点数量
        int totalTimePoints = 1440;
        // Taxi中移动对象轨迹的数量 ，拥有4315
        int numberOfTracksOfObject = 4315;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 300, 800, 1500));
        // 时间效率结果分析的传染源id
        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));
//        String filePath = "D:\\dataset\\contact\\taxi_result\\first_contacted_object—R-tree\\sourceId="
//                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";


//        String dataset = "TDrive_rename";
//        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
//        // TDrive中移动对象轨迹的数量，拥有4142
//        int numberOfTracksOfObject = 4142;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
//        // 时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(5));
//        String filePath = "D:\\dataset\\contact\\TDrive_result\\first_contacted_object—R-tree\\sourceId="
//                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";

        // 全部移动对象轨迹，构建R树
        List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
        // r树初始化
        RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);
//        rTrees[0].visualize(600,600)
//                .save("C:\\Users\\Administrator\\Desktop\\mytree0.png");
//        rTrees[1].visualize(600,600)
//                .save("C:\\Users\\Administrator\\Desktop\\mytree1.png");
        for(int i = 2; i <= 6; i = i + 1) {
            degree = i;
            System.out.println("正在执行：degree = " + degree);

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
//        for(QueryResult result : results) {
//            System.err.println(result);
//        }
        String filePath = "D:\\dataset\\contact\\TDrive_result\\first_contacted_object—R-tree\\sourceId="
                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";

            Utils.writeListToFile(results, filePath);
        }

    }

    @Test
    public void test1() {
        List<HashSet<Integer>> possiblyInfectedSetArray = new ArrayList<>(5);
        for(int i = 0; i < 5; i++) {
            possiblyInfectedSetArray.add(new HashSet<>());
        }
        possiblyInfectedSetArray.get(0).add(1);
        System.out.println("possiblyInfectedSetArray.size() = " + possiblyInfectedSetArray.size());
        for(int i = 0; i < 5; i++) {
            System.out.println("possiblyInfectedSetArray.get(i).isEmpty() = " + possiblyInfectedSetArray.get(i).isEmpty());
        }
    }
}
