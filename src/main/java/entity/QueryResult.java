package entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询结果
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
@NoArgsConstructor
public class QueryResult {

    /**
     * 初始传染源id
     */
    private int sourceId;

    /**
     * 第k个被传染对象的id
     */
    private int objectId;

    /**
     * k度密接传播路径
     */
    private List<ContactEvent> K_Degree_contactPath;

    /**
     * 度数
     */
    private int k;

    public QueryResult(int sourceId, int objectId, List<ContactEvent> k_Degree_contactPath, int k) {
        this.sourceId = sourceId;
        this.objectId = objectId;
        this.K_Degree_contactPath = k_Degree_contactPath;
        this.k = k;
    }
}
