package sa.edu.kau.fcit.cpit252.project.model;

import sa.edu.kau.fcit.cpit252.project.files.FileType;
import java.io.Serializable;
import java.time.LocalDateTime;

public class PromotionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String requesterUsername;
    private final Role currentRole;
    private final Role requestedRole;
    private final LocalDateTime requestTime;
    private boolean pending;

    public PromotionRequest(String requesterUsername, Role currentRole) {
        this.requesterUsername = requesterUsername;
        this.currentRole = currentRole;
        this.requestedRole = getNextRole(currentRole);
        this.requestTime = LocalDateTime.now();
        this.pending = true;
    }

    private Role getNextRole(Role role) {
        switch (role) {
            case GUEST:   return Role.USER;
            case USER:    return Role.MANAGER;
            default:      return role;
        }
    }

    public String getRequesterUsername() { return requesterUsername; }
    public Role getCurrentRole()         { return currentRole; }
    public Role getRequestedRole()       { return requestedRole; }
    public LocalDateTime getRequestTime(){ return requestTime; }
    public boolean isPending()           { return pending; }
    public void setPending(boolean p)    { this.pending = p; }
}