package com.qiujie.ai.scope;

import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.RoleMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.vo.StaffDeptVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "hrm.ai", name = "enabled", havingValue = "true")
public class AiDataScopeService {

    private static final Set<String> ALL_SCOPE_ROLES = Set.of("admin", "hr");
    private static final Set<String> DEPT_SCOPE_ROLES = Set.of(
            "manager", "ceo", "cto", "director_of_sales",
            "publicity_director", "director_of_planning", "finance_minister"
    );

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private RoleMapper roleMapper;

    public AiOperatorContext getCurrentOperator() {
        String staffCode = requireStaffCode();
        StaffDeptVO staff = staffMapper.queryByCode(staffCode);
        if (staff == null) {
            throw new ServiceException(BusinessStatusEnum.STAFF_NOT_EXIST);
        }
        AiOperatorContext context = new AiOperatorContext();
        context.setStaffId(staff.getId());
        context.setCode(staff.getCode());
        context.setName(staff.getName());
        context.setDeptId(staff.getDeptId());
        context.setDeptName(staff.getDeptName());
        context.setScope(resolveScope(staff.getId()));
        return context;
    }

    public boolean canAccess(StaffDeptVO target) {
        if (target == null) {
            return false;
        }
        return canAccess(getCurrentOperator(), target);
    }

    public boolean canAccess(AiOperatorContext operator, StaffDeptVO target) {
        if (operator == null || target == null) {
            return false;
        }
        return switch (operator.getScope()) {
            case ALL -> true;
            case DEPT -> Objects.equals(operator.getDeptId(), target.getDeptId());
            case SELF -> Objects.equals(operator.getStaffId(), target.getId())
                    || Objects.equals(operator.getCode(), target.getCode());
        };
    }

    public String buildAccessDeniedMessage(StaffDeptVO target) {
        AiOperatorContext operator = getCurrentOperator();
        return buildAccessDeniedMessage(operator, target);
    }

    public String buildAccessDeniedMessage(AiOperatorContext operator, StaffDeptVO target) {
        String targetDesc = nullToEmpty(target.getName());
        if (target.getDeptName() != null && !target.getDeptName().isBlank()) {
            targetDesc += "（" + target.getDeptName() + "）";
        }
        return switch (operator.getScope()) {
            case SELF -> """
                    【权限不足】您当前仅可查询本人信息（工号 %s）。
                    无法查看其他员工「%s」的数据。
                    """.formatted(nullToEmpty(operator.getCode()), targetDesc).trim();
            case DEPT -> """
                    【权限不足】您当前仅可查询本部门「%s」员工信息。
                    无法查看「%s」的数据。
                    """.formatted(nullToEmpty(operator.getDeptName()), targetDesc).trim();
            case ALL -> "";
        };
    }

    public String describeCurrentScope() {
        AiOperatorContext operator = getCurrentOperator();
        return switch (operator.getScope()) {
            case ALL -> "全公司数据";
            case DEPT -> "本部门（" + nullToEmpty(operator.getDeptName()) + "）";
            case SELF -> "本人（工号 " + nullToEmpty(operator.getCode()) + "）";
        };
    }

    private AiDataScope resolveScope(Integer staffId) {
        List<String> roleCodes = roleMapper.queryRoleCodesByStaffId(staffId);
        if (roleCodes.stream().anyMatch(ALL_SCOPE_ROLES::contains)) {
            return AiDataScope.ALL;
        }
        if (roleCodes.stream().anyMatch(DEPT_SCOPE_ROLES::contains)) {
            return AiDataScope.DEPT;
        }
        return AiDataScope.SELF;
    }

    private String requireStaffCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ServiceException(BusinessStatusEnum.UNAUTHORIZED);
        }
        String staffCode = authentication.getName();
        if (staffCode == null || staffCode.isBlank() || "anonymousUser".equals(staffCode)) {
            throw new ServiceException(BusinessStatusEnum.UNAUTHORIZED);
        }
        return staffCode;
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "未知";
    }
}
