package ${package}.dao.base;

import java.util.List;

/**
 * @author yangyanju
 * @version 1.0
 */
public interface BaseMapper<TEntity, ID> {

    /**
     * 有选择性的新增对象.
     *
     * @param entity 对象实例.
     * @return 受影响行数.
     */
    int insert(TEntity entity);

    /**
     * 根据ID删除对象(物理删除).
     *
     * @param id 对象ID.
     * @return 受影响行数.
     */
    int deleteById(ID id);

    /**
     * 标记删除(逻辑删除).
     *
     * @param id 对象ID.
     * @return 受影响行数.
     */
    int markAsDeleted(ID id);

    /**
     * 有选择性的更新对象.
     *
     * @param entity 对象实例.
     * @return 受影响行数.
     */
    int updateById(TEntity entity);

    /**
     * 根据ID获取对象.
     *
     * @param id 对象ID.
     * @return 对象实例.
     */
    TEntity findById(ID id);

    /**
     * 获取所有对象.
     *
     * @return 对象集合.
     */
    List<TEntity> findAll();
}
