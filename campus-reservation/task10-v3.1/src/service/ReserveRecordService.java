package service;
import entity.*;
import exception.*;
import dao.*;
import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.*;
public class ReserveRecordService {
private final ResourceDao resourceDao;
 private final ReserveRecordDao recordDao;
 private final List<CampusResource> resources;
 private final List<ReserveRecord> records;
public ReserveRecordService(ResourceDao resourceDao,ReserveRecordDao recordDao) {
 this.resourceDao=resourceDao; this.recordDao=recordDao;
 this.resources=resourceDao.findAll(); this.records=recordDao.findAll();
 }
public Result<List<ReserveRecord>> queryMyReserve(User currentUser){
        requireStudent(currentUser);

        List<ReserveRecord> mine = records.stream()
                .filter(r -> currentUser.getUsername()
                        .equals(r.getApplicantUsername()))
                .collect(Collectors.toList());

        return Result.success("查询成功",mine);

    }
public Result<List<ReserveRecord>> queryAllReserves(
            User currentUser) {
        requireAdmin(currentUser);
        return Result.success("查询成功", records);
    }
public Result<List<ReserveRecord>> queryReservesByCondition(
            User currentUser,
            String resourceId,
            String applicantUsername,
            ReserveStatus statusFilter) {

        requireAdmin(currentUser);

        List<ReserveRecord> matched = records.stream()
                .filter(Objects::nonNull)
                .filter(r -> resourceId == null || resourceId.trim().isEmpty()
                        || resourceId.trim().equals(r.getResourceId()))
                .filter(r -> applicantUsername == null
                        || applicantUsername.trim().isEmpty()
                        || applicantUsername.trim()
                        .equals(r.getApplicantUsername()))
                .filter(r -> statusFilter == null
                        || statusFilter == r.getStatus())
                .collect(Collectors.toList());

        return Result.success("查询成功", matched);
    }
public Result<ReserveRecord> submitReserve(
            User currentUser,
            String resourceId,
            LocalDateTime start,
            LocalDateTime end,
            String remark) throws IOException {
        String safeRemark = remark == null ? "" : remark;

        if (safeRemark.contains("|")
                || safeRemark.contains("\n")
                || safeRemark.contains("\r")) {
            throw new BusinessException("预约备注不能包含 | 或换行符");
        }

        requireStudent(currentUser);

        if (records == null || resourceId == null
                || resourceId.trim().isEmpty()
                || start == null || end == null) {
            throw new EmptyParameterException("资源 ID 和预约时间不能为空");
        }

        if (!start.isBefore(end)) {
            throw new BusinessException("预约开始时间必须早于结束时间");
        }

        if (end.isBefore(LocalDateTime.now())) {
            throw new BusinessException("预约结束时间不能早于当前时间");
        }

        // 复用已有的资源查找方法；资源不存在时由它抛出业务异常
        CampusResource resource = requireResourceById( resourceId);

        if (!"AVAILABLE".equals(resource.getStatus())) {
            throw new BusinessException("该资源当前不可预约");
        }

        boolean conflict = records.stream()
                .filter(Objects::nonNull)
                .filter(record -> resourceId.equals(record.getResourceId()))
                .filter(record -> record.getStatus() == ReserveStatus.PASS)
                .anyMatch(record ->
                        start.isBefore(record.getReserveEndTime())
                                && end.isAfter(record.getReserveStartTime()));

        if (conflict) {
            throw new BusinessException("预约时间与已有预约冲突");
        }

        String recordId = UUID.randomUUID().toString();

        ReserveRecord newRecord = new ReserveRecord(
                recordId,
                resourceId,
                currentUser.getUsername(),
                LocalDateTime.now(),
                start,
                end,
                ReserveStatus.PENDING,
                remark == null ? "" : remark
        );

        // 先保存候选集合；保存成功后才更新内存集合
        List<ReserveRecord> updated = new ArrayList<>(records);
        updated.add(newRecord);
        recordDao.saveAll(updated);

        records.clear();
        records.addAll(updated);

        return Result.success("预约申请已提交，等待管理员审核", newRecord);
    }
public Result<Void> approveReserve(
            User currentUser,
            String recordId) throws IOException {

        requireAdmin(currentUser);

        ReserveRecord oldRecord = findReserveOptional( recordId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        if (oldRecord.getStatus() != ReserveStatus.PENDING) {
            throw new BusinessException("只有待审核预约才能通过");
        }

        CampusResource oldResource =
                requireResourceById( oldRecord.getResourceId());

        if (!"AVAILABLE".equals(oldResource.getStatus())) {
            throw new BusinessException("关联资源当前不可用");
        }

        // 复制集合后，用新对象替换预约记录，避免浅拷贝提前改动原记录
        List<ReserveRecord> updatedRecords = new ArrayList<>(records);
        int recordIndex = updatedRecords.indexOf(oldRecord);

        ReserveRecord passedRecord = new ReserveRecord(
                oldRecord.getRecordId(),
                oldRecord.getResourceId(),
                oldRecord.getApplicantUsername(),
                oldRecord.getApplyTime(),
                oldRecord.getReserveStartTime(),
                oldRecord.getReserveEndTime(),
                ReserveStatus.PASS,
                oldRecord.getRemark()
        );
        updatedRecords.set(recordIndex, passedRecord);

        List<CampusResource> updatedResources = new ArrayList<>(resources);
        int resourceIndex = updatedResources.indexOf(oldResource);

        CampusResource occupiedResource = new CampusResource(
                oldResource.getId(),
                oldResource.getName(),
                oldResource.getType(),
                "OCCUPIED",
                oldResource.getLocation(),
                oldResource.getDescription()
        );
        updatedResources.set(resourceIndex, occupiedResource);

        recordDao.saveAll(updatedRecords);
        resourceDao.saveAll(updatedResources);

        // 两次保存都成功后，才替换内存列表
        records.clear();
        records.addAll(updatedRecords);
        resources.clear();
        resources.addAll(updatedResources);

        return Result.success("预约审核通过，资源已设为占用");
    }
public Result<Void> rejectReserve(
            User currentUser,
            String recordId) throws IOException {

        requireAdmin(currentUser);

        ReserveRecord oldRecord = findReserveOptional( recordId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        if (oldRecord.getStatus() != ReserveStatus.PENDING) {
            throw new BusinessException("只有待审核预约才能驳回");
        }

        List<ReserveRecord> updatedRecords = new ArrayList<>(records);
        int index = updatedRecords.indexOf(oldRecord);

        ReserveRecord rejectedRecord = new ReserveRecord(
                oldRecord.getRecordId(),
                oldRecord.getResourceId(),
                oldRecord.getApplicantUsername(),
                oldRecord.getApplyTime(),
                oldRecord.getReserveStartTime(),
                oldRecord.getReserveEndTime(),
                ReserveStatus.REJECT,
                oldRecord.getRemark()
        );
        updatedRecords.set(index, rejectedRecord);

        recordDao.saveAll(updatedRecords);

        records.clear();
        records.addAll(updatedRecords);

        return Result.success("预约已驳回");
    }
public Result<Void> cancelReserve(
            User currentUser,
            String recordId) throws IOException {

        requireStudent(currentUser);

        ReserveRecord oldRecord = findReserveOptional( recordId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        if (!currentUser.getUsername().equals(oldRecord.getApplicantUsername())) {
            throw new BusinessException("只能取消自己的预约");
        }

        ReserveStatus oldStatus = oldRecord.getStatus();
        if (oldStatus != ReserveStatus.PENDING
                && oldStatus != ReserveStatus.PASS) {
            throw new BusinessException("当前状态不允许取消预约");
        }

        ReserveRecord cancelledRecord = new ReserveRecord(
                oldRecord.getRecordId(),
                oldRecord.getResourceId(),
                oldRecord.getApplicantUsername(),
                oldRecord.getApplyTime(),
                oldRecord.getReserveStartTime(),
                oldRecord.getReserveEndTime(),
                ReserveStatus.CANCEL,
                oldRecord.getRemark()
        );

        List<ReserveRecord> updatedRecords = new ArrayList<>(records);
        updatedRecords.set(updatedRecords.indexOf(oldRecord), cancelledRecord);

        // PENDING 取消只改预约记录；PASS 取消还要释放资源
        List<CampusResource> updatedResources = null;

        if (oldStatus == ReserveStatus.PASS) {
            CampusResource oldResource =
                    requireResourceById( oldRecord.getResourceId());

            CampusResource releasedResource = new CampusResource(
                    oldResource.getId(),
                    oldResource.getName(),
                    oldResource.getType(),
                    "AVAILABLE",
                    oldResource.getLocation(),
                    oldResource.getDescription()
            );

            updatedResources = new ArrayList<>(resources);
            updatedResources.set(updatedResources.indexOf(oldResource), releasedResource);
        }

        recordDao.saveAll(updatedRecords);
        if (oldStatus == ReserveStatus.PASS) {
            resourceDao.saveAll(updatedResources);
        }

        records.clear();
        records.addAll(updatedRecords);

        if (oldStatus == ReserveStatus.PASS) {
            resources.clear();
            resources.addAll(updatedResources);
        }

        return Result.success("预约已取消");
    }
private Optional<ReserveRecord> findReserveOptional(
            String id){
        if (id == null) {
            return Optional.empty();
        }

        return records.stream()
                .filter(r ->r!= null && id.equals(r.getRecordId()))
                .findFirst();
    }
private void requireAdmin(User currentUser){
        if(currentUser == null
                ||!"admin".equals(currentUser.getRole())){
            throw new BusinessException("无权限执行资源管理操作");
        }
    }
private void requireStudent(User currentUser){
        if(currentUser == null
                ||!"student".equals(currentUser.getRole())){
            throw new BusinessException("无权限执行学生预约操作");
        }
    }
private Optional<CampusResource> findResourceOptional(
            String id) {
        if (id == null) {
            return Optional.empty();
        }

        return resources.stream()
                .filter(resource -> resource != null
                        && id.equals(resource.getId()))
                .findFirst();
    }
private CampusResource requireResourceById(
            String id){
        return findResourceOptional(id)
                .orElseThrow(()-> new BusinessException("资源不存在"));
    }
}