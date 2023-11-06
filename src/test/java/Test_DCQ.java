import K_DCQ.DCQ;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import org.junit.Test;
import utils.HaversineDistance;
import utils.Initialization;
import utils.Utils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Test_DCQ {

    @Test
    public void test() {
        // 度数
        int degree = 4;
        // 距离阈值 km为单位
        double thresholdOfDistance = 0.02;
        // 滑动窗口大小
        int widthOfSlidingWindow = 8;
        List<Integer> infectiousSourceID = new ArrayList<>(Arrays.asList(2000, 2500 , 3000, 3500, 4000));

        // 数据集名称
        String dataset = "taxi_rename";
        // Taxi数据集中轨迹数据的采样点数量
        int totalTimePoints = 1440;
        // Taxi中移动对象轨迹的数量
        int numberOfTracksOfObject = 4315;
        String filePath = "D:\\dataset\\contact\\taxi_result\\first_contacted_object\\sourceId="
                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + ".txt";


//        String dataset = "TDrive_rename";
//        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
//        // TDrive中移动对象轨迹的数量
//        int numberOfTracksOfObject = 4142;
//        String filePath = "D:\\dataset\\contact\\TDrive_result\\first_contacted_object\\sourceId="
//                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + ".txt";


        // 待分析对象列表
        List<MovingObject> objectsToBeAnalyzed = Initialization.InitializeObjectsToBeAnalyzed(infectiousSourceID, dataset, numberOfTracksOfObject);
        // 初始传染源集合
        Set<MovingObject> initialInfectiousObjects = Initialization.InitializeInfectiousObjects(infectiousSourceID, dataset);
        DCQ dcq = new DCQ(degree, thresholdOfDistance, widthOfSlidingWindow, totalTimePoints,
                objectsToBeAnalyzed, initialInfectiousObjects);
        long startTime = System.currentTimeMillis();
        List<QueryResult> results = dcq.startQuery_compareWithContactObjectsFirst();
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("k度密接数量：" + results.size());
        System.out.println("总共花费时间（纳秒）：" + elapsedTime);
//        for(QueryResult result : results) {
//            System.err.println(result);
//        }
        Utils.writeListToFile(results, filePath);
    }

    @Test
    public void test1() {
        PositionPoint point2 = new PositionPoint(0, 31.2211, 121.468);
        PositionPoint point1 = new PositionPoint(0, 31.2215, 121.469);
        System.out.println("HaversineDistance.calculateHaversineDistance(point1, point2) = " +
                HaversineDistance.calculateHaversineDistance(point1, point2));
    }

    @Test
    public void test2() {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        for(int i = 0; i < 3; i++) {
            for(String item : list) {
                if(item.equals("item2")) {
                    list.remove(item);
                }
                System.out.println("Current Item: " + item);
            }
            System.out.println("Updated" + i + " List: " + list);
        }

        System.out.println("Updated List: " + list);
    }

    @Test
    public void test3() {
        Set<Map<String, Integer>> contactedMovingObjectsInCurrentWindow = new LinkedHashSet<>();
        Map<String, Integer> a = new HashMap<>();
        a.put("id", 1);
        contactedMovingObjectsInCurrentWindow.add(a);
        System.out.println("contactedMovingObjectsInCurrentWindow = " + contactedMovingObjectsInCurrentWindow);
        Map<String, Integer> b = new HashMap<>();
        b.put("id", 1);
        contactedMovingObjectsInCurrentWindow.add(b);
        System.out.println("contactedMovingObjectsInCurrentWindow = " + contactedMovingObjectsInCurrentWindow);
        Map<String, Integer> c = new HashMap<>();
        c.put("id", 2);
        contactedMovingObjectsInCurrentWindow.add(c);
        System.out.println("contactedMovingObjectsInCurrentWindow = " + contactedMovingObjectsInCurrentWindow);
    }

    private DCQ test4() {
        return null;
    }

}
