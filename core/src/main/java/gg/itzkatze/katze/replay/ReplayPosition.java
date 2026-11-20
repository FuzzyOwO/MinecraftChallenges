package gg.itzkatze.katze.replay;

import java.util.Objects;

record ReplayPosition(double x, double y, double z, float yaw, float pitch) {
    static ReplayPosition sample(ReplayFrame.Actor current, ReplayFrame.Actor next, double fraction) {
        if (next == null || !current.world().equals(next.world()) || !Objects.equals(current.vehicle(), next.vehicle())
                || Math.pow(next.x() - current.x(), 2) + Math.pow(next.y() - current.y(), 2) + Math.pow(next.z() - current.z(), 2) > 64) {
            return new ReplayPosition(current.x(), current.y(), current.z(), current.yaw(), current.pitch());
        }
        double alpha = Math.clamp(fraction, 0, 1);
        return new ReplayPosition(current.x() + (next.x() - current.x()) * alpha, current.y() + (next.y() - current.y()) * alpha,
                current.z() + (next.z() - current.z()) * alpha, (float) (current.yaw() + Math.IEEEremainder(next.yaw() - current.yaw(), 360) * alpha),
                (float) (current.pitch() + (next.pitch() - current.pitch()) * alpha));
    }
}
