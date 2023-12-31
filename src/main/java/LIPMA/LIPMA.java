package LIPMA;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Circle;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import entity.ContactEvent;
import entity.PositionPoint;
import entity.QueryResult;
import rx.functions.Func1;
import utils.HaversineDistance;

import java.util.*;

/**
 * 陈玉基本方案，再k度拼接
 *
 * @author cy
 * @date 2023/11/05
 */
public class LIPMA {

    /**
     * 滑动窗口宽度
     */
    private int widthOfSlidingWindow;

    /**
     * 采样点数量（即轨迹集合中单条轨迹数据最多的值）
     */
    private int totalTimePoints;

    /**
     * 距离阈值
     */
    private double thresholdOfDistance;

    /**
     * 全部轨迹数据，移动对象id-1即是对应的轨迹数据
     */
    private List<List<PositionPoint>> allTraOfObjects;

    /**
     * 初始传染源对象的id
     */
    private Set<Integer> initialInfectiousObjectsId;

    /**
     * 已确定的密接对象
     */
    private Set<Integer> identifiedContactObjectsId;

    /**
     * 全部移动对象的所有时间采样点的r树
     */
    private RTree<Integer, Point>[] rTrees;

    // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
    private TreeSet<Integer>[] tempInfectedObjectIdInEachWindowArray;

    public LIPMA() {

    }
    public LIPMA(double d, int widthOfSlidingWindow, int totalTimePoints,
                RTree<Integer, Point>[] rTrees, List<List<PositionPoint>> allTraOfObjects, Set<Integer> initialInfectiousObjectsId) {
        this.thresholdOfDistance = d;
        this.widthOfSlidingWindow = widthOfSlidingWindow;
        this.totalTimePoints = totalTimePoints;
        this.rTrees = rTrees;
        this.allTraOfObjects = allTraOfObjects;
        this.initialInfectiousObjectsId = initialInfectiousObjectsId;
        this.identifiedContactObjectsId = new TreeSet<>();
        this.tempInfectedObjectIdInEachWindowArray = new TreeSet[widthOfSlidingWindow];
        for(int i= 0; i < widthOfSlidingWindow; i++) {
            tempInfectedObjectIdInEachWindowArray[i] = new TreeSet<>();
        }
    }

    /**
     * K度密接事件拼接
     *
     * @param k k度数
     * @param contactEventList 密接事件列表
     * @return {@link List}<{@link QueryResult}>
     */
    public List<List<ContactEvent>> k_degree_splice(int k, List<ContactEvent> contactEventList) {
        List<List<ContactEvent>> result = new ArrayList<>();
        List<ContactEvent> currentSequence = new ArrayList<>();

        combineEventsHelper(contactEventList, k, 0, currentSequence, result);

        return result;
    }

    private void combineEventsHelper(List<ContactEvent> events, int k, int startIndex, List<ContactEvent> currentSequence, List<List<ContactEvent>> result) {
        if (currentSequence.size() == k) {
            result.add(new ArrayList<>(currentSequence));
            return;
        }

        for (int i = startIndex; i < events.size(); i++) {
            ContactEvent currentEvent = events.get(i);

            if (currentSequence.isEmpty() && (initialInfectiousObjectsId.contains(currentEvent.getSourceId()))) {
                currentSequence.add(currentEvent);
                combineEventsHelper(events, k, i + 1, currentSequence, result);
                currentSequence.remove(currentSequence.size() - 1);
            } else if (!currentSequence.isEmpty() && currentEvent.getSourceId() == currentSequence.get(currentSequence.size() - 1).getObjectId()) {
                currentSequence.add(currentEvent);
                combineEventsHelper(events, k, i + 1, currentSequence, result);
                currentSequence.remove(currentSequence.size() - 1);
            }
        }
    }


    /**
     * 开始查询
     *
     * @return {@link List}<{@link ContactEvent}>
     */
    public List<ContactEvent> startQuery() {
        List<ContactEvent> results = new ArrayList<>();
        for(int windowStart = 0; windowStart <= totalTimePoints - widthOfSlidingWindow; windowStart++) {
            // 将第i个窗口的发现的传染源，在i+1个窗口的开始添加到已确定密接对象集合中。
            identifiedContactObjectsId.addAll(tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow]);
            // 1.先开始查询密接对象是否传染其他人
            TreeSet<Integer> tempInfectedObjectIdSet1 = commonQuery(windowStart, identifiedContactObjectsId, results);
            // 2.再开始查询初始传染源是否传染其他人
            TreeSet<Integer> tempInfectedObjectIdSet2 = commonQuery(windowStart, initialInfectiousObjectsId, results, tempInfectedObjectIdSet1);
            // 当前时间窗口内被传染的对象id
            tempInfectedObjectIdSet1.addAll(tempInfectedObjectIdSet2);
            // 记录在当前i时间窗口内被传染的对象id，在i+width开始的时间窗口被密接对象集合identifiedContactObjectsId添加进去。
            int index = windowStart % widthOfSlidingWindow;
            tempInfectedObjectIdInEachWindowArray[index] = tempInfectedObjectIdSet1;
//            System.out.println("LIPMA优化拼接：滑动窗口" + windowStart + "检测完毕！");
        }
        return results;
    }

    private TreeSet<Integer> commonQuery(int windowStart, Set<Integer> sourceObjectIdSet, List<ContactEvent> results) {
        TreeSet<Integer> tempSet = new TreeSet<>(); // 记录当前窗口已被传染的移动对象id
        for(Integer sourceId : sourceObjectIdSet) {
            // 记录当前滑动窗口内每个时间点与传染源距离都小于d的 移动对象Id。
            List<TreeSet<Integer>> possiblyInfectedSetArray = initializepossiblyInfectedSetArray();
            int indexOfArray = 0;
            // 遍历当前窗口内，width个时间点
            for(int timeOfSample = windowStart; timeOfSample < windowStart + widthOfSlidingWindow; timeOfSample++) {
                // 传染源在该时间点的经度
                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
                // 传染源在该时间点的纬度
                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
                // 使用矩形查询
                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
                // 给定点查询，对r树进行范围搜索 queryPoint和distance单位一致。
//                Point queryPoint = Geometries.point(lonOfSource, latOfSource);
//                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryPoint, thresholdOfDistance).toList().toBlocking().single();
                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
                indexOfArray++;
            }
            // 对possiblyInfectedSetArray数组取交集。
            TreeSet<Integer> contactedObjectIdSetInWindow = takeIntersectionOfObjectIdSet(possiblyInfectedSetArray);
            // 当前窗口内的密接对象不能包含前widthOfSlidingWindow个窗口的已经确定的密接对象,因为还没添加到密接对象集合中。
            for(int i = 0; i < widthOfSlidingWindow; i++) {
                contactedObjectIdSetInWindow.removeAll(tempInfectedObjectIdInEachWindowArray[i]);
            }
            // 得到了发生密接事件的objectId集合：contactedObjectIdSet
            int contactTime = windowStart + widthOfSlidingWindow - 1;
            List<ContactEvent> fusionResult = constructContactEvent(sourceId, contactedObjectIdSetInWindow, tempSet, contactTime);
            results.addAll(fusionResult);
            //将这次发生密接事件的id加入到临时已确定密接对象id集合中。
            tempSet.addAll(contactedObjectIdSetInWindow);
        }
        return tempSet;
    }

    private TreeSet<Integer> commonQuery(int windowStart, Set<Integer> sourceObjectIdSet, List<ContactEvent> results,  TreeSet<Integer> tempInfectedObjectIdSet1) {
        TreeSet<Integer> tempSet = new TreeSet<>(); // 记录当前窗口已被传染的移动对象id
        for(Integer sourceId : sourceObjectIdSet) {
            // 记录当前滑动窗口内每个时间点与传染源距离都小于d的 移动对象Id。
            List<TreeSet<Integer>> possiblyInfectedSetArray = initializepossiblyInfectedSetArray();
            int indexOfArray = 0;
            // 遍历当前窗口内，width个时间点
            for(int timeOfSample = windowStart; timeOfSample < windowStart + widthOfSlidingWindow; timeOfSample++) {
                // 传染源在该时间点的经度
                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
                // 传染源在该时间点的纬度
                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
                // 使用矩形查询
                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
                // 给定点查询，对r树进行范围搜索 queryPoint和distance单位一致。
//                Point queryPoint = Geometries.point(lonOfSource, latOfSource);
//                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryPoint, thresholdOfDistance).toList().toBlocking().single();
                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
                indexOfArray++;
            }
            // 对possiblyInfectedSetArray数组取交集。
            TreeSet<Integer> contactedObjectIdSetInWindow = takeIntersectionOfObjectIdSet(possiblyInfectedSetArray);
            // 当前窗口内的密接对象不能包含前widthOfSlidingWindow个窗口的已经确定的密接对象,因为还没添加到密接对象集合中。
            for(int i = 0; i < widthOfSlidingWindow; i++) {
                contactedObjectIdSetInWindow.removeAll(tempInfectedObjectIdInEachWindowArray[i]);
            }
            // 在同一个窗口内初始传染源的移动对象中除去被密接对象传染源的移动对象。
            contactedObjectIdSetInWindow.removeAll(tempInfectedObjectIdSet1);
            // 得到了发生密接事件的objectId集合：contactedObjectIdSet
            int contactTime = windowStart + widthOfSlidingWindow - 1;
            List<ContactEvent> fusionResult = constructContactEvent(sourceId, contactedObjectIdSetInWindow, tempSet, contactTime);
            results.addAll(fusionResult);
            //将这次发生密接事件的id加入到临时已确定密接对象id集合中。
            tempSet.addAll(contactedObjectIdSetInWindow);
        }
        return tempSet;
    }

    /**
     * 构造密接事件
     *
     * @param sourceId 源id
     * @param contactedObjectIdSetInWindow 在窗口中设置被接触对象id
     * @param contactTime 接触时间
     * @return {@link List}<{@link QueryResult}>
     */
    private List<ContactEvent> constructContactEvent(Integer sourceId, TreeSet<Integer> contactedObjectIdSetInWindow, TreeSet<Integer> tempSet, int contactTime) {
        List<ContactEvent> results = new ArrayList<>();
        for(Integer objectId : contactedObjectIdSetInWindow) {
            // 已经记录了该objectId的密接事件
            if(tempSet.contains(objectId)) {
                continue;
            }
            results.add(new ContactEvent(sourceId, objectId, contactTime));
        }
        return results;
    }

    /**
     * 初始化可能受感染集合数组
     * 在每个滑动窗口都需要用到
     *
     * @return {@link List}<{@link HashSet}<{@link Integer}>>
     */
    private List<TreeSet<Integer>> initializepossiblyInfectedSetArray() {
        List<TreeSet<Integer>> result = new ArrayList<>(widthOfSlidingWindow);
        // 初始化列表中的每个HashSet。
        for(int i = 0; i < widthOfSlidingWindow; i++) {
            result.add(new TreeSet<>());
        }
        return result;
    }

    /**
     * 构建查询矩形
     *
     * @param lonOfSource 传染源经度
     * @param latOfSource 传染源纬度
     * @return {@link Rectangle}
     */
    private Rectangle lonAndLatTranformAndFormRectangle(double lonOfSource, double latOfSource) {
        Position from = new Position(latOfSource, lonOfSource);
        Position north = from.predict(thresholdOfDistance, 0);
        Position south = from.predict(thresholdOfDistance, 180);
        Position east = from.predict(thresholdOfDistance, 90);
        Position west = from.predict(thresholdOfDistance, 270);
        return Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat());
    }

    /**
     * 在搜索结果中过滤错误点
     *
     * @param searchResult 搜索结果
     */
    private void filterErrorPointInSearchResult(List<Entry<Integer, Point>> searchResult, double lonOfSource, double latOfSource) {
        PositionPoint sourcePostion = new PositionPoint(0, latOfSource, lonOfSource);
        List<Entry<Integer, Point>> errorPointList = new ArrayList<>();
        for(Entry<Integer, Point> entry : searchResult) {
            // 查询得到点的经度
            double lonOfObject = entry.geometry().x();
            // 查询得到点的纬度
            double latOfObject = entry.geometry().y();
            PositionPoint objectPositon = new PositionPoint(0, latOfObject, lonOfObject);
            // 过滤矩形中不合理的点
            if(HaversineDistance.calculateHaversineDistance(sourcePostion, objectPositon) - thresholdOfDistance > 0) {
                errorPointList.add(entry);
            }
        }
        searchResult.removeAll(errorPointList);
    }

    /**
     * 搜索结果处理
     *
     * @param searchResult 搜索结果
     * @param possiblyInfectedSetArray 可能受感染集合阵列
     * @param indexOfArray 数组索引
     */
    private void searchResltProcessing(List<Entry<Integer, Point>> searchResult, List<TreeSet<Integer>> possiblyInfectedSetArray, int indexOfArray) {
        TreeSet<Integer> tempObjectIdSet = new TreeSet<>();
        for(Entry<Integer, Point> entry : searchResult) {
            // 改点所属的移动对象Id
            Integer objectId = entry.value();
            tempObjectIdSet.add(objectId);
        }
        possiblyInfectedSetArray.set(indexOfArray, tempObjectIdSet);
    }

    /**
     * 取移动对象id集合交集
     *
     * @param possiblyInfectedSetArray 可能受感染集合阵列
     * @return {@link HashSet}<{@link Integer}>
     */
    private TreeSet<Integer> takeIntersectionOfObjectIdSet(List<TreeSet<Integer>> possiblyInfectedSetArray) {
        TreeSet<Integer> result;
        // 如果第一个就为空，则该不符合密接事件的定义，因为需要窗口内所有的时间点都距离小于d
        if(possiblyInfectedSetArray.get(0).isEmpty()) {
            return new TreeSet<>();
        }
        else {
            // 方便后续数组没两个取交集
            result = possiblyInfectedSetArray.get(0);
        }
        for(int i = 1; i < widthOfSlidingWindow; i++) {
            // 处理空的情况
            if(possiblyInfectedSetArray.get(i).isEmpty()){
                // 如果某个为空，则该不符合密接事件的定义，因为需要窗口内所有的时间点都距离小于d
                return new TreeSet<>();
            }
            else {
                // 与下一个取交集
                result.retainAll(possiblyInfectedSetArray.get(i));
            }
        }
        // 已经发生密接事件的id不再记录
        result.removeAll(identifiedContactObjectsId);
        // 移除初始传染源id
        result.removeAll(initialInfectiousObjectsId);
        return result;
    }
}
