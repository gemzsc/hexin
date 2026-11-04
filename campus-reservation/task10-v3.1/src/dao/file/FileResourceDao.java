package dao.file;
import dao.ResourceDao;
import entity.CampusResource;
import utils.FileUtil;
import java.util.List;
import java.io.IOException;
public class FileResourceDao implements ResourceDao {
 private final List<CampusResource> values;
 public FileResourceDao() throws IOException { values=FileUtil.loadResources(); }
 public List<CampusResource> findAll() { return values; }
 public void saveAll(List<CampusResource> updated) throws IOException { FileUtil.saveResources(updated); }
}
