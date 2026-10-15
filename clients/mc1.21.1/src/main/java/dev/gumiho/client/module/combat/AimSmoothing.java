package dev.gumiho.client.module.combat;

/** Exact integration of a speed-capped exponential approach, independent of render FPS. */
public final class AimSmoothing {
    private AimSmoothing() { }
    public static double step(double error, double seconds, double degreesPerSecond, double responseSeconds) {
        if (!Double.isFinite(error) || !Double.isFinite(seconds) || seconds <= 0
                || !Double.isFinite(degreesPerSecond) || degreesPerSecond <= 0
                || !Double.isFinite(responseSeconds) || responseSeconds <= 0) return 0;
        double dt = Math.min(seconds, 0.1);
        double distance = Math.abs(error);
        double boundary = degreesPerSecond * responseSeconds;
        double linearTime = Math.max(0, (distance - boundary) / degreesPerSecond);
        double linear = Math.min(dt, linearTime);
        double remaining = distance - degreesPerSecond * linear;
        remaining *= Math.exp(-(dt - linear) / responseSeconds);
        return Math.copySign(distance - remaining, error);
    }
}
