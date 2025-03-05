package LM_K_RDCQ;

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
import utils.HaversineDistance;

import java.util.*;

public class LM_RDCQ {

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
     * 时间松弛阈值
     */
    private int thresholdOfTime;

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
    private Map<Integer, Integer> degreeOfContactedMovingObjects;

    /**
     * 待分析的移动对象
     *
     * new CopyOnWriteArrayList<>();保证线程安全
     */
    private Set<Integer> objectsToBeAnalyzed;

    /**
     * 动态候选集合candlist
     * LinkedHashSet 保证插入顺序
     *
     * map<Integer, Integer>第一个表示密接对象id，第二个表示密接事件发生的时间</>
     */
//    private Set<List<Map<String, Integer>>> dynamicCandidateSet;

    // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
    private Map<Integer, Integer>[] tempInfectedObjectIdInEachWindowArray;

    /**
     * 全部移动对象的所有时间采样点的r树
     */
    private RTree<Integer, Point>[] rTrees;

    public LM_RDCQ() {

    }

    public LM_RDCQ(int k, double d, int widthOfSlidingWindow, double ratioOfTime,
                   int totalTimePoints,
                   RTree<Integer, Point>[] rTrees,
                   List<List<PositionPoint>> allTraOfObjects,
                   Set<Integer> objectsToBeAnalyzed,
                   Set<Integer> initialInfectiousObjectsId) {
        this.degree = k;
        this.thresholdOfDistance = d;
        this.widthOfSlidingWindow = widthOfSlidingWindow;
        this.totalTimePoints = totalTimePoints;
        this.thresholdOfTime = (int) (widthOfSlidingWindow * ratioOfTime);
        this.rTrees = rTrees;
        this.allTraOfObjects = allTraOfObjects;
        this.objectsToBeAnalyzed = objectsToBeAnalyzed;
        this.initialInfectiousObjectsId = initialInfectiousObjectsId;
        this.degreeOfContactedMovingObjects = new HashMap<>();
//        this.dynamicCandidateSet = new LinkedHashSet<>();
        this.tempInfectedObjectIdInEachWindowArray = new HashMap[widthOfSlidingWindow];
        // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
        for(int i = 0; i < widthOfSlidingWindow; i++) {
            tempInfectedObjectIdInEachWindowArray[i] = new HashMap<>();
        }
    }

    public List<LooseContactEvent> queryResult() {

        List<LooseContactEvent> looseContactEvents = new ArrayList<>();
        int result = 0;

        Set<Integer> allSourceObject = new HashSet<>(initialInfectiousObjectsId);


        // 依次处理滑动窗口
        for(int windowStart = 0; windowStart <= totalTimePoints - widthOfSlidingWindow; windowStart++) {
            if(windowStart % 100 == 0)
                System.err.println("当前时间窗口：" + windowStart);
            // 在第i个窗口，需要把在第i-width窗口发生密接事件的移动对象 加入到对应的k度密接对象集合M中。
            degreeOfContactedMovingObjects.putAll(tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow]);
            allSourceObject.addAll(tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow].keySet());

            Map<Integer, Integer> contactedMovingObjectsInCurrentWindow = new HashMap<>();
            List<LooseContactEvent> events = commonQuery(windowStart, objectsToBeAnalyzed, allSourceObject, contactedMovingObjectsInCurrentWindow);
            looseContactEvents.addAll(events);
            result += events.size();

            // 将每个时间窗口的被传染的密接对象记录下来。
            tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow] = contactedMovingObjectsInCurrentWindow;

        }
        System.out.println("queryResult = " + result);
        List<Integer> kdegreeresult = new ArrayList<>();
        for(Map.Entry<Integer, Integer> entry : degreeOfContactedMovingObjects.entrySet()) {
            if(entry.getValue() == degree) {
                kdegreeresult.add(entry.getKey());
            }
        }
        System.err.println("k度密接结果：" + kdegreeresult );
        System.out.println("k度密接数量：" + kdegreeresult.size());
        return looseContactEvents;
    }

    private List<LooseContactEvent> commonQuery(int windowStart, Set<Integer> objectsToBeAnalyzed,
                             Set<Integer> allSourceObject,
                             Map<Integer, Integer> contactedMovingObjectsInCurrentWindow) {
        List<LooseContactEvent> results = new ArrayList<>();
        Iterator<Integer> iterator = objectsToBeAnalyzed.iterator();
        int contactTime = windowStart + widthOfSlidingWindow - 1; // 密接发生的时间
        while(iterator.hasNext()) {
            Integer objectId = iterator.next();
            LooseContactEvent looseContactEvent = new LooseContactEvent(new HashSet<>(), objectId, contactTime);
            TreeSet<Integer> onePointSourceIdSet = new TreeSet<>();
            // 记录窗口内非空的时间点个数
            int count = 0;
            for(int i = windowStart;  i < windowStart + widthOfSlidingWindow; i++) {
                double lonOfObject = allTraOfObjects.get(objectId - 1).get(i).getLongitude();
                double latOfObject = allTraOfObjects.get(objectId - 1).get(i).getLatitude();
                Rectangle queryRectangle = lonAndLatTranformAndFormRectangle(lonOfObject, latOfObject);
                List<Entry<Integer, Point>> searchResult = rTrees[i].search(queryRectangle).toList().toBlocking().single();
                TreeSet<Integer> tempSourceSet = searchResltProcessing(searchResult, allSourceObject);
                // 对矩形选取的点需要做筛选，因为矩形对角的距离是大于d的。
                filterErrorPointInSearchResult(tempSourceSet, lonOfObject, latOfObject, i);
                // 当前时间点存在传染源 发生密接行为
                if(!tempSourceSet.isEmpty()) {
                    onePointSourceIdSet.addAll(tempSourceSet);
                    count++;
                }
            }
            if(count >= thresholdOfTime) {
                iterator.remove();
                looseContactEvent.getSourceId().addAll(onePointSourceIdSet);
                // 查询出来的rectangle中 必定包含本身这个点
                looseContactEvent.getSourceId().remove(objectId);
                int currentMaxDegree = 0;
                for(Integer sourceId : looseContactEvent.getSourceId()) {
                    currentMaxDegree = Math.max(currentMaxDegree, degreeOfContactedMovingObjects.getOrDefault(sourceId, 0) + 1);
                }
                contactedMovingObjectsInCurrentWindow.put(objectId, currentMaxDegree);
                results.add(looseContactEvent);
            }
        }
        return results;
    }

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
    private void filterErrorPointInSearchResult(Set<Integer> searchResult, double lonOfObject, double latOfObject, int time) {
        PositionPoint objectPostion = new PositionPoint(0, latOfObject, lonOfObject);
        Iterator<Integer> iterator = searchResult.iterator();
        while(iterator.hasNext()) {
            Integer sourceId = iterator.next();
            double lonOfSource = allTraOfObjects.get(sourceId - 1).get(time).getLongitude();
            double latOfSource= allTraOfObjects.get(sourceId - 1).get(time).getLatitude();
            PositionPoint sourcePositon = new PositionPoint(0, latOfSource, lonOfSource);
            // 过滤矩形中不合理的点
            if(HaversineDistance.calculateHaversineDistance(sourcePositon, objectPostion) - thresholdOfDistance > 0) {
                iterator.remove();
            }
        }
    }

    /**
     * 搜索结果处理
     *
     * @param searchResult 搜索结果
     *
     */
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



    /**
     * 初始化可能受感染集合数组
     * 在每个滑动窗口都需要用到
     *
     * @return {@link List}<{@link HashSet}<{@link Integer}>>
     */
    private List<TreeSet<Integer>> initializepossiblyInfectedSetArray() {
        List<TreeSet<Integer>> result = new ArrayList<>();
        // 初始化列表中的每个HashSet。
        for(int i = 0; i < widthOfSlidingWindow; i++) {
            result.add(new TreeSet<>());
        }
        return result;
    }

}
