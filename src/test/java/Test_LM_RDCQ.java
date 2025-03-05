import K_RDCQ.RDCQ;
import LM_K_RDCQ.LM_RDCQ;
import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import entity.LooseContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import org.junit.Test;
import utils.HaversineDistance;
import utils.Initialization;
import utils.Utils;

import java.util.*;

public class Test_LM_RDCQ {

    @Test
    public void test() {
        // 度数
        int degree = 3;
        // 距离阈值 km单位
        double thresholdOfDistance = 0.03;
        // 滑动窗口大小
        int widthOfSlidingWindow = 10;

        // 时间松弛阈值
        double ratioOfTime = 0.8;

        // 数据集名称
//        String dataset = "taxi_rename";
////         Taxi数据集中轨迹数据的采样点数量
//        int totalTimePoints = 1440;
////         Taxi中移动对象轨迹的数量 ，拥有4315
//        int numberOfTracksOfObject = 4315;
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20, 100, 113, 300, 800, 1500, 2720));
//         时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(20));

        String dataset = "TDrive_rename";
//        // TDrive数据集中轨迹数据的采样点数量
        int totalTimePoints = 2017;
//        // TDrive中移动对象轨迹的数量，拥有4142
        int numberOfTracksOfObject = 4142;
        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1000, 1200, 1400, 1600, 1800));
////        // 时间效率结果分析的传染源id
//        Set<Integer> infectiousSourceID = new HashSet<>(Arrays.asList(1800));

        // 全部移动对象轨迹，构建R树
        List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
        // r树初始化
        RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

        double[] ratioOfTimes = {0.6, 0.7, 0.8, 0.9, 1.0};
//        double[] distances = {0.01, 0.02, 0.03, 0.04, 0.05};

        for(int i = 0; i <= 4; i = i + 1) {
            ratioOfTime = ratioOfTimes[i];
            System.out.println("正在执行：ratioOfTime = " + ratioOfTime);

//            // 全部移动对象轨迹，构建R树
//            List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
//            // r树初始化
//            RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

            // 不同数据集修改存储路径
            String filePath = "D:\\dataset\\contact\\taxi_result\\loose-multi-source-rtree\\sourceId="
                    + infectiousSourceID.toString() + "_k=" + degree + "_d=" + thresholdOfDistance + "_ratio=" + ratioOfTime + "_w=" + widthOfSlidingWindow + "_n=" + numberOfTracksOfObject + ".txt";

            // 待分析对象列表
            Set<Integer> objectsToBeAnalyzed = Initialization.InitializeObjectsIDToBeAnalyzedByIterator(infectiousSourceID, numberOfTracksOfObject);

            LM_RDCQ lm_rdcq = new LM_RDCQ(degree, thresholdOfDistance, widthOfSlidingWindow,
                    ratioOfTime, totalTimePoints, rTrees, allTra,
                    objectsToBeAnalyzed,
                    infectiousSourceID);
            long startTime = System.currentTimeMillis();
            List<LooseContactEvent> results = lm_rdcq.queryResult();
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("总共花费时间（毫秒）：" + elapsedTime);
            System.out.println();
            Utils.writeListToFile(results, filePath);
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

    @Test
    public void test3() {
        // 距离阈值 km单位
        double thresholdOfDistance = 0.03;
        // 数据集名称
        String dataset = "taxi_rename";
        int totalTimePoints = 1440;

        Set<Integer> allSourceObject = new HashSet<>(Arrays.asList(20, 100, 113, 300, 800, 1500, 2720));

        int numberOfTracksOfObject = 4315;

        // 全部移动对象轨迹，构建R树
        List<List<PositionPoint>> allTra = Initialization.InitializeAllTras(dataset, numberOfTracksOfObject);
        // r树初始化
        RTree<Integer, Point>[] rTrees = Initialization.InitializeRTree(allTra, totalTimePoints);

        for(int i = 0;  i < 10; i++) {
            double lonOfObject = allTra.get(646 - 1).get(i).getLongitude();
            double latOfObject = allTra.get(646 - 1).get(i).getLatitude();
            Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfObject, latOfObject);
            List<Entry<Integer, Point>> searchResult = rTrees[i].search(queryRectangle).toList().toBlocking().single();
            TreeSet<Integer> tempSourceSet = searchResltProcessing(searchResult, allSourceObject);
            System.out.println("time" + i+ "   tempSourceSet = " + tempSourceSet);
            filterErrorPointInSearchResult(tempSourceSet, lonOfObject, latOfObject, allTra, i);
            System.err.println("time" + i+ "   tempSourceSet = " + tempSourceSet);
        }


    }
    private void filterErrorPointInSearchResult(Set<Integer> searchResult, double lonOfObject, double latOfObject, List<List<PositionPoint>> allTraOfObjects, int time) {
        PositionPoint objectPostion = new PositionPoint(0, latOfObject, lonOfObject);
        Iterator<Integer> iterator = searchResult.iterator();
        while(iterator.hasNext()) {
            Integer sourceId = iterator.next();
            double lonOfSource = allTraOfObjects.get(sourceId - 1).get(time).getLongitude();
            double latOfSource= allTraOfObjects.get(sourceId - 1).get(time).getLatitude();
            PositionPoint sourcePositon = new PositionPoint(0, latOfSource, lonOfSource);
            // 过滤矩形中不合理的点
            if(HaversineDistance.calculateHaversineDistance(sourcePositon,objectPostion) - 0.03 > 0) {
                System.out.println("bumanzu 距离");
                iterator.remove();
            }
        }
    }

    private TreeSet<Integer> searchResltProcessing(List<Entry<Integer, Point>> searchResult,
                                                   Set<Integer> allSourceObject) {
        TreeSet<Integer> tempObjectIdSet = new TreeSet<>();
        for(Entry<Integer, Point> entry : searchResult) {
            // 改点所属的移动对象Id
            Integer objectId = entry.value();
            if(allSourceObject.contains(objectId)) {
                tempObjectIdSet.add(objectId);
            }
        }
//        possiblyInfectedSetArray.set(indexOfArray, tempObjectIdSet);
        return tempObjectIdSet;
    }

    private Rectangle lonAndLatTranformAndFormRectangle(double lonOfSource, double latOfSource) {
        Position from = new Position(latOfSource, lonOfSource);
        Position north = from.predict(0.03, 0);
        Position south = from.predict(0.03, 180);
        Position east = from.predict(0.03, 90);
        Position west = from.predict(0.03, 270);
        return Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat());
    }
}
