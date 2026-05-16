package sa.edu.kau.fcit.cpit252.project;

public class FileResource {
    private final String name;
    private final String path; 
    private final FileType type;

    public FileResource(String name, String path, FileType type) {
        this.name = name;
        this.path = path;
        this.type = type;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public FileType getType() { return type; }
}