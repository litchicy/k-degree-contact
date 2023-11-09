import K_DCQ.DCQ;
import entity.MovingObject;
import entity.QueryResult;
import org.junit.Test;
import utils.Initialization;
import utils.Utils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Test_DCQ {

    @Test
    public void test() {
        // 度数
        int degree = 3;
        // 距离阈值 km为单位
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
//        String filePath = "D:\\dataset\\contact\\taxi_result\\first_contacted_object\\sourceId="
//                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";


//        String dataset = "TDrive_rename";
        // TDrive数据集中轨迹数据的采样点数量
//        int totalTimePoints = 2017;
        // TDrive中移动对象轨迹的数量，默认取4100 拥有4142
//        int numberOfTracksOfObject = 4142;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(5));
//        List<MovingObject> objectsToBeAnalyzed = Initialization.InitializeObjectsToBeAnalyzed(infectiousSourceID, dataset, numberOfTracksOfObject);
//        // 初始传染源集合
//        Set<MovingObject> initialInfectiousObjects = Initialization.InitializeInfectiousObjects(infectiousSourceID, dataset);


        for(int i = 2; i <= 6; i = i + 1) {
            degree = i;
            System.out.println("正在执行：degree = " + degree);
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
            System.out.println(i + " k度密接数量：" + results.size());
            System.out.println("总共花费时间（毫秒）：" + elapsedTime);
            System.out.println();
//        for(QueryResult result : results) {
//            System.err.println(result);
//        }
//            String filePath = "D:\\dataset\\contact\\TDrive_result\\first_contacted_object\\sourceId="
//                    + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";
//
            String filePath = "D:\\dataset\\contact\\taxi_result\\first_contacted_object\\sourceId="
                + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";

            Utils.writeListToFile(results, filePath);
        }

    }

    @Test
    public void test1() {
        LinkedHashSet<Integer> set1 = new LinkedHashSet<>();
        set1.add(1);
        set1.add(2);
        set1.add(3);

        LinkedHashSet<Integer> set2 = new LinkedHashSet<>();
        set2.add(2);
        set2.add(3);
        set2.add(4);

        // 创建一个新的 LinkedHashSet 用于存放交集
        LinkedHashSet<Integer> intersectionSet = new LinkedHashSet<>(set1);

        // 使用 retainAll 方法获取交集
        set1.retainAll(set2);

        System.out.println("交集为：" + set1);
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
