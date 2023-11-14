import LIPMA.LIPMA;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import entity.ContactEvent;
import entity.PositionPoint;
import entity.QueryResult;
import org.junit.Test;
import utils.Initialization;
import utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Test_LIPMA {

    @Test
    public void test() {
        // 度数
        int degree = 3;
        // 距离阈值 km单位
        double thresholdOfDistance = 0.03;
        // 滑动窗口大小
        int widthOfSlidingWindow = 8;

//         数据集名称
        String dataset = "taxi_rename";
//        // Taxi数据集中轨迹数据的采样点数量
        int totalTimePoints = 1440;
//        // Taxi中移动对象轨迹的数量 ，默认取4100 拥有4315
        int numberOfTracksOfObject = 4315;
//                Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 300, 800, 1500));
        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));


//        String dataset = "TDrive_rename";
        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
        // TDrive中移动对象轨迹的数量，默认取4100 拥有4142
//        int numberOfTracksOfObject = 4142;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
        // 时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1800));

        // 全部移动对象轨迹，构建R树
//        List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
        // r树初始化
//        RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

        for(int i = 2; i <= 6; i = i + 1) {
            degree = i;
            System.out.println("正在执行：degree = " + degree);
            // 不同数据集修改存储路径
            String filePath = "D:\\dataset\\contact\\taxi_result\\chenyu\\sourceId="
                    + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";


//            // 全部移动对象轨迹，构建R树
            List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
//            // r树初始化
            RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

            LIPMA lipma = new LIPMA(thresholdOfDistance, widthOfSlidingWindow, totalTimePoints, rTrees, allTra, infectiousSourceID);
            long startTime = System.currentTimeMillis();
            List<ContactEvent> results = lipma.startQuery();
            List<List<ContactEvent>> kResults = lipma.k_degree_splice(degree, results);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("密接事件数量：" + results.size());
            System.out.println("总共花费时间（纳秒）：" + elapsedTime);
            System.out.println(i + " k度密接事件数量：" + kResults.size());
            System.out.println();
            Utils.writeListToFile(results, filePath);
            Utils.appendListToFile(kResults, filePath);
        }

    }


}
