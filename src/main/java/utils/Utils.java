package utils;

import entity.MovingObject;
import entity.QueryResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 工具类
 *
 * @author cy
 * @date 2023/11/05
 */
public class Utils {

    /**
     * 输出初始传染源对象ID
     *
     * @param objectSet 对象集
     */
    public static void soutObjectsOfInitialInfected(Set<MovingObject> objectSet) {
        for(MovingObject movingObject : objectSet) {
            System.out.println("初始传染源ID = " + movingObject.getId());
        }
    }

    /**
     * 输出已确定的密接对象id
     *
     * @param objectSet 对象集
     */
    public static void soutObjectsOfContact(Set<MovingObject> objectSet) {
        for(MovingObject movingObject : objectSet) {
            System.out.println("密接对象传染源ID = " + movingObject.getId());
        }
    }

    /**
     * 将列表写入文件
     *
     * @param dataList 数据列表
     * @param filePath 文件路径
     */
    public static void writeListToFile (Collection<?> dataList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Object item : dataList) {
                writer.write(item.toString());
                writer.newLine();
            }
            System.out.println("写入成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将列表写入文件
     *
     * @param dataList 数据列表
     * @param filePath 文件路径
     */
    public static void writeListIDToFile (Collection<List<Integer>> dataList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (List<Integer> innerList : dataList) {
                for (Integer item : innerList) {
                    writer.write(item.toString() + " ");
                }
                writer.newLine();
            }
            System.out.println("写入成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将列表追加写入到文件
     *
     * @param list 列表
     * @param filePath 文件路径
     */
    public static void appendListToFile(Collection<?> list, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (Object item : list) {
                writer.write(item.toString());
                writer.newLine();
            }
            System.out.println("追加写入成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
