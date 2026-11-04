package service;
import entity.*;
import exception.*;
import dao.*;
import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.*;
public class ResourceService {
private final ResourceDao resourceDao;
 private final ReserveRecordDao recordDao;
 private final List<CampusResource> resources;
 private final List<ReserveRecord> records;
public ResourceService(ResourceDao resourceDao,ReserveRecordDao recordDao) {
 this.resourceDao=resourceDao; this.recordDao=recordDao;
 this.resources=resourceDao.findAll(); this.records=recordDao.findAll();
 }
public Result<List<CampusResource>> queryAllResources() { return Result.success("查询成功",resources); }
private boolean isValidResourceStatus(String status){
        return "AVAILABLE".equals(status)
                || "OCCUPIED".equals(status)
                || "DISABLED".equals(status);
    }
public Result<List<CampusResource>> queryAvailableResources(
            ) {
        List<CampusResource> availableResources = resources.stream().filter(resource ->"AVAILABLE".
                equals(resource.getStatus())).collect(Collectors.toList());

        return Result.success("查询成功", availableResources);
    }
public Result<List<CampusResource>> searchResourceByName(
            String keyword){
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("搜索关键词不能为空");
        }

        String normalizeKeyword = keyword.trim().toLowerCase();

        List<CampusResource> matched = resources.stream()
                .filter(resource -> resource.getName() != null
                        && resource.getName().toLowerCase()
                        .contains(normalizeKeyword))
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());

        return Result.success("查询成功",matched);
    }
public Result<CampusResource> queryResourceById(
            String id) {
        Optional<CampusResource> found = findResourceOptional( id);

        if (found.isPresent()) {
            return Result.success("查询成功", found.get());
        }
        return Result.fail("未找到该 ID 对应的资源");
    }
private int findResourceIndexById(
            String id) {

        if (resources == null || id == null) {
            return -1;
        }

        return IntStream.range(0, resources.size())
                .filter(index -> resources.get(index) != null
                        && id.equals(resources.get(index).getId()))
                .findFirst()
                .orElse(-1);
    }
public Result<CampusResource> addResource(
            User currentUser,String name,String type,String location,String description) throws IOException{
                requireAdmin(currentUser);

                if(name.isEmpty()){
                    throw new BusinessException("资源名称不能为空");
                }
                if(name.contains("|")){
                    throw new BusinessException("资源名称不能包含分隔符 |");
                }

                if(type.isEmpty()){
                    throw new BusinessException("资源类型不能为空");
                }
                if(type.contains("|")){
                    throw new BusinessException("资源类型不能包含分隔符 |");
                }

                if(location.isEmpty()){
                    throw new BusinessException("资源位置不能为空");
                }
                if(location.contains("|")){
                    throw new BusinessException("资源位置不能包含分隔符 |");
                }

                if(description.contains("|")){
                    throw new BusinessException("资源描述不能包含分隔符 |");
                }

                String id = java.util.UUID.randomUUID().toString();
                String status = "AVAILABLE";

                CampusResource resource = new CampusResource(
                        id,name,type,status,location,description);

                List<CampusResource> updated = new ArrayList<>(resources);
                updated.add(resource);

                resourceDao.saveAll(updated);
                resources.clear();
                resources.addAll(updated);

                return Result.success("资源新增成功",resource);
    }
public Result<Void> updateResource(
            User currentUser,String id,String name,String type,String location,String status,String description) throws IOException{
        requireAdmin(currentUser);

        CampusResource oldResource = requireResourceById(id);
        String newName = name.isEmpty() ? oldResource.getName() : name;
        String newType = type.isEmpty() ? oldResource.getType() : type;
        String newLocation = location.isEmpty() ? oldResource.getLocation() : location;
        String newStatus = status.isEmpty() ? oldResource.getStatus() : status;
        String newDescription = description.isEmpty() ? oldResource.getDescription() : description;
        if(newName.trim().isEmpty()
                || newType.trim().isEmpty()
                || newLocation.trim().isEmpty()){
            throw new BusinessException("名称、类型和位置不能为空");
        }

        if(!isValidResourceStatus(newStatus)){
            throw new BusinessException(
                    "资源状态只能是 AVAILABLE、OCCUPIED 或 DISABLED");
        }

        boolean hasPassedReserve = records.stream()
                .filter(r -> r != null)
                .anyMatch(r -> oldResource.getId().equals(r.getResourceId())
                        && r.getStatus() == ReserveStatus.PASS);

        if (hasPassedReserve && !"OCCUPIED".equals(newStatus)) {
            throw new BusinessException("该资源存在已通过预约，状态不能改为可预约或停用");
        }

        String[] fields = {
                newName, newType, newStatus,
                newLocation, newDescription
        };

        for (String field : fields) {
            if (field.contains("|")) {
                throw new BusinessException("资源字段不能包含分隔符 |");
            }
        }

        CampusResource changed = new CampusResource(
                oldResource.getId(),newName,newType,newStatus,
                newLocation,newDescription
        );

        List<CampusResource> updated = new ArrayList<>(resources);
        int targetIndex = findResourceIndexById(id);

        if (targetIndex == -1) {
            return Result.fail("资源不存在");
        }

        updated.set(targetIndex, changed);

        resourceDao.saveAll(updated);
        resources.clear();
        resources.addAll(updated);
        return Result.success("资源修改成功");
    }
public Result<Void> deleteResource(
            User currentUser,String id) throws IOException{
        requireAdmin(currentUser);

        CampusResource target = requireResourceById( id);

        boolean hasPassedReserve = records.stream()
                .filter(r -> r != null)
                .anyMatch(r -> id.equals(r.getResourceId())
                        && r.getStatus() == ReserveStatus.PASS);

        if (hasPassedReserve) {
            throw new BusinessException("该资源存在已通过预约，不能删除");
        }

        int targetIndex = findResourceIndexById( target.getId());

        if (targetIndex == -1) {
            return Result.fail("资源不存在");
        }

        List<CampusResource> updated = new ArrayList<>(resources);
        updated.remove(targetIndex);

        resourceDao.saveAll(updated);

        resources.clear();
        resources.addAll(updated);
        return Result.success("资源删除成功");
    }
private void requireAdmin(User currentUser){
        if(currentUser == null
                ||!"admin".equals(currentUser.getRole())){
            throw new BusinessException("无权限执行资源管理操作");
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