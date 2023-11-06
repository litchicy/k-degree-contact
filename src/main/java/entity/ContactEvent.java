package entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密接事件数据结构定义
 *
 * @author cy
 * @date 2023/11/04
 */
@Data
@NoArgsConstructor
public class ContactEvent {

    /**
     * 传染源id
     */
    private int sourceId;

    /**
     * 被传染对象id
     */
    private int objectId;

    /**
     * 密接事件发生的时间
     */
    private int contactTime;

    public ContactEvent(int sourceId, int objectId, int contactTime) {
        this.sourceId = sourceId;
        this.objectId = objectId;
        this.contactTime = contactTime;
    }
}
