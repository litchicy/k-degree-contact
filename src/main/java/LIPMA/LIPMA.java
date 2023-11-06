package LIPMA;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import entity.ContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
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
        this.identifiedContactObjectsId = new HashSet<>();

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
        for(int windowStart = 0; windowStart < totalTimePoints - widthOfSlidingWindow; windowStart++) {
            // 1.先开始查询密接对象是否传染其他人
            commonQuery(windowStart, identifiedContactObjectsId, results);
            // 2.再开始查询初始传染源是否传染其他人
            commonQuery(windowStart, initialInfectiousObjectsId, results);
            System.out.println("cy拼接 windows" + windowStart + "检测完毕！");
        }
        return results;
    }

    private void commonQuery(int windowStart, Set<Integer> sourceObjectIdSet, List<ContactEvent> results) {
        Set<Integer> tempSet = new HashSet<>();
        for(Integer sourceId : sourceObjectIdSet) {
            // 记录当前滑动窗口内每个时间点与传染源距离都小于d的 移动对象Id。
            List<HashSet<Integer>> possiblyInfectedSetArray = initializepossiblyInfectedSetArray();
            int indexOfArray = 0;
            // 遍历当前窗口内，width个时间点
            for(int timeOfSample = windowStart; timeOfSample < windowStart + widthOfSlidingWindow; timeOfSample++) {
                // 传染源在该时间点的经度
                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
                // 传染源在该时间点的纬度
                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
//                Point queryPoint = Geometries.point(lonOfSource, latOfSource);
//                Circle queryRectangle = Geometries.circle(lonOfSource, latOfSource, thresholdOfDistance);
                // 对r树进行范围搜索 queryPoint和distance单位一致。
                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
//                List<Entry<Integer, Rectangle>> searchResult = rTrees[timeOfSample].search(queryRectangle, thresholdOfDistance).toList().toBlocking().single();
                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
                indexOfArray++;
            }
            // 对possiblyInfectedSetArray数组取交集。
            HashSet<Integer> contactedObjectIdSetInWindow = takeIntersectionOfObjectIdSet(possiblyInfectedSetArray);
            //将这次发生密接事件的id加入到临时已确定密接对象id集合中。
            tempSet.addAll(contactedObjectIdSetInWindow);
            // 得到了发生密接事件的objectId集合：contactedObjectIdSet
            int contactTime = windowStart + widthOfSlidingWindow - 1;
            List<ContactEvent> fusionResult = constructContactEvent(sourceId, contactedObjectIdSetInWindow, contactTime);
            results.addAll(fusionResult);
        }
        //将该窗口内密接事件的id加入到已确定密接对象id集合中
        identifiedContactObjectsId.addAll(tempSet);

    }

    /**
     * 构造密接事件
     *
     * @param sourceId 源id
     * @param contactedObjectIdSetInWindow 在窗口中设置被接触对象id
     * @param contactTime 接触时间
     * @return {@link List}<{@link QueryResult}>
     */
    private List<ContactEvent> constructContactEvent(Integer sourceId, HashSet<Integer> contactedObjectIdSetInWindow, int contactTime) {
        List<ContactEvent> results = new ArrayList<>();
        for(Integer objecetId : contactedObjectIdSetInWindow) {
            results.add(new ContactEvent(sourceId, objecetId, contactTime));
        }
        return results;
    }

    /**
     * 初始化可能受感染集合数组
     * 在每个滑动窗口都需要用到
     *
     * @return {@link List}<{@link HashSet}<{@link Integer}>>
     */
    private List<HashSet<Integer>> initializepossiblyInfectedSetArray() {
        List<HashSet<Integer>> result = new ArrayList<>(widthOfSlidingWindow);
        // 初始化列表中的每个HashSet。
        for(int i = 0; i < widthOfSlidingWindow; i++) {
            result.add(new HashSet<>());
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
//                System.err.println("error entry.value() = " + entry.value());
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
    private void searchResltProcessing(List<Entry<Integer, Point>> searchResult, List<HashSet<Integer>> possiblyInfectedSetArray, int indexOfArray) {
        HashSet<Integer> tempObjectIdSet = new HashSet<>();
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
    private HashSet<Integer> takeIntersectionOfObjectIdSet(List<HashSet<Integer>> possiblyInfectedSetArray) {
        HashSet<Integer> result;
        // 如果第一个就为空，则该不符合密接事件的定义，因为需要窗口内所有的时间点都距离小于d
        if(possiblyInfectedSetArray.get(0).isEmpty()) {
            return new HashSet<>();
        }
        else {
            // 方便后续数组没两个取交集
            result = possiblyInfectedSetArray.get(0);
        }
        for(int i = 1; i < widthOfSlidingWindow; i++) {
            // 处理空的情况
            if(possiblyInfectedSetArray.get(i).isEmpty()){
                // 如果某个为空，则该不符合密接事件的定义，因为需要窗口内所有的时间点都距离小于d
                return new HashSet<>();
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
