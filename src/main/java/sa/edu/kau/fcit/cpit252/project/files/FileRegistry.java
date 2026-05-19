package sa.edu.kau.fcit.cpit252.project.files;

import sa.edu.kau.fcit.cpit252.project.model.Role;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileRegistry {

    private static FileRegistry instance;
    private final Map<String, FileResource> registry;

    private FileRegistry() {
        registry = new LinkedHashMap<>();
    }

    public static synchronized FileRegistry getInstance() {
        if (instance == null) instance = new FileRegistry();
        return instance;
    }

    public void register(FileResource file) {
        registry.put(file.getName(), file);
        System.out.println(">> [REGISTRY] File registered: " + file.getName() + " [" + file.getType() + "]");
    }

    public FileResource getFile(String name) {
        return registry.get(name);
    }

    public void delete(String name) {
        registry.remove(name);
        System.out.println(">> [REGISTRY] File removed: " + name);
    }

    public Map<String, FileResource> getAllVisibleTo(Role role) {
        Map<String, FileResource> visible = new LinkedHashMap<>();
        for (FileResource file : registry.values()) {
            switch (file.getType()) {
                case SENSITIVE:
                    if (role == Role.OWNER || role == Role.MANAGER) visible.put(file.getName(), file);
                    break;
                case INTERNAL:
                    if (role != Role.GUEST) visible.put(file.getName(), file);
                    break;
                case NORMAL:
                    visible.put(file.getName(), file);
                    break;
            }
        }
        return visible;
    }

    public boolean isEmpty() {
        return registry.isEmpty();
    }

    public Map<String, FileResource> getAll() {
        return registry;
    }
}
