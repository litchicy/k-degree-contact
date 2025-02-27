package entity;

import java.util.List;
import java.util.Set;

public class LooseQueryResult {

    /**
     * 传染源id集合
     */
    private Set<Integer> sourceId;

    /**
     * 第k个被传染对象的id
     */
    private int objectId;

    /**
     * k度密接传播路径
     */
    private List<LooseContactEvent> K_Degree_contactPath;

    /**
     * 度数
     */
    private int k;

    public LooseQueryResult(Set<Integer> sourceId, int objectId, List<LooseContactEvent> k_Degree_contactPath, int k) {
        this.sourceId = sourceId;
        this.objectId = objectId;
        this.K_Degree_contactPath = k_Degree_contactPath;
        this.k = k;
    }
}
