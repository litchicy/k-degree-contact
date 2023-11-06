package K_DCQ;

import entity.ContactEvent;
import entity.MovingObject;
import entity.PositionPoint;
import entity.QueryResult;
import utils.HaversineDistance;
import utils.Utils;

import java.util.*;

/**
 * 基于滑动窗口的k度密接事件查询算法
 *
 * @author cy
 * @date 2023/11/04
 */
public class DCQ {

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
     * 已确定的密接对象
     */
    private Set<MovingObject> identifiedContactObjects;

    /**
     * 动态候选集合candlist
     * LinkedHashSet 保证插入顺序
     *
     * map<Integer, Integer>第一个表示密接对象id，第二个表示密接事件发生的时间</>
     */
    private Set<List<Map<String, Integer>>> dynamicCandidateSet;

    public DCQ() {

    }

    public DCQ(int k, double d, int widthOfSlidingWindow, int totalTimePoints,
               List<MovingObject> objectsToBeAnalyzed, Set<MovingObject> initialInfectiousObjects) {
        this.degree = k;
        this.thresholdOfDistance = d;
        this.widthOfSlidingWindow = widthOfSlidingWindow;
        this.totalTimePoints = totalTimePoints;
        this.objectsToBeAnalyzed = objectsToBeAnalyzed;
        this.initialInfectiousObjects = initialInfectiousObjects;
        this.identifiedContactObjects = new LinkedHashSet<>();
        this.dynamicCandidateSet = new LinkedHashSet<>();
    }

    /**
     * 查询算法，每个待分析对象先与已确定的密接对象进行密接判断
     *
     * @return {@link List}<{@link QueryResult}>
     */
    public List<QueryResult> startQuery_compareWithContactObjectsFirst() {

        List<QueryResult> result = new ArrayList<>();
        // 依次处理滑动窗口
        for(int windowStart = 0; windowStart < totalTimePoints - widthOfSlidingWindow; windowStart++) {
//        for(int windowStart = 0; windowStart < 1; windowStart++) {
            // 不存在待分析对象，结束时间窗口的循环
            if(objectsToBeAnalyzed.isEmpty()) {
                break;
            }
            Set<MovingObject> contactedMovingObjectsInCurrentWindow = new LinkedHashSet<>();
            // 每个窗口内查询全部的待分析移动对象
            for(MovingObject analyzedObject : objectsToBeAnalyzed) {
                // 1.先与已经确定的密接对象进行判断
                boolean ifContact = commonQueryProcessing(identifiedContactObjects, contactedMovingObjectsInCurrentWindow, analyzedObject, windowStart, result);
                // 发生密接事件，则不需要与确定的密接对象进行判断，直接分析下一个待分析对象。
                if(ifContact) {
                    continue;
                }
                // 2.再与初始传染源进行判断
                commonQueryProcessing(initialInfectiousObjects, contactedMovingObjectsInCurrentWindow, analyzedObject, windowStart, result);
            }
            // 将每个窗口内的密接对象一次性添加到已确定的密接对象集合中。
            identifiedContactObjects.addAll(contactedMovingObjectsInCurrentWindow);
            System.out.println("window " + windowStart + "检查完毕！");
        }
        return result;
    }

    /**
     * 公共查询处理
     * 以单个移动对象进行分析
     *
     * @param sourceSet 传染源对象集合
     * @param analyzedObject 待分析对象
     * @param windowStart 窗口开始
     * @param result 结果
     * @return boolean
     */
    private boolean commonQueryProcessing(Set<MovingObject> sourceSet, Set<MovingObject> contactedMovingObjectsInCurrentWindow,
                                          MovingObject analyzedObject, int windowStart, List<QueryResult> result) {
        ContactEvent contactEvent = null;
        int contactTime = windowStart + widthOfSlidingWindow - 1; // 密接发生的时间
        for(MovingObject source : sourceSet) {
            // 判断是否发发生密接事件
            contactEvent = checkIfContact(source, analyzedObject, windowStart);
            if(contactEvent != null) { // 发生密接事件
//                System.out.println("contactEvent = " + contactEvent);
                moveToIdentifiedContactObjectsSet(analyzedObject, contactedMovingObjectsInCurrentWindow);
//                System.out.println("contactEvent = " + contactEvent);
                // 与动态候选序列进行融合处理
                QueryResult oneResult = fusionProcessingDynamicCandidateSet(source, analyzedObject, contactTime);
                if(oneResult != null) {
                    // 结果符合k度密接定义，加入到结果集合。
//                    System.err.println("oneResult = " + oneResult);
                    result.add(oneResult);
                }
                // 一旦发生密接事件，则返回结果为true
                return true;
            }
        }
        // 未发生密接事件
        return false;
    }

    /**
     * 检查是否接触
     * 检查在滑动窗口内是否发生密接事件
     *
     * @param windowStart 窗口开始时间点
     * @param source 传染源
     * @param object 待分析对象
     * @return ContactEvent
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


    /**
     * 融合处理动态候选集
     *
     * @param source 传染源源
     * @param object 密接对象
     * @param contactTime 密接事件发生的时间
     * @return {@link QueryResult}
     */
    private QueryResult fusionProcessingDynamicCandidateSet(MovingObject source, MovingObject object, int contactTime) {
        // 1.判断source是否属于初始传染源。
        if(initialInfectiousObjects.contains(source)) {
            // 查询1度密接情况需要特殊处理
            if(degree == 1) {
                // 构建密接事件
                ContactEvent contactEvent = new ContactEvent(source.getId(), object.getId(), contactTime);
                List<ContactEvent> contactEventList = new ArrayList<>();
                // 密接事件添加到k度密接事件组内
                contactEventList.add(contactEvent);
                return new QueryResult(source.getId(), object.getId(), contactEventList, degree); // 返回结果
            }
            // 创建新的路径候选序列，如果密接事件传染源是初始传染源。
            List<Map<String, Integer>> newCandPath = new ArrayList<>();
            // 路径候选序列中第一个节点是初始传染源，密接时间默认为0。
            Map<String, Integer> newSourceNode = new HashMap<>();
            newSourceNode.put("id", source.getId());
            newSourceNode.put("time", 0);
            newCandPath.add(newSourceNode);
            // 路径候选序列中第二个节点为密接对象，密接时间为窗口的最后一个时间点。
            Map<String, Integer> newContactObjectNode = new HashMap<>();
            newContactObjectNode.put("id", object.getId());
            newContactObjectNode.put("time", contactTime);
            newCandPath.add(newContactObjectNode);
            // 将新的候选序列cand加入到动态候选序列中。
            dynamicCandidateSet.add(newCandPath);
            return null;
        }
        // 2.source不属于初始传染源，则需要进行candlist遍历，寻找path中含有source的cand。
        else {
//            Set<List<Map<String, Integer>>> newCandidateToAdd = new LinkedHashSet<>();
            for(List<Map<String, Integer>> cand : dynamicCandidateSet) {
                int candSize = cand.size(); // 当前cand中路径的长度（即传染源和密接对象的个数）
                // 1)先判断是否是最后一个节点为source,且当前路径长度为k个，加上当前新节点则满足为k度密接定义（即一个初始传染源，k个密接对象），则构建k度密接事件加入到结果集合中。
                if(cand.get(candSize - 1).get("id").equals(source.getId()) && candSize == degree) {
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
                            latterId = object.getId();
                            time = contactTime;
                        }
                        ContactEvent contactEvent = new ContactEvent(formerId, latterId, time);
//                        System.err.println("contactEvent = " + contactEvent);
                        k_degree_contactPath.add(contactEvent);
                    }
                    // 返回符合k度密接事件定义的结果。
                    return new QueryResult(cand.get(0).get("id"), object.getId(), k_degree_contactPath, degree);
                }
                // 2) source是最后一个节点，但是长度不足为k
                else if(cand.get(candSize - 1).get("id").equals(source.getId())) {
                    Map<String, Integer> newNode = new HashMap<>();
                    newNode.put("id", object.getId());
                    newNode.put("time", contactTime);
                    cand.add(newNode);
                }
                // 3)是中间节点，则必定不符合k度密接事件的定义，长度不为k。因为候选序列中路径长度都是小于k的。
                else {
                    for(int i = 1; i < candSize - 1; i++) {
                        Map<String, Integer> tempNode = cand.get(i);
                        // 存在这样的路径path含有sourceId值的节点，则进行更新
                        if(tempNode.get("id").equals(source.getId())) {
                            List<Map<String, Integer>> newCandPath = new ArrayList<>();
                            // 将查找到的cand中的前i个节点添加到newCand中。
                            for(int j = 0; j < i + 1; j++) {
                                newCandPath.add(cand.get(j));
                            }
                            // 将新的contactNode添加到newCand中。
                            Map<String, Integer> newContactNode = new HashMap<>();
                            newContactNode.put("id", object.getId());
                            newContactNode.put("time", contactTime);
                            newCandPath.add(newContactNode);
                            dynamicCandidateSet.add(newCandPath);
                            return null;
                        }
                    }
                }
            }
//            dynamicCandidateSet.addAll(newCandidateToAdd);
        }
        return null;
    }



    /**
     * 查询算法，每个待分析对象先与初始传染源进行密接判断
     *
     * @return {@link List}<{@link QueryResult}>
     */
    public List<QueryResult> startQuery_compareWithInitiallyInfectedObjectFirst() {
        List<QueryResult> result = new ArrayList<>();
        // 依次处理滑动窗口
        for(int windowStart = 0; windowStart < totalTimePoints - widthOfSlidingWindow; windowStart++) {
            // 不存在待分析对象，则直接返回结果
            if(objectsToBeAnalyzed.isEmpty()) {
                break;
            }
            Set<MovingObject> contactedMovingObjectsInCurrentWindow = new LinkedHashSet<>();
            // 每个窗口内查询全部的待分析移动对象
            for(MovingObject analyzedObject : objectsToBeAnalyzed) {
                // 判断其不为空
                if(analyzedObject == null) {
                    continue;
                }
                // 1.先与初始传染源进行判断
                boolean ifContact = commonQueryProcessing(initialInfectiousObjects, contactedMovingObjectsInCurrentWindow, analyzedObject, windowStart, result);
                // 发生密接事件，则不需要与确定的密接对象进行判断，直接分析下一个待分析对象。
                if(ifContact) {
                    continue;
                }
                // 2.再与已经确定的密接对象进行判断
                commonQueryProcessing(identifiedContactObjects, contactedMovingObjectsInCurrentWindow, analyzedObject, windowStart, result);
            }
            // 将每个窗口内的密接对象一次性添加到已确定的密接对象集合中。
            identifiedContactObjects.addAll(contactedMovingObjectsInCurrentWindow);
        }
        return result;
    }

}
