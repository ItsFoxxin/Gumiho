package dev.gumiho.link;

import java.util.List;

/** UI hints only. The relay repeats every authorization check. */
public final class LinkPolicy {
    public static boolean canInvite(String actorRole) {
        return List.of("FOUNDER","COFOUNDER","OFFICER").contains(actorRole);
    }
    public static boolean canSetRole(String actorId,String actorRole,LinkData.Member target,
                                     String nextRole,boolean anotherCofounder) {
        if(actorId==null || target==null || actorId.equals(target.id()) || "FOUNDER".equals(target.role())
                || "FOUNDER".equals(nextRole) || !LinkData.ROLES.contains(nextRole)) return false;
        if("FOUNDER".equals(actorRole)) return !"COFOUNDER".equals(nextRole) || !anotherCofounder || "COFOUNDER".equals(target.role());
        return "COFOUNDER".equals(actorRole) && !"COFOUNDER".equals(target.role())
                && !"COFOUNDER".equals(nextRole);
    }
    public static boolean canRemove(String actorId,String actorRole,LinkData.Member target) {
        if(actorId==null || target==null || actorId.equals(target.id()) || "FOUNDER".equals(target.role())) return false;
        return "FOUNDER".equals(actorRole)
                || "COFOUNDER".equals(actorRole) && !"COFOUNDER".equals(target.role())
                || "OFFICER".equals(actorRole) && "MEMBER".equals(target.role());
    }
    public static boolean canTransferFounder(String actorId,String actorRole,LinkData.Member target,
                                              boolean teamHasCofounder) {
        return actorId!=null && "FOUNDER".equals(actorRole) && target!=null && !actorId.equals(target.id())
                && !"FOUNDER".equals(target.role())
                && (!teamHasCofounder || "COFOUNDER".equals(target.role()));
    }
    private LinkPolicy() { }
}
