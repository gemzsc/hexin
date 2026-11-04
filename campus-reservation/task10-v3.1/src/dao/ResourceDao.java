package dao;
import entity.CampusResource;
import java.util.List;
import java.io.IOException;
public interface ResourceDao { List<CampusResource> findAll(); void saveAll(List<CampusResource> values) throws IOException; }
