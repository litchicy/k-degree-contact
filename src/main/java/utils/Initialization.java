package utils;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import entity.MovingObject;
import entity.PositionPoint;
import entity.Trajectory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 初始化传染源和待分析对象
 *
 * @author cy
 * @date 2023/11/04
 */
public class Initialization {

    /**
     * 初始化传染源集合
     *
     * @param idOfSourceList 传染源源列表Id
     * @param datasetName 数据集名称
     * @return {@link LinkedHashSet}<{@link MovingObject}>
     */
    public static LinkedHashSet<MovingObject> InitializeInfectiousObjects(Collection<Integer> idOfSourceList, String datasetName) {
        // 初始化的传染源集合
        LinkedHashSet<MovingObject> infectiousObjects = new LinkedHashSet<>();
        // 将用户设定的传染源依次加入
        for(Integer integer : idOfSourceList) {
            // 传染源
            MovingObject movingObject = new MovingObject();
            movingObject.setId(integer); //传染源编号
            // 轨迹
            Trajectory trajectory = new Trajectory();
            List<PositionPoint> tra = new ArrayList<>(); // 传染源全部轨迹点组成的轨迹数据
            // 文件路径
            String filePath = "D:\\dataset\\contact\\" + datasetName + "\\" + integer + ".txt";
            try(BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    double lat = Double.parseDouble(parts[1]); // 纬度
                    double lon = Double.parseDouble(parts[2]); // 经度
                    int time = Integer.parseInt(parts[3]); // 轨迹点编号，即时间
                    PositionPoint point = new PositionPoint(time, lat, lon); // 单个轨迹点
                    tra.add(point);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            trajectory.setTra(tra);
            movingObject.setTrajectory(trajectory);
            infectiousObjects.add(movingObject);
        }
        return infectiousObjects;
    }

    /**
     * 初始化待分析的移动对象
     *
     * @param idOfSourceList 传染源列表Id
     * @param datasetName 数据集名称
     * @param numberOfTracks 轨迹数量
     * @return {@link CopyOnWriteArrayList}<{@link MovingObject}>
     */
    public static CopyOnWriteArrayList<MovingObject> InitializeObjectsToBeAnalyzed(Collection<Integer> idOfSourceList, String datasetName, int numberOfTracks) {
        // 初始化的传染源集合
        CopyOnWriteArrayList<MovingObject> objectsToBeAnalyzed = new CopyOnWriteArrayList<>();
        // 将用户设定的传染源依次加入
        for(int i = 1; i < numberOfTracks + 1; i++) {
            if(!idOfSourceList.contains(i)) {
                // 传染源
                MovingObject movingObject = new MovingObject();
                movingObject.setId(i); //传染源编号
                // 轨迹
                Trajectory trajectory = new Trajectory();
                List<PositionPoint> tra = new ArrayList<>(); // 传染源全部轨迹点组成的轨迹数据
                // 文件路径
                String filePath = "D:\\dataset\\contact\\" + datasetName + "\\" + i + ".txt";
                try(BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        double lat = Double.parseDouble(parts[1]); // 纬度
                        double lon = Double.parseDouble(parts[2]); // 经度
                        int time = Integer.parseInt(parts[3]); // 轨迹点编号，即时间
                        PositionPoint point = new PositionPoint(time, lat, lon); // 单个轨迹点
                        tra.add(point);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                trajectory.setTra(tra);
                movingObject.setTrajectory(trajectory);
                objectsToBeAnalyzed.add(movingObject);
            }

        }
        return objectsToBeAnalyzed;
    }

    public static List<MovingObject> InitializeObjectsToBeAnalyzedByIterator(Collection<Integer> idOfSourceList, String datasetName, int numberOfTracks) {
        // 初始化的传染源集合
        List<MovingObject> objectsToBeAnalyzed = new ArrayList<>();
        // 将用户设定的传染源依次加入
        for(int i = 1; i < numberOfTracks + 1; i++) {
            if(!idOfSourceList.contains(i)) {
                // 传染源
                MovingObject movingObject = new MovingObject();
                movingObject.setId(i); //传染源编号
                // 轨迹
                Trajectory trajectory = new Trajectory();
                List<PositionPoint> tra = new ArrayList<>(); // 传染源全部轨迹点组成的轨迹数据
                // 文件路径
                String filePath = "D:\\dataset\\contact\\" + datasetName + "\\" + i + ".txt";
                try(BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        double lat = Double.parseDouble(parts[1]); // 纬度
                        double lon = Double.parseDouble(parts[2]); // 经度
                        int time = Integer.parseInt(parts[3]); // 轨迹点编号，即时间
                        PositionPoint point = new PositionPoint(time, lat, lon); // 单个轨迹点
                        tra.add(point);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                trajectory.setTra(tra);
                movingObject.setTrajectory(trajectory);
                objectsToBeAnalyzed.add(movingObject);
            }

        }
        return objectsToBeAnalyzed;
    }

    public static Set<Integer> InitializeObjectsIDToBeAnalyzedByIterator(Collection<Integer> idOfSourceList, int numberOfTracks) {
        // 初始化的传染源集合
        Set<Integer> objectsToBeAnalyzed = new HashSet<>();
        // 将用户设定的传染源依次加入
        for(int i = 1; i < numberOfTracks + 1; i++) {
            objectsToBeAnalyzed.add(i);
        }
        objectsToBeAnalyzed.removeAll(idOfSourceList);
        return objectsToBeAnalyzed;
    }

    public static List<List<PositionPoint>> InitializeAllTras(String datasetName, int numberOfTracks) {
        List<List<PositionPoint>> result = new ArrayList<>();
        // 将用户设定的传染源依次加入
        for(int i = 1; i < numberOfTracks + 1; i++) {
            // 轨迹
            List<PositionPoint> tra = new ArrayList<>(); // 传染源全部轨迹点组成的轨迹数据
            // 文件路径
            String filePath = "D:\\dataset\\contact\\" + datasetName + "\\" + i + ".txt";
            try(BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    double lat = Double.parseDouble(parts[1]); // 纬度
                    double lon = Double.parseDouble(parts[2]); // 经度
                    int time = Integer.parseInt(parts[3]); // 轨迹点编号，即时间
                    PositionPoint point = new PositionPoint(time, lat, lon); // 单个轨迹点
                    tra.add(point);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            result.add(tra);

        }
        return result;
    }



    /**
     * 初始化rtree
     * 一个时间点一个rtree
     *
     * @param trasOfAllObjects 所有对象轨迹数据
     * @param totalTimePoints 时间点数量
     * @return {@link RTree}<{@link Integer}, {@link Point}>{@link []}
     */
    public static RTree<Integer, Point>[] InitializeRTree(List<List<PositionPoint>> trasOfAllObjects, int totalTimePoints) {
        RTree<Integer, Point>[] rTree = new RTree[totalTimePoints];
        // i代表当前时间点的rtree
        for(int i = 0; i < totalTimePoints; i++) {
            rTree[i] = RTree.maxChildren(4).create();
            // j+1代表移动对象标号
            for(int j = 0; j < trasOfAllObjects.size(); j++) {
                // 移动对象Id
                int objectId = j + 1;
                // 获取 第i条轨迹中i时间点的位置点
                double lon = trasOfAllObjects.get(j).get(i).getLongitude();
                double lat = trasOfAllObjects.get(j).get(i).getLatitude();
//                Rectangle rectangle = Geometries.rectangleGeographic(lon, lat, lon, lat);
                Point point = Geometries.point(lon, lat);
                rTree[i] = rTree[i].add(objectId, point);
            }
        }
        return rTree;
    }

}
