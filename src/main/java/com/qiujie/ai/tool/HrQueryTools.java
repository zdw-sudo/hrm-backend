package com.qiujie.ai.tool;

import cn.hutool.core.date.DateUtil;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.service.StaffLeaveService;
import com.qiujie.service.StaffService;
import com.qiujie.vo.StaffDeptVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "hrm.ai", name = "enabled", havingValue = "true")
public class HrQueryTools {

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffLeaveService staffLeaveService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Tool(description = """
            根据员工姓名查询员工基本信息。
            适用场景：查某人在哪个部门、工号、电话、在职状态等。
            当用户提到具体员工姓名且需要人员档案信息时，优先调用此工具。
            若返回多条同名员工，应告知用户并列出差异（如部门）供确认。
            """)
    public String getEmployeeByName(
            @ToolParam(description = "员工姓名，必须是中文全名，例如：张三") String staffName
    ) {
        if (staffName == null || staffName.isBlank()) {
            return "【员工查询结果】员工姓名不能为空。";
        }
        List<StaffDeptVO> staffList = findStaffByName(staffName.trim());
        if (staffList.isEmpty()) {
            return "【员工查询结果】未找到姓名为「" + staffName.trim() + "」的员工，请确认姓名是否正确。";
        }
        if (staffList.size() == 1) {
            return formatSingleEmployee(staffList.get(0));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【员工查询结果】找到 ").append(staffList.size()).append(" 条同名记录，请用户确认：\n");
        for (int i = 0; i < staffList.size(); i++) {
            StaffDeptVO staff = staffList.get(i);
            sb.append(i + 1).append(". ")
                    .append(nullToEmpty(staff.getName()))
                    .append(" | ").append(nullToEmpty(staff.getDeptName()))
                    .append(" | 工号 ").append(nullToEmpty(staff.getCode()))
                    .append(" | 员工ID ").append(staff.getId())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    @Tool(description = """
            查询指定员工在某一自然月的考勤汇总。
            返回迟到、早退、旷工、调休、休假等次数统计。
            适用场景：用户问某人某月考勤怎么样、迟到几次、有没有旷工。
            若用户说上个月、本月，需先换算为 yyyy-MM 格式再传入 month 参数。
            """)
    public String getMonthlyAttendance(
            @ToolParam(description = "员工姓名，例如：张三") String staffName,
            @ToolParam(description = "查询月份，格式 yyyy-MM，例如：2025-05") String month
    ) {
        if (staffName == null || staffName.isBlank()) {
            return "【考勤统计】员工姓名不能为空。";
        }
        if (month == null || month.isBlank()) {
            return "【考勤统计】月份不能为空，请使用 yyyy-MM 格式。";
        }
        String monthYyyyMm = normalizeMonth(month.trim());
        if (monthYyyyMm == null) {
            return "【考勤统计】月份格式无效，请使用 yyyy-MM 或 yyyyMM 格式。";
        }

        List<StaffDeptVO> staffList = findStaffByName(staffName.trim());
        if (staffList.isEmpty()) {
            return "【考勤统计】未找到员工「" + staffName.trim() + "」，无法查询考勤。";
        }
        if (staffList.size() > 1) {
            return "【考勤统计】存在多名同名员工「" + staffName.trim() + "」，请先确认具体是哪一位（可结合部门信息）。";
        }

        StaffDeptVO staff = staffList.get(0);
        Integer staffId = staff.getId();
        int late = countSafe(staffId, AttendanceStatusEnum.LATE.getCode(), monthYyyyMm);
        int leaveEarly = countSafe(staffId, AttendanceStatusEnum.LEAVE_EARLY.getCode(), monthYyyyMm);
        int absenteeism = countSafe(staffId, AttendanceStatusEnum.ABSENTEEISM.getCode(), monthYyyyMm);
        int timeOff = countSafe(staffId, AttendanceStatusEnum.TIME_OFF.getCode(), monthYyyyMm);
        int leaveDays = countLeaveDays(staffId, monthYyyyMm);

        return """
                【考勤统计】
                员工：%s（%s）
                月份：%s
                - 迟到：%d 次
                - 早退：%d 次
                - 旷工：%d 次
                - 调休：%d 次
                - 休假：%d 天（工作日）
                - 说明：数据来源于系统考勤记录
                """.formatted(
                nullToEmpty(staff.getName()),
                nullToEmpty(staff.getDeptName()),
                formatMonthDisplay(monthYyyyMm),
                late, leaveEarly, absenteeism, timeOff, leaveDays
        ).trim();
    }

    @Tool(description = """
            查询指定员工的历史请假申请记录。
            返回请假类型、天数、起始日期、审批状态（待审核/通过/驳回/撤销/审核中）等。
            适用场景：用户问某人请过什么假、请假审批到哪了、最近有没有请假。
            仅查询，不能用于提交或撤销请假。
            """)
    public String getLeaveRecordsByStaffName(
            @ToolParam(description = "员工姓名，例如：李四") String staffName,
            @ToolParam(description = "最多返回几条记录，默认 5，最大 20") Integer limit
    ) {
        if (staffName == null || staffName.isBlank()) {
            return "【请假记录】员工姓名不能为空。";
        }
        int recordLimit = limit == null || limit <= 0 ? 5 : Math.min(limit, 20);

        List<StaffDeptVO> staffList = findStaffByName(staffName.trim());
        if (staffList.isEmpty()) {
            return "【请假记录】未找到员工「" + staffName.trim() + "」。";
        }
        if (staffList.size() > 1) {
            return "【请假记录】存在多名同名员工「" + staffName.trim() + "」，请先确认具体是哪一位。";
        }

        StaffDeptVO staff = staffList.get(0);
        ResponseDTO result = staffLeaveService.queryByStaffId(1, recordLimit, staff.getId());
        if (result.getCode() != BusinessStatusEnum.SUCCESS.getCode() || result.getData() == null) {
            return "【请假记录】查询失败，请稍后重试。";
        }

        List<StaffLeave> leaves = extractLeaveList(result.getData());
        if (leaves.isEmpty()) {
            return "【请假记录】员工 " + staff.getName() + " 暂无请假申请记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【请假记录】\n");
        sb.append("员工：").append(staff.getName());
        if (staff.getDeptName() != null) {
            sb.append("（").append(staff.getDeptName()).append("）");
        }
        sb.append(" | 共 ").append(leaves.size()).append(" 条记录\n");

        for (int i = 0; i < leaves.size(); i++) {
            StaffLeave leave = leaves.get(i);
            sb.append(i + 1).append(". ");
            if (leave.getTypeNum() != null) {
                sb.append(leave.getTypeNum().getMessage());
            } else {
                sb.append("未知类型");
            }
            sb.append(" | ").append(leave.getDays() != null ? leave.getDays() : 0).append("天");
            sb.append(" | 起始：").append(leave.getStartDate() != null ? leave.getStartDate() : "未知");
            sb.append(" | 状态：");
            if (leave.getStatus() != null) {
                sb.append(leave.getStatus().getMessage());
            } else {
                sb.append("未知");
            }
            if (leave.getRemark() != null && !leave.getRemark().isBlank()) {
                sb.append(" | 备注：").append(leave.getRemark());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private List<StaffDeptVO> findStaffByName(String staffName) {
        ResponseDTO result = staffService.list(1, 20, staffName, null, null, null);
        if (result.getCode() != BusinessStatusEnum.SUCCESS.getCode() || result.getData() == null) {
            return List.of();
        }
        if (!(result.getData() instanceof Map<?, ?> data)) {
            return List.of();
        }
        Object listObj = data.get("list");
        if (!(listObj instanceof List<?> rawList)) {
            return List.of();
        }
        List<StaffDeptVO> staffList = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof StaffDeptVO staffDeptVO) {
                staffList.add(staffDeptVO);
            }
        }
        return staffList;
    }

    @SuppressWarnings("unchecked")
    private List<StaffLeave> extractLeaveList(Object data) {
        if (!(data instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object listObj = map.get("list");
        if (!(listObj instanceof List<?> rawList)) {
            return List.of();
        }
        List<StaffLeave> leaves = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> itemMap) {
                Object staffLeaveObj = itemMap.get("staffLeave");
                if (staffLeaveObj instanceof StaffLeave staffLeave) {
                    leaves.add(staffLeave);
                }
            }
        }
        return leaves;
    }

    private String formatSingleEmployee(StaffDeptVO staff) {
        String gender = staff.getGender() != null ? staff.getGender().getMessage() : "未知";
        String status = Objects.equals(staff.getStatus(), 1) ? "正常在职" : "状态异常";
        return """
                【员工查询结果】
                共找到 1 条记录：
                - 姓名：%s
                - 工号：%s
                - 部门：%s
                - 性别：%s
                - 电话：%s
                - 状态：%s
                - 员工ID：%s
                """.formatted(
                nullToEmpty(staff.getName()),
                nullToEmpty(staff.getCode()),
                nullToEmpty(staff.getDeptName()),
                gender,
                nullToEmpty(staff.getPhone()),
                status,
                staff.getId()
        ).trim();
    }

    private int countSafe(Integer staffId, Integer status, String monthYyyyMm) {
        Integer count = attendanceMapper.countTimes(staffId, status, monthYyyyMm);
        return count != null ? count : 0;
    }

    private int countLeaveDays(Integer staffId, String monthYyyyMm) {
        List<Date> leaveDateList = attendanceMapper.queryLeaveDate(
                staffId, AttendanceStatusEnum.LEAVE.getCode(), monthYyyyMm);
        if (leaveDateList == null || leaveDateList.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Date date : leaveDateList) {
            if (date != null && !DateUtil.isWeekend(date)) {
                count++;
            }
        }
        return count;
    }

    private String normalizeMonth(String month) {
        String digits = month.replace("-", "").replace("/", "");
        if (digits.matches("\\d{6}")) {
            return digits;
        }
        return null;
    }

    private String formatMonthDisplay(String monthYyyyMm) {
        if (monthYyyyMm.length() == 6) {
            return monthYyyyMm.substring(0, 4) + "年" + monthYyyyMm.substring(4, 6) + "月";
        }
        return monthYyyyMm;
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "未知";
    }
}
