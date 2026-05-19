package sa.edu.kau.fcit.cpit252.project.observer;

public enum AccessEvent {
    // File access events
    ACCESS_GRANTED,
    ACCESS_DENIED,
    LIMIT_REACHED,

    // Authentication events
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    PASSWORD_CHANGED,

    // User management events
    USER_CREATED,
    USER_DELETED,
    USER_PROMOTED,
    USER_DEMOTED,

    // File management events
    FILE_REGISTERED,
    FILE_DELETED,
    FILE_LOCKED,
    FILE_UNLOCKED
}