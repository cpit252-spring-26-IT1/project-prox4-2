package sa.edu.kau.fcit.cpit252.project;

public class FileResource {
    private String name;
    private FileType type; // Using Enum instead of String

    public FileResource(String name, FileType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public FileType getType() {
        return type;
    }
}

