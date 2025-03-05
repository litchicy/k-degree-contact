import K_DCQ.DCQ;
import LM_K_DCQ.LM_DCQ;
import entity.LooseContactEvent;
import entity.MovingObject;
import entity.QueryResult;
import org.junit.Test;
import utils.Initialization;
import utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Test_LM_DCQ {

    @Test
    public void test() {
        // 度数
        int degree = 3;
        // 距离阈值 km为单位
        double thresholdOfDistance = 0.03;
        // 滑动窗口大小
        int widthOfSlidingWindow = 10;

        // 时间松弛阈值
        double ratioOfTime = 0.8;

        // 数据集名称
        String dataset = "taxi_rename";
        // Taxi数据集中轨迹数据的采样点数量
        int totalTimePoints = 1440;
        // Taxi中移动对象轨迹的数量 ，默认取4100 拥有4315
        int numberOfTracksOfObject = 4315;
        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 113, 300, 800, 1500, 2720));
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));


//        String dataset = "TDrive_rename";
//        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
//        // TDrive中移动对象轨迹的数量，默认取4100 拥有4142
//        int numberOfTracksOfObject = 4142;
////        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1800));

//        List<MovingObject> objectsToBeAnalyzed = Initialization.InitializeObjectsToBeAnalyzed(infectiousSourceID, dataset, numberOfTracksOfObject);
//        // 初始传染源集合
//        Set<MovingObject> initialInfectiousObjects = Initialization.InitializeInfectiousObjects(infectiousSourceID, dataset);

        // 待分析对象列表
        List<MovingObject> objectsToBeAnalyzed = Initialization.InitializeObjectsToBeAnalyzedByIterator(infectiousSourceID, dataset, numberOfTracksOfObject);
        // 初始传染源集合
        Set<MovingObject> initialInfectiousObjects = Initialization.InitializeInfectiousObjects(infectiousSourceID, dataset);

//        for(int i = 2; i <= 6; i = i + 1) {
//            degree = i;
            System.out.println("正在执行：degree = " + degree);
            // 不同数据集修改存储路径
            String filePath = "D:\\dataset\\contact\\taxi_result\\loose-multi-source-base\\sourceId="
                    + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_ratio=" + ratioOfTime +"_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";

//            // 待分析对象列表
//            List<MovingObject> objectsToBeAnalyzed = Initialization.InitializeObjectsToBeAnalyzedByIterator(infectiousSourceID, dataset, numberOfTracksOfObject);
//            // 初始传染源集合
//            Set<MovingObject> initialInfectiousObjects = Initialization.InitializeInfectiousObjects(infectiousSourceID, dataset);
            LM_DCQ lmDcq = new LM_DCQ(degree, thresholdOfDistance, widthOfSlidingWindow, ratioOfTime, totalTimePoints,
                    objectsToBeAnalyzed, initialInfectiousObjects);
            long startTime = System.currentTimeMillis();
            List<LooseContactEvent> results = lmDcq.queryResult();
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
//            System.out.println(2 + " k度密接数量：" + results);
            System.out.println("总共花费时间（毫秒）：" + elapsedTime);
            System.out.println();
            Utils.writeListToFile(results, filePath);
//        }

    }
}
