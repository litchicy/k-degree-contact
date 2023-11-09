package LIPMA;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import entity.ContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import utils.HaversineDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * chenyu的基本方案，滑动窗口进行k度拼接
 *
 * @author cy
 * @date 2023/11/07
 */
public class BASE_LIPMA {

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
     * 待分析的移动对象
     *
     * new CopyOnWriteArrayList<>();保证线程安全
     */
    private List<MovingObject> objectsToBeAnalyzed;

    /**
     * 初始传染源对象集合
     */
    private Set<MovingObject> initialInfectiousObjects;

    /**
     * 初始传染源id集合
     */
    private Set<Integer> initialInfectiousSourcesId;

    /**
     * 已确定的密接对象
     */
    private Set<MovingObject> identifiedContactObjects;

    public BASE_LIPMA() {

    }

    public BASE_LIPMA(double d, int widthOfSlidingWindow, int totalTimePoints,
                      List<MovingObject> objectsToBeAnalyzed, Set<MovingObject> initialInfectiousObjects, Set<Integer> initialInfectiousSourcesId) {
        this.thresholdOfDistance = d;
        this.widthOfSlidingWindow = widthOfSlidingWindow;
        this.totalTimePoints = totalTimePoints;
        this.objectsToBeAnalyzed = objectsToBeAnalyzed;
        this.initialInfectiousObjects = initialInfectiousObjects;
        this.initialInfectiousSourcesId = initialInfectiousSourcesId;
        this.identifiedContactObjects = new TreeSet<>();
    }

    /**
     * 开始查询
     *
     * @return {@link List}<{@link ContactEvent}>
     */
    public List<ContactEvent> startQuery() {
        List<ContactEvent> result = new ArrayList<>();
        for(int windowStart = 0; windowStart < totalTimePoints - widthOfSlidingWindow; windowStart++) {
            if(objectsToBeAnalyzed.isEmpty()) {
                break;
            }
            Set<MovingObject> contactedMovingObjectsInCurrentWindow = new TreeSet<>();
            // 每个窗口内查询全部的待分析移动对象
            for(MovingObject analyzedObject : objectsToBeAnalyzed) {
                // 1.先与已经确定的密接对象进行判断
                boolean ifContact = commonQuery(identifiedContactObjects, contactedMovingObjectsInCurrentWindow, analyzedObject, windowStart, result);
                // 发生密接事件，则不需要与确定的密接对象进行判断，直接分析下一个待分析对象。
                if(ifContact) {
                    continue;
                }
                // 2.再与初始传染源进行判断
                commonQuery(initialInfectiousObjects, contactedMovingObjectsInCurrentWindow, analyzedObject, windowStart, result);
            }
            // 将每个窗口内的密接对象一次性添加到已确定的密接对象集合中。
            identifiedContactObjects.addAll(contactedMovingObjectsInCurrentWindow);
//            System.out.println("LIPMA_BASE基本：滑动窗口" + windowStart + "检查完毕！");
        }
        return result;
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

            if (currentSequence.isEmpty() && (initialInfectiousSourcesId.contains(currentEvent.getSourceId()))) {
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
     * 通用查询
     *
     * @param sourceSet 源设置
     * @param contactedMovingObjectsInCurrentWindow 接触当前窗口中移动对象
     * @param analyzedObject 分析对象
     * @param windowStart 窗口开始
     * @param result 结果
     * @return boolean
     */
    private boolean commonQuery(Set<MovingObject> sourceSet, Set<MovingObject> contactedMovingObjectsInCurrentWindow,
                                MovingObject analyzedObject, int windowStart, List<ContactEvent> result) {
        ContactEvent contactEvent = null;
        for(MovingObject source : sourceSet) {
            // 判断是否发发生密接事件
            contactEvent = checkIfContact(source, analyzedObject, windowStart);
            if(contactEvent != null) { // 发生密接事件
                // 将object移入密接对象集合中
                moveToIdentifiedContactObjectsSet(analyzedObject, contactedMovingObjectsInCurrentWindow);
                result.add(contactEvent);
                // 一旦发生密接事件，则返回结果为true
                return true;
            }
        }
        // 未发生密接事件
        return false;
    }

    /**
     * 检查是否密接
     *
     * @param source 源
     * @param object 对象
     * @param windowStart 窗口开始
     * @return {@link ContactEvent}
     */
    private ContactEvent checkIfContact(MovingObject source, MovingObject object, int windowStart) {
        // 传染源轨迹数据
        List<PositionPoint> traOfSource = source.getTrajectory().getTra();
        // 待分析移动对象轨迹数据
        List<PositionPoint> traOfObject = object.getTrajectory().getTra();
        // 传染源坐标
        PositionPoint siteOfSource;
        // 待分析移动对象坐标
        PositionPoint siteOfObject;
        // 密接判断结果
        boolean flag = true;
        // 检查当前窗口两个对象内是否发生密接事件
        for (int i = windowStart; i < windowStart + widthOfSlidingWindow; i++) {
            siteOfSource = traOfSource.get(i);
            siteOfObject = traOfObject.get(i);
            // 计算两者的欧式距离一旦不满足条件，跳出检测
            if(HaversineDistance.calculateHaversineDistance(siteOfSource, siteOfObject) - thresholdOfDistance > 0) {
                flag = false;
                break;
            }
        }
//        System.out.println(source.getId() + "号传染源和" + object.getId() + "号移动对象，在窗口" + windowId + "内检查完毕。结果为：" + contactResult);
        if(flag) {
            return new ContactEvent(source.getId(), object.getId(), windowStart + widthOfSlidingWindow - 1);
        }
        return null;
    }

    /**
     * 将确定的密接对象移入密接对象集合中
     *
     * @param object 对象
     */
    private void moveToIdentifiedContactObjectsSet(MovingObject object, Set<MovingObject> contactedMovingObjectsInCurrentWindow) {
        // 添加到密接对象集合中
        contactedMovingObjectsInCurrentWindow.add(object);
        // 从待分析对象中移除
        objectsToBeAnalyzed.remove(object);
    }

}
