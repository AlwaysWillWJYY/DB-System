package simpledb.storage.evict;

import simpledb.storage.PageId;

/**
 * @description:    丢弃策略的抽象接口
 */

public interface EvictStrategy {

    /**
     * 修改对应的数据结构以满足丢弃策略
     * @param pageId
     */
    void modifyData(PageId pageId);

    /**
     * 获取要丢弃的Page的PageId信息,用于丢弃
     * @return  PageId
     */
    PageId getEvictPageId();

}
