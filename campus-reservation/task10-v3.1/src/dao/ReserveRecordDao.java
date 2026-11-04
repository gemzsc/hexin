package dao;
import entity.ReserveRecord;
import java.util.List;
import java.io.IOException;
public interface ReserveRecordDao { List<ReserveRecord> findAll(); void saveAll(List<ReserveRecord> values) throws IOException; }
