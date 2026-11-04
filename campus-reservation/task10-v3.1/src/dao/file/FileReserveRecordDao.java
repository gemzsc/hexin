package dao.file;
import dao.ReserveRecordDao;
import entity.ReserveRecord;
import utils.FileUtil;
import java.util.List;
import java.io.IOException;
public class FileReserveRecordDao implements ReserveRecordDao {
 private final List<ReserveRecord> values;
 public FileReserveRecordDao() throws IOException { values=FileUtil.loadReserveRecords(); }
 public List<ReserveRecord> findAll() { return values; }
 public void saveAll(List<ReserveRecord> updated) throws IOException { FileUtil.saveReserveRecords(updated); }
}
