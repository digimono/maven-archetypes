package ${package}.dao.base;

import java.util.List;

/**
 * @author ${author}
 */
public interface BaseMapper<TEntity, ID> {

    int insertSelective(TEntity entity);

    int deleteById(ID id);

    int markAsDeletedById(ID id);

    int updateByIdSelective(TEntity entity);

    TEntity findById(ID id);

    List<TEntity> findAll();
}
