package K_RDCQ;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.*;
import entity.ContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import utils.HaversineDistance;

import java.beans.Introspector;
import java.util.*;


public class RDCQ {

    /**
     * 地球半径km
     */
    private final double EARTH_RADIUM = 6371.0;

    /**
     * 度数
     */
    private int degree;

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
     * 动态候选集合candlist
     * LinkedHashSet 保证插入顺序
     *
     * map<Integer, Integer>第一个表示密接对象id，第二个表示密接事件发生的时间</>
     */
    private Set<List<Map<String, Integer>>> dynamicCandidateSet;

    /**
     * 全部移动对象的所有时间采样点的r树
     */
    private RTree<Integer, Point>[] rTrees;

    public RDCQ() {

    }

    public RDCQ(int k, double d, int widthOfSlidingWindow, int totalTimePoints,
                RTree<Integer, Point>[] rTrees, List<List<PositionPoint>> allTraOfObjects, Set<Integer> initialInfectiousObjectsId) {
        this.degree = k;
        this.thresholdOfDistance = d;
        this.widthOfSlidingWindow = widthOfSlidingWindow;
        this.totalTimePoints = totalTimePoints;
        this.rTrees = rTrees;
        this.allTraOfObjects = allTraOfObjects;
        this.initialInfectiousObjectsId = initialInfectiousObjectsId;
        this.identifiedContactObjectsId = new HashSet<>();
        this.dynamicCandidateSet = new LinkedHashSet<>();
    }


    /**
     * 首先开始查询被接触对象
     *
     * @return {@link List}<{@link QueryResult}>
     */
    public List<QueryResult> startQuery_contactedObjectFirst() {
        List<QueryResult> results = new ArrayList<>();

        for(int windowStart = 0; windowStart < totalTimePoints - widthOfSlidingWindow; windowStart++) {
//        for(int windowStart = 0; windowStart < 1; windowStart++) {
            // 1.先开始查询密接对象是否传染其他人
            commonQuery(windowStart, identifiedContactObjectsId, results);
            // 2.再开始查询初始传染源是否传染其他人
            commonQuery(windowStart, initialInfectiousObjectsId, results);
            System.out.println("滑动窗口" + windowStart + "检测完毕！");
        }
        return results;
    }

    /**
     * 通用查询方法
     *
     * @param windowStart 窗口开始
     * @param sourceObjectIdSet 传染源对象id集合
     * @param results 结果
     */
    private void commonQuery(int windowStart, Set<Integer> sourceObjectIdSet,  List<QueryResult> results) {
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
            List<QueryResult> fusionResult = fusionProcessingDynamicCandidateSet(sourceId, contactedObjectIdSetInWindow, contactTime);
            results.addAll(fusionResult);
        }
        //将该窗口内密接事件的id加入到已确定密接对象id集合中
        identifiedContactObjectsId.addAll(tempSet);
    }

    /**
     * 融合处理动态候选集
     *
     * @param sourceId 传染源id集合
     * @param contactedObjectIdSet 密接对象id集合
     * @param contactTime 密接时间
     * @return {@link List}<{@link QueryResult}>
     */
    private List<QueryResult> fusionProcessingDynamicCandidateSet(Integer sourceId, HashSet<Integer> contactedObjectIdSet, int contactTime) {
        List<QueryResult> fusionResultWithDegree = new ArrayList<>();
        // 1.判断sourceId是否属于初始传染源。
        if(initialInfectiousObjectsId.contains(sourceId)) {
            // 遍历处理全部的感染者
            for(Integer objectId : contactedObjectIdSet) {
                // 查询1度密接情况需要特殊处理
                if(degree == 1) {
                    // 构建密接事件
                    ContactEvent contactEvent = new ContactEvent(sourceId, objectId, contactTime);
                    List<ContactEvent> contactEventList = new ArrayList<>();
                    // 密接事件添加到k度密接事件组内
                    contactEventList.add(contactEvent);
                    fusionResultWithDegree.add(new QueryResult(sourceId, objectId, contactEventList, degree)); // 返回结果
                }
                // 创建新的路径候选序列，如果密接事件传染源是初始传染源。
                List<Map<String, Integer>> newCandPath = new ArrayList<>();
                // 路径候选序列中第一个节点是初始传染源，密接时间默认为0。
                Map<String, Integer> newSourceNode = new HashMap<>();
                newSourceNode.put("id", sourceId);
                newSourceNode.put("time", 0);
                newCandPath.add(newSourceNode);
                // 路径候选序列中第二个节点为密接对象，密接时间为窗口的最后一个时间点。
                Map<String, Integer> newContactObjectNode = new HashMap<>();
                newContactObjectNode.put("id", objectId);
                newContactObjectNode.put("time", contactTime);
                newCandPath.add(newContactObjectNode);
                // 将新的候选序列cand加入到动态候选序列中。
                dynamicCandidateSet.add(newCandPath);
            }
            return fusionResultWithDegree;
        }
        // 2.source不属于初始传染源，则需要进行candlist遍历，寻找path中含有source的cand。
        else {
            Set<List<Map<String, Integer>>> newCandidateToAdd = new LinkedHashSet<>();
            for(Integer objectId : contactedObjectIdSet) {
                for(List<Map<String, Integer>> cand : dynamicCandidateSet) {
                    int candSize = cand.size(); // 当前cand中路径的长度（即传染源和密接对象的个数）
                    // 1)先判断是否是最后一个节点为source,且当前路径长度为k个，加上当前新节点则满足为k度密接定义（即一个初始传染源，k个密接对象），则构建k度密接事件加入到结果集合中。
                    if(cand.get(candSize - 1).get("id").equals(sourceId) && candSize == degree) {
//                    System.out.println("cand = " + cand);
//                    System.out.println("cand.get(candSize - 1) = " + cand.get(candSize - 1));
//                    System.err.println("source.getId() = " + source.getId());
                        List<ContactEvent> k_degree_contactPath = new ArrayList<>();
                        for(int i = 0; i < candSize; i++) {
                            int formerId = cand.get(i).get("id"); // 密接事件中传染源id
                            int latterId; // 密接事件中被传染者id
                            int time; // 密接事件发生的时间
                            if(i + 1 != candSize ) {// 遍历到候选路径中最后一个节点的情况
                                latterId = cand.get(i + 1).get("id");
                                time = cand.get(i + 1).get("time");
                            }
                            else {
                                latterId = objectId;
                                time = contactTime;
                            }
                            ContactEvent contactEvent = new ContactEvent(formerId, latterId, time);
//                        System.err.println("contactEvent = " + contactEvent);
                            k_degree_contactPath.add(contactEvent);
                        }
                        // 返回符合k度密接事件定义的结果。
                        fusionResultWithDegree.add(new QueryResult(cand.get(0).get("id"), objectId, k_degree_contactPath, degree));
                    }
                    // 2) source是最后一个节点，但是长度不足为k
                    else if(cand.get(candSize - 1).get("id").equals(sourceId)) {
                        Map<String, Integer> newNode = new HashMap<>();
                        newNode.put("id", objectId);
                        newNode.put("time", contactTime);
                        cand.add(newNode);
                    }
                    // 3)是中间节点，则必定不符合k度密接事件的定义，长度不为k。因为候选序列中路径长度都是小于k的。
                    else {
                        for(int i = 1; i < candSize - 1; i++) {
                            Map<String, Integer> tempNode = cand.get(i);
                            // 存在这样的路径path含有sourceId值的节点，则进行更新
                            if(tempNode.get("id").equals(sourceId)) {
                                List<Map<String, Integer>> newCandPath = new ArrayList<>();
                                // 将查找到的cand中的前i个节点添加到newCand中。
                                for(int j = 0; j < i + 1; j++) {
                                    newCandPath.add(cand.get(j));
                                }
                                // 将新的contactNode添加到newCand中。
                                Map<String, Integer> newContactNode = new HashMap<>();
                                newContactNode.put("id", objectId);
                                newContactNode.put("time", contactTime);
                                newCandPath.add(newContactNode);
                                newCandidateToAdd.add(newCandPath);
                            }
                        }
                    }
                }
            }
            dynamicCandidateSet.addAll(newCandidateToAdd);
        }
        return fusionResultWithDegree;
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
            // System.out.println("objectId = " + objectId);
            Geometry geometry = entry.geometry();
            // System.out.println("geometry = " + geometry.toString());
            tempObjectIdSet.add(objectId);
//            if(geometry.distance(queryPoint) < thresholdOfDistance) {
//                // 将可能发生密接事件的id加入到集合中
//                tempObjectIdSet.add(objectId);
//            }
        }
        possiblyInfectedSetArray.set(indexOfArray, tempObjectIdSet);
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
                System.err.println("error entry.value() = " + entry.value());
                errorPointList.add(entry);
            }
        }
        searchResult.removeAll(errorPointList);
    }
}
