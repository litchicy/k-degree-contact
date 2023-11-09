import LIPMA.*;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import entity.ContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import org.junit.Test;
import utils.Initialization;
import utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Test_LIPMA_BASE {

    @Test
    public void test() {
        // 度数
        int degree = 3;
        // 距离阈值 km单位
        double thresholdOfDistance = 0.03;
        // 滑动窗口大小
        int widthOfSlidingWindow = 8;

//        // 数据集名称
        String dataset = "taxi_rename";
//        // Taxi数据集中轨迹数据的采样点数量
        int totalTimePoints = 1440;
//        // Taxi中移动对象轨迹的数量 ，默认取4100 拥有4315
        int numberOfTracksOfObject = 4315;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 300, 800, 1500));
        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));
//        String filePath = "D:\\dataset\\contact\\taxi_result\\chenyu—base\\sourceId="
//                + infectiousSourceID.toString() + "_k=" + degree +  "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + ".txt";


//        String dataset = "TDrive_rename";
        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
        // TDrive中移动对象轨迹的数量，默认取4100 拥有4142
//        int numberOfTracksOfObject = 4142;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(5));

        //        String filePath = "D:\\dataset\\contact\\TDrive_result\\chenyu-base\\sourceId="
//                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + ".txt";
//


        for(int i = 2; i <= 6; i = i + 1) {
            degree = i;
            System.out.println("正在执行：degree = " + degree);

            // 待分析对象列表
            List<MovingObject> objectsToBeAnalyzed = Initialization.InitializeObjectsToBeAnalyzed(infectiousSourceID, dataset, numberOfTracksOfObject);
            // 初始传染源集合
            Set<MovingObject> initialInfectiousObjects = Initialization.InitializeInfectiousObjects(infectiousSourceID, dataset);
            BASE_LIPMA base_lipma = new BASE_LIPMA(thresholdOfDistance, widthOfSlidingWindow, totalTimePoints,
                    objectsToBeAnalyzed, initialInfectiousObjects, infectiousSourceID);

            long startTime = System.currentTimeMillis();
            List<ContactEvent> results = base_lipma.startQuery();
            List<List<ContactEvent>> kResults = base_lipma.k_degree_splice(degree, results);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("密接事件数量：" + results.size());
            System.out.println("总共花费时间（纳秒）：" + elapsedTime);
            System.out.println("k度密接事件数量：" + kResults.size());

//            String filePath = "D:\\dataset\\contact\\TDrive_result\\chenyu-base\\sourceId="
//                    + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + ".txt";
//
            String filePath = "D:\\dataset\\contact\\taxi_result\\chenyu—base\\sourceId="
                + infectiousSourceID.toString() + "_k=" + degree +  "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + ".txt";

            Utils.writeListToFile(results, filePath);
            Utils.appendListToFile(kResults, filePath);
        }

    }
}
