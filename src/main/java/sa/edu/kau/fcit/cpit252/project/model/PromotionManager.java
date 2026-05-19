package sa.edu.kau.fcit.cpit252.project;

import java.util.ArrayList;
import java.util.List;

public class PromotionManager {

    private static PromotionManager instance;
    private final List<PromotionRequest> requests;

    private PromotionManager() {
        requests = new ArrayList<>();
    }

    public static synchronized PromotionManager getInstance() {
        if (instance == null) instance = new PromotionManager();
        return instance;
    }

    public void requestPromotion(UserAccount requester) {
        if (requester.getRole() == Role.MANAGER || requester.getRole() == Role.OWNER) {
            System.out.println(Colors.yellow(">> [INFO] Your role cannot be promoted further."));
            return;
        }

        for (PromotionRequest r : requests) {
            if (r.getRequesterUsername().equals(requester.getUsername()) && r.isPending()) {
                System.out.println(Colors.yellow(">> [INFO] You already have a pending promotion request."));
                return;
            }
        }

        PromotionRequest request = new PromotionRequest(requester.getUsername(), requester.getRole());
        requests.add(request);
        System.out.println(Colors.green(">> [SUCCESS] Promotion request submitted: "
            + requester.getRole() + " → " + request.getRequestedRole()));
    }

    public void showPendingRequests() {
        List<PromotionRequest> pending = getPendingRequests();
        if (pending.isEmpty()) {
            System.out.println(Colors.yellow(">> [INFO] No pending promotion requests."));
            return;
        }
        System.out.println(Colors.cyan("\n─── Pending Promotion Requests ───"));
        for (int i = 0; i < pending.size(); i++) {
            PromotionRequest r = pending.get(i);
            System.out.println("  [" + (i + 1) + "] " +
                Colors.white(r.getRequesterUsername()) +
                " — " + Colors.yellow(r.getCurrentRole().toString()) +
                " → " + Colors.green(r.getRequestedRole().toString()) +
                "  (" + r.getRequestTime().toLocalDate() + ")");
        }
        System.out.println();
    }

    public List<PromotionRequest> getPendingRequests() {
        List<PromotionRequest> pending = new ArrayList<>();
        for (PromotionRequest r : requests) {
            if (r.isPending()) pending.add(r);
        }
        return pending;
    }

    public void approveRequest(int index, UserAccount owner, AuthenticationManager auth) {
        List<PromotionRequest> pending = getPendingRequests();
        if (index < 1 || index > pending.size()) {
            System.out.println(Colors.red(">> [INVALID] Request number out of range."));
            return;
        }
        PromotionRequest request = pending.get(index - 1);
        auth.promoteUser(request.getRequesterUsername(), owner);
        request.setPending(false);
        System.out.println(Colors.green(">> [APPROVED] Promotion request approved."));
    }

    public void rejectRequest(int index) {
        List<PromotionRequest> pending = getPendingRequests();
        if (index < 1 || index > pending.size()) {
            System.out.println(Colors.red(">> [INVALID] Request number out of range."));
            return;
        }
        PromotionRequest request = pending.get(index - 1);
        request.setPending(false);
        System.out.println(Colors.yellow(">> [REJECTED] Promotion request rejected."));
    }
}