import java.time.LocalDateTime;

public class ReserveRecord {
    private String recordId;
    private String resourceId;
    private String applicantUsername;
    private LocalDateTime applyTime;
    private LocalDateTime reserveStartTime;
    private LocalDateTime reserveEndTime;
    private ReserveStatus status;
    private String remark;

    public ReserveRecord(){}

    public ReserveRecord(String recordId,String resourceId,String applicantUsername
                            ,LocalDateTime applyTime,LocalDateTime reserveStartTime,LocalDateTime reserveEndTime,ReserveStatus status,String remark){
        this.recordId = recordId;
        this.resourceId = resourceId;
        this.applicantUsername = applicantUsername;
        this.applyTime = applyTime;
        this.reserveStartTime = reserveStartTime;
        this.reserveEndTime = reserveEndTime;
        this.status = status;
        this.remark = remark;
    }

    public String getRecordId(){
        return recordId;
    }

    public void setRecordId(String recordId){
        this.recordId = recordId;
    }

    public String getResourceId(){
        return resourceId;
    }

    public void setResourceId(String resourceId){
        this.resourceId = resourceId;
    }

    public String getApplicantUsername(){
        return applicantUsername;
    }

    public void setApplicantUsername(String applicantUsername){
        this.applicantUsername = applicantUsername;
    }

    public LocalDateTime getApplyTime(){
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime){
        this.applyTime = applyTime;
    }

    public LocalDateTime getReserveStartTime(){
        return reserveStartTime;
    }

    public void setReserveStartTime(LocalDateTime reserveStartTime){
        this.reserveStartTime = reserveStartTime;
    }

    public LocalDateTime getReserveEndTime(){
        return reserveEndTime;
    }

    public void setReserveEndTime(LocalDateTime reserveEndTime){
        this.reserveEndTime = reserveEndTime;
    }

    public ReserveStatus getStatus() {
        return status;
    }

    public void setStatus(ReserveStatus status){
        this.status = status;
    }

    public String getRemark(){
        return remark;
    }

    public void setRemark(String remark){
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ReserveRecord{" +
                "recordId='" + recordId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", applicantUsername='" + applicantUsername + '\'' +
                ", applyTime=" + applyTime +
                ", reserveStartTime=" + reserveStartTime +
                ", reserveEndTime=" + reserveEndTime +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                '}';
    }
}
