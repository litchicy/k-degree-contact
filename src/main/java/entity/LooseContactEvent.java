package entity;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class LooseContactEvent {

    /**
     * 传染源i集合
     */
    private Set<Integer> sourceId;

    /**
     * 被传染对象id
     */
    private int objectId;

    /**
     * 密接事件发生的时间
     */
    private int contactTime;

    public LooseContactEvent(Set<Integer> sourceId, int objectId, int contactTime) {
        this.sourceId = sourceId;
        this.objectId = objectId;
        this.contactTime = contactTime;
    }
}
