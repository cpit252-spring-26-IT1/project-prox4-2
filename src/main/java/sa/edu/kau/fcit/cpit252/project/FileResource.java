package sa.edu.kau.fcit.cpit252.project;

public class FileResource {
    private final String name;
    private final String path;
    private final FileType type;
    private boolean locked;

    public FileResource(String name, String path, FileType type) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.locked = false;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public FileType getType() { return type; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
}