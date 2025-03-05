package LM_K_DCQ;

import entity.*;
import utils.HaversineDistance;

import java.util.*;

/**
 * 基于滑动窗口的松散型多源k度密接事件查询算法
 *
 * @author cy
 * @date 2023/11/04
 */
public class LM_DCQ {

    /**
     * 度数
     */
    private int degree;

    /**
     * 滑动窗口宽度
     */
    private int widthOfSlidingWindow; // 默认设置为10

    /**
     * 采样点数量（即轨迹集合中单条轨迹数据最多的值）
     */
    private int totalTimePoints;

    /**
     * 距离阈值
     */
    private double thresholdOfDistance;

    // 时间阈值
    private int thresholdOfTime;

    /**
     * 待分析的移动对象
     *
     * new CopyOnWriteArrayList<>();保证线程安全
     */
    private List<MovingObject> objectsToBeAnalyzed;

    /**
     * 初始传染源对象集合
     */
    private Set<MovingObject> initialInfectiousObjects;

    // 存储确定的密接对象和对应的度数
    private Map<MovingObject, Integer> degreeOfContactedMovingObjects;

    // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
    private Map<MovingObject, Integer>[] tempInfectedObjectIdInEachWindowArray;

    public LM_DCQ() {

    }

    public LM_DCQ(int k, double d, int widthOfSlidingWindow, double ratioOfTime, int totalTimePoints,
                  List<MovingObject> objectsToBeAnalyzed, Set<MovingObject> initialInfectiousObjects) {
        this.degree = k;
        this.thresholdOfDistance = d;
        this.widthOfSlidingWindow = widthOfSlidingWindow;
        this.thresholdOfTime = (int) (widthOfSlidingWindow * ratioOfTime);
        this.totalTimePoints = totalTimePoints;
        this.objectsToBeAnalyzed = objectsToBeAnalyzed;
        this.initialInfectiousObjects = initialInfectiousObjects;
        this.degreeOfContactedMovingObjects = new HashMap<>();
        this.tempInfectedObjectIdInEachWindowArray = new HashMap[widthOfSlidingWindow];
        // 存储w个时间窗口的密接对象id集合，第一个时间窗口的确定的密接对象需要在
        for(int i = 0; i < widthOfSlidingWindow; i++) {
            tempInfectedObjectIdInEachWindowArray[i] = new HashMap<>();
        }
    }

    public List<LooseContactEvent> queryResult() {

        List<LooseContactEvent> looseContactEvents = new ArrayList<>();
        int result = 0;

        List<MovingObject> allSourceObject = new ArrayList<>();
        allSourceObject.addAll(initialInfectiousObjects);


        // 依次处理滑动窗口
        for(int windowStart = 0; windowStart <= totalTimePoints - widthOfSlidingWindow; windowStart++) {
            if(windowStart % 100 == 0)
                System.err.println("当前时间窗口：" + windowStart);
            // 在第i个窗口，需要把在第i-width窗口发生密接事件的移动对象 加入到对应的k度密接对象集合M中。
            degreeOfContactedMovingObjects.putAll(tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow]);
            allSourceObject.addAll(tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow].keySet());

            if(objectsToBeAnalyzed.isEmpty()) {
                break;
            }
            Map<MovingObject, Integer> contactedMovingObjectsInCurrentWindow = new HashMap<>();
            // 每个窗口内都检查一下待分析对象是否被感染
            Iterator<MovingObject> iterator = objectsToBeAnalyzed.iterator();
            while(iterator.hasNext()) {
                MovingObject analyzedObject = iterator.next();
                LooseContactEvent looseContactEvent = commonQuery(iterator, analyzedObject, windowStart, allSourceObject, contactedMovingObjectsInCurrentWindow);
                if(looseContactEvent != null) {
                    looseContactEvents.add(looseContactEvent);
                    result++;
                }
            }
            // 将每个时间窗口的被传染的密接对象记录下来。
            tempInfectedObjectIdInEachWindowArray[windowStart % widthOfSlidingWindow] = contactedMovingObjectsInCurrentWindow;

        }
        System.out.println("queryResult = " + result);
        List<Integer> kdegreeresult = new ArrayList<>();
        for(Map.Entry<MovingObject, Integer> entry : degreeOfContactedMovingObjects.entrySet()) {
            if(entry.getValue() == degree) {
                kdegreeresult.add(entry.getKey().getId());
            }
        }
        System.err.println("k度密接结果：" + kdegreeresult);
        System.out.println("k度密接数量：" + kdegreeresult.size());
        return looseContactEvents;
    }


    private LooseContactEvent commonQuery(Iterator<MovingObject> iterator, MovingObject analyzedObject, int windowStart,
                                          List<MovingObject> allSourceObject,
                                          Map<MovingObject, Integer> contactedMovingObjectsInCurrentWindow) {
        int contactTime = windowStart + widthOfSlidingWindow - 1; // 密接发生的时间
        LooseContactEvent looseContactEvent = new LooseContactEvent(new HashSet<>(), analyzedObject.getId(), contactTime);
        //  记录当前的密接对象的度数
        int currentMaxDegree = 0;
        int count = 0;

        for (int i = windowStart; i < windowStart + widthOfSlidingWindow; i++) {
            int sourceCount = 0;
            for(MovingObject source : allSourceObject) {
                int id = checkIfContact(source, analyzedObject, i);
                if(id != -1) {
                    looseContactEvent.getSourceId().add(id);
                    sourceCount++;
                    currentMaxDegree = Math.max(currentMaxDegree, degreeOfContactedMovingObjects.getOrDefault(source, 0) + 1);
                }
            }
            // 记录滑动窗口内 发生单点密接行为的时间点个数
            if(sourceCount > 0) {
                count++;
            }
        }
        //发生密接关系，加入到当前窗口待处理的密接集合，并且移除待分析集合
        if(count >= thresholdOfTime) {
            contactedMovingObjectsInCurrentWindow.put(analyzedObject, currentMaxDegree);
            iterator.remove();
            return looseContactEvent;
        }
        return null;
    }

    private int checkIfContact(MovingObject source, MovingObject object, int timePoint) {
        // 传染源轨迹数据
        List<PositionPoint> traOfSource = source.getTrajectory().getTra();
        // 待分析移动对象轨迹数据
        List<PositionPoint> traOfObject = object.getTrajectory().getTra();
        // 传染源坐标
        PositionPoint siteOfSource;
        // 待分析移动对象坐标
        PositionPoint siteOfObject;

        siteOfSource = traOfSource.get(timePoint);
        siteOfObject = traOfObject.get(timePoint);
        // 计算两者的欧式距离
        if(HaversineDistance.calculateHaversineDistance(siteOfSource, siteOfObject) - thresholdOfDistance > 0) {
            return -1;
        }
//        looseContactEvent.getSourceId().add(source.getId());
        return source.getId();
    }

}
