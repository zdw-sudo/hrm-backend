package com.qiujie.mapper;

import com.qiujie.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-01-27
 */
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
            select pr.code
            from per_role pr
            inner join per_staff_role psr on pr.id = psr.role_id
            where psr.staff_id = #{staffId}
              and pr.is_deleted = 0
            """)
    List<String> queryRoleCodesByStaffId(@Param("staffId") Integer staffId);
}
