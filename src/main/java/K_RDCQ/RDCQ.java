package K_RDCQ;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.*;
import entity.ContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import sun.reflect.generics.tree.Tree;
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

    // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
    private TreeSet<Integer>[] tempInfectedObjectIdInEachWindowArray;

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
        this.identifiedContactObjectsId = new TreeSet<>();
        this.dynamicCandidateSet = new LinkedHashSet<>();
        this.tempInfectedObjectIdInEachWindowArray = new TreeSet[widthOfSlidingWindow];
        // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
        for(int i = 0; i < widthOfSlidingWindow; i++) {
            tempInfectedObjectIdInEachWindowArray[i] = new TreeSet<>();
        }
    }


    /**
     * 首先开始查询被接触对象
     *
     * @return {@link List}<{@link QueryResult}>
     */
    public List<QueryResult> startQuery_contactedObjectFirst() {
        List<QueryResult> results = new ArrayList<>();
        for(int windowStart = 0; windowStart <= totalTimePoints - widthOfSlidingWindow; windowStart++) {
//        for(int windowStart = 0; windowStart < 1; windowStart++) {
            //将时间窗口i内发生的对象源，在i+width开始的时间窗口作为传染源使用
            identifiedContactObjectsId.addAll(tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow]);
            // 1.先开始查询密接对象是否传染其他人
            TreeSet<Integer> tempInfectedObjectIdSet1 = commonQuery(windowStart, identifiedContactObjectsId, results);
            // 2.再开始查询初始传染源是否传染其他人, 但是需要排除在第一步中被传染的移动对象id：tempInfectedObjectIdSet1。
            TreeSet<Integer> tempInfectedObjectIdSet2 = commonQuery(windowStart, initialInfectiousObjectsId, results, tempInfectedObjectIdSet1);
            // 当前时间窗口内被传染的对象id
            tempInfectedObjectIdSet1.addAll(tempInfectedObjectIdSet2);
            // 记录在当前i时间窗口内被传染的对象id，在i+width开始的时间窗口被密接对象集合identifiedContactObjectsId添加进去。
            int index = windowStart % widthOfSlidingWindow;
            tempInfectedObjectIdInEachWindowArray[index] = tempInfectedObjectIdSet1;
//            System.out.println("RDCQ优化：滑动窗口" + windowStart + "检测完毕！");
        }
        return results;
    }

    /**
     * 通用查询被密接对象传染的移动对象的方法
     *
     * @param windowStart 窗口开始
     * @param sourceObjectIdSet 传染源对象id集合
     * @param results 结果
     */
    private TreeSet<Integer> commonQuery(int windowStart, Set<Integer> sourceObjectIdSet,  List<QueryResult> results) {
        TreeSet<Integer> tempSet = new TreeSet<>(); // 记录当前窗口已被传染的移动对象id集合
        for(Integer sourceId : sourceObjectIdSet) {
            // 记录当前滑动窗口内每个时间点与传染源距离都小于d的 移动对象Id。
            List<TreeSet<Integer>> possiblyInfectedSetArray = initializepossiblyInfectedSetArray();
            int indexOfArray = 0;
            // 遍历当前窗口内，width个时间点
//            for(int timeOfSample = windowStart; timeOfSample < windowStart + widthOfSlidingWindow; timeOfSample++) {
//                // 传染源在该时间点的经度
//                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
//                // 传染源在该时间点的纬度
//                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
//                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
//                // 对r树进行范围搜索 queryPoint和distance单位一致。
//                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
//                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
//                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
//                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
//                indexOfArray++;
//            }
            // 第一层过滤点策略
            for(int count = 0; count < 3; count++) {
                //开始点，中间点和末尾时间点
                int timeOfSample;
                if(count == 0) {
                    timeOfSample = windowStart;
                }
                else if(count == 1) {
                    timeOfSample = windowStart + ((widthOfSlidingWindow - 1) / 2);
                }
                else {
                    timeOfSample = windowStart + widthOfSlidingWindow - 1;
                }
                // 传染源在该时间点的经度
                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
                // 传染源在该时间点的纬度
                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
                // 对r树进行范围搜索 queryPoint和distance单位一致。
                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
                indexOfArray++;
            }
            // 对possiblyInfectedSetArray数组取交集。 单个传染源密接的对象id。
            TreeSet<Integer> contactedObjectIdSetInWindow = takeIntersectionOfObjectIdSet(possiblyInfectedSetArray);
            // 距离确认
            caculateDistanceBetweenMayContactObjectsAndSource(contactedObjectIdSetInWindow, sourceId, windowStart);
            // 当前窗口内的密接对象不能包含前widthOfSlidingWindow个窗口的已经确定的密接对象,因为还没添加到密接对象集合中。
            for(int i = 0; i < widthOfSlidingWindow; i++) {
                contactedObjectIdSetInWindow.removeAll(tempInfectedObjectIdInEachWindowArray[i]);
            }
            // 得到了发生密接事件的objectId集合：contactedObjectIdSet
            int contactTime = windowStart + widthOfSlidingWindow - 1;
            List<QueryResult> fusionResult = fusionProcessingDynamicCandidateSet(sourceId, contactedObjectIdSetInWindow, tempSet, contactTime);
            results.addAll(fusionResult);
            //将这次发生密接事件的id加入到临时已确定密接对象id集合中。
            tempSet.addAll(contactedObjectIdSetInWindow);
        }
        return tempSet;
    }

    /**
     * 通用查询被初始传染源传染的移动对象的方法
     *
     * @param windowStart 窗口开始
     * @param sourceObjectIdSet 源对象id集
     * @param results 结果
     * @param tempInfectedObjectIdSet1 临时感染对象id set1
     * @return {@link TreeSet}<{@link Integer}>
     */
    private TreeSet<Integer> commonQuery(int windowStart, Set<Integer> sourceObjectIdSet,  List<QueryResult> results, TreeSet<Integer> tempInfectedObjectIdSet1) {
        TreeSet<Integer> tempSet = new TreeSet<>(); // 记录当前窗口已被传染的移动对象id集合
        for(Integer sourceId : sourceObjectIdSet) {
            // 记录当前滑动窗口内每个时间点与传染源距离都小于d的 移动对象Id。
            List<TreeSet<Integer>> possiblyInfectedSetArray = initializepossiblyInfectedSetArray();
            int indexOfArray = 0;
            // 遍历当前窗口内，width个时间点
//            for(int timeOfSample = windowStart; timeOfSample < windowStart + widthOfSlidingWindow; timeOfSample++) {
//                // 传染源在该时间点的经度
//                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
//                // 传染源在该时间点的纬度
//                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
//                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
//                // 对r树进行范围搜索 queryPoint和distance单位一致。
//                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
//                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
//                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
//                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
//                indexOfArray++;
//            }
            // 第一层过滤点策略
            for(int count = 0; count < 3; count++) {
                //开始点，中间点和末尾时间点
                int timeOfSample;
                if(count == 0) {
                    timeOfSample = windowStart;
                }
                else if(count == 1) {
                    timeOfSample = windowStart + ((widthOfSlidingWindow - 1) / 2);
                }
                else {
                    timeOfSample = windowStart + widthOfSlidingWindow - 1;
                }
                // 传染源在该时间点的经度
                double lonOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLongitude();
                // 传染源在该时间点的纬度
                double latOfSource = allTraOfObjects.get(sourceId - 1).get(timeOfSample).getLatitude();
                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfSource, latOfSource);
                // 对r树进行范围搜索 queryPoint和distance单位一致。
                List<Entry<Integer, Point>> searchResult = rTrees[timeOfSample].search(queryRectangle).toList().toBlocking().single();
                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
                filterErrorPointInSearchResult(searchResult, lonOfSource, latOfSource);
                searchResltProcessing(searchResult, possiblyInfectedSetArray, indexOfArray);
                indexOfArray++;
            }
            // 对possiblyInfectedSetArray数组取交集。 单个传染源密接的对象id。
            TreeSet<Integer> contactedObjectIdSetInWindow = takeIntersectionOfObjectIdSet(possiblyInfectedSetArray);
            // 距离确认
            caculateDistanceBetweenMayContactObjectsAndSource(contactedObjectIdSetInWindow, sourceId, windowStart);
            // 当前窗口内的密接对象不能包含前widthOfSlidingWindow个窗口的已经确定的密接对象,因为还没添加到密接对象集合中。
            for(int i = 0; i < widthOfSlidingWindow; i++) {
                contactedObjectIdSetInWindow.removeAll(tempInfectedObjectIdInEachWindowArray[i]);
            }
            // 在同一个窗口内初始传染源的移动对象中除去被密接对象传染源的移动对象。
            contactedObjectIdSetInWindow.removeAll(tempInfectedObjectIdSet1);
            // 得到了发生密接事件的objectId集合：contactedObjectIdSet
            int contactTime = windowStart + widthOfSlidingWindow - 1;
            List<QueryResult> fusionResult = fusionProcessingDynamicCandidateSet(sourceId, contactedObjectIdSetInWindow, tempSet, contactTime);
            results.addAll(fusionResult);
            //将这次发生密接事件的id加入到临时已确定密接对象id集合中。
            tempSet.addAll(contactedObjectIdSetInWindow);
        }
        return tempSet;
    }

    /**
     * 计算可能密接对象与源之间距离
     *
     * @param contactedObjectIdSetInWindow 在窗口中设置被接触对象id
     * @param sourceId 源id
     * @param windowStart windowstart
     * @return {@link TreeSet}<{@link Integer}>
     */
    private void caculateDistanceBetweenMayContactObjectsAndSource(TreeSet<Integer> contactedObjectIdSetInWindow, Integer sourceId, int windowStart) {
        for(int i = windowStart + 1; i < windowStart + widthOfSlidingWindow - 1; i++) {
            // 获取传染源的此时刻的位置
            PositionPoint sourcePosition = allTraOfObjects.get(sourceId - 1).get(i);
            if(i == windowStart + ((widthOfSlidingWindow - 1) / 2)) { // 跳过中间抽样点的检查
                continue;
            }
            // 检查每个可能密接对象在窗口内每个时刻都满足要求
            Iterator<Integer> iterator = contactedObjectIdSetInWindow.iterator();
            while(iterator.hasNext()) {
                Integer objectId = iterator.next();
                PositionPoint objectPosition = allTraOfObjects.get(objectId - 1).get(i);
                if(HaversineDistance.calculateHaversineDistance(sourcePosition, objectPosition) - thresholdOfDistance > 0) {
                    iterator.remove();
                }
            }
//            contactedObjectIdSetInWindow = result;
        }
//        return contactedObjectIdSetInWindow;
    }

    /**
     * 融合处理动态候选集
     *
     * @param sourceId 传染源id集合
     * @param contactedObjectIdSet 密接对象id集合
     * @param contactTime 密接时间
     * @return {@link List}<{@link QueryResult}>
     */
    private List<QueryResult> fusionProcessingDynamicCandidateSet(Integer sourceId, TreeSet<Integer> contactedObjectIdSet, TreeSet<Integer> tempSet, int contactTime) {
        List<QueryResult> fusionResultWithDegree = new ArrayList<>();
        // 1.判断sourceId是否属于初始传染源。
        if(initialInfectiousObjectsId.contains(sourceId)) {
            // 遍历处理全部的感染者
            for(Integer objectId : contactedObjectIdSet) {
                // 已经记录了该objectId的密接事件
                if(tempSet.contains(objectId)) {
                    continue;
                }
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
                // 已经记录了该objectId的密接事件
                if(tempSet.contains(objectId)) {
                    continue;
                }
                for(List<Map<String, Integer>> cand : dynamicCandidateSet) {
                    int candSize = cand.size(); // 当前cand中路径的长度（即传染源和密接对象的个数）
                    // 1)先判断是否是最后一个节点为source,且当前路径长度为k个，加上当前新节点则满足为k度密接定义（即一个初始传染源，k个密接对象），则构建k度密接事件加入到结果集合中。
                    if(cand.get(candSize - 1).get("id").equals(sourceId) && candSize == degree) {
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
    private List<TreeSet<Integer>> initializepossiblyInfectedSetArray() {
        List<TreeSet<Integer>> result = new ArrayList<>();
        // 初始化列表中的每个HashSet。
        for(int i = 0; i < 3; i++) {
            result.add(new TreeSet<>());
        }
        return result;
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
        for(int i = 1; i < 3; i++) {
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
}
