package dev.gumiho.client.module.combat;

import dev.gumiho.client.module.utility.LaunchMath;
import dev.gumiho.client.config.GumihoConfig;
import dev.gumiho.client.config.CombatConfigMigration;

public final class ResetPreviewRegressionTest {
    private static int assertions;
    public static void run() {
        assertions = 0;
        MaceFlight cycle = new MaceFlight();
        check(cycle.newCycle(true, 0), "initial cycle");
        check(!cycle.newCycle(true, 0), "stationary ground does not churn target");
        check(cycle.newCycle(false, 0.8), "launch reacquires reachable target");
        check(!cycle.newCycle(false, 0.4), "ascent stable");
        check(!cycle.newCycle(false, -0.3), "descent retains valid landing target");
        check(cycle.newCycle(false, 0.9), "observed bounce reacquires instead of permanent lock");
        check(cycle.newCycle(true, 0), "landing drops airborne lock");
        cycle.reset(); check(cycle.newCycle(false, -0.3), "world/weapon reset");
        check(Double.isInfinite(MaceFlight.score(0, 0, 4, true, 11, 3)), "distant grounded lock rejected");
        check(Double.isFinite(MaceFlight.score(0, 0, 4, true, 2, 3)), "near grounded target accepted");
        check(Double.isInfinite(MaceFlight.score(5, 10, 4, false, 11, 3)), "unreachable lateral reset rejected");
        check(MaceFlight.score(0.5, 10, 4, false, 10, 3) < MaceFlight.score(3, 10, 4, false, 5, 3), "landing alignment outranks old lock proximity");
        close(MaceFlight.correctionRadius(2, 4), 0.65);
        check(MaceFlight.correctionRadius(10, 4) < MaceFlight.correctionRadius(30, 4), "late fall has smaller correction envelope");
        close(MaceFlight.correctionRadius(100, 4), 4);
        check(MaceFlight.atHeight(0, 1, 0, 0, -1, 0, 2) == null, "floor above feet rejected");
        check(MaceFlight.atHeight(0, Double.NaN, 0, 0, -1, 0, 0) == null, "invalid landing rejected");
        for (double height : new double[] {2, 4, 12, 24}) for (double vy : new double[] {-1, 0, 0.8}) {
            var landing = MaceFlight.atHeight(5, height, 7, 0.2, vy, -0.1, 0);
            check(landing != null && landing.ticks() > 0 && landing.ticks() < 80, "landing found");
            double n = (int)landing.ticks(), f = landing.ticks()-n;
            double sum = (1-Math.pow(0.91,n))/0.09+f*Math.pow(0.91,n);
            close(landing.x(), 5+0.2*sum); close(landing.z(), 7-0.1*sum);
        }
        var forward = MaceFlight.steering(0, 3, 0);
        check(forward.forward() > 0 && Math.abs(forward.left()) < 0.001, "yaw zero forward correction");
        check(MaceFlight.steering(3, 0, 0).left() > 0, "yaw zero left correction");
        check(MaceFlight.steering(-3, 0, 90).forward() > 0, "world correction rotated into local controls");
        check(MaceFlight.steering(0.01, 0.01, 0).forward() == 0, "dead zone avoids correction jitter");
        for (int yaw=-180;yaw<=180;yaw+=15) for (double pitch : new double[]{-89,-45,0,45,89}) {
            for (double speed : new double[]{0.5,0.7,1.5,1.6,2.5,3,3.15}) {
                var v = LaunchMath.direction(pitch,yaw,-20,speed);
                close(Math.sqrt(v.x()*v.x()+v.y()*v.y()+v.z()*v.z()),speed);
            }
        }
        check(LaunchMath.direction(0,0,-20,0.5).y() > 0, "potion upward launch offset");
        close(LaunchMath.direction(0,0,0,1.5).z(),1.5);
        check(LaunchMath.direction(Double.NaN,0,0,1) == null, "invalid preview angle rejected");
        check(!ShieldThreatRules.projectile(true,1,false,false), "piercing bypass not falsely blockable");
        check(!ShieldThreatRules.projectile(true,0,true,false), "returning trident excluded");
        check(ShieldThreatRules.projectile(true,0,false,false), "normal arrows/tridents");
        check(ShieldThreatRules.projectile(false,0,false,true), "supported fireball threat");
        check(!ShieldThreatRules.projectile(false,0,false,false), "unknown threat not mislabeled");
        check(!ShieldThreatRules.frontal(-0.5) && ShieldThreatRules.frontal(0.5), "front arc only");
        check(ShieldThreatRules.closeThreat(2,3.5,true,true,1), "close melee requires no attack toggle");
        check(!ShieldThreatRules.closeThreat(5,6,true,true,1), "distant melee not idle shield hold");
        check(!ShieldThreatRules.closeThreat(2,3.5,true,false,1), "wall blocks close threat");
        check(!ShieldThreatRules.closeThreat(2,3.5,true,true,-1), "opponent facing away ignored");
        var guard = new ImpactGuard();
        for(int tick=0;tick<20;tick++) check(guard.update(tick,Double.POSITIVE_INFINITY,7,true), "ongoing melee guard spans native startup");
        check(!guard.update(25,Double.POSITIVE_INFINITY,7,false), "guard expires after threat leaves");
        var c = new GumihoConfig();
        check(c.projectileHeldPreview && c.maceLandingGuide && !c.maceAirControl, "visual defaults and opt-in movement");
        check(c.shieldCloseGuard && c.shieldExplosions, "expanded defense defaults");
        c.maceResetRadius = Double.NaN; CombatConfigMigration.normalize(c); close(c.maceResetRadius,4);
        System.out.println("PASS: "+assertions+" mace reset, launch-preview and shield-defense assertions.");
    }
    private static void close(double actual,double expected){check(Math.abs(actual-expected)<1e-5,"expected "+expected+", got "+actual);}
    private static void check(boolean pass,String label){assertions++;if(!pass)throw new AssertionError(label);}
}
