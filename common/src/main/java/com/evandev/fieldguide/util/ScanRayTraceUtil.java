package com.evandev.fieldguide.util;

import com.evandev.fieldguide.config.ServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Predicate;

public final class ScanRayTraceUtil {

    private ScanRayTraceUtil() {
    }

    // mirrors ProjectileUtil.getEntityHitResult, but with configurable padding
    public static EntityHitResult getScanEntityHitResult(Entity source, Vec3 startVec, Vec3 endVec, AABB searchBox, Predicate<Entity> filter, double maxDistSq) {
        Level level = source.level();
        double minSize = ServerConfig.get().minScanHitboxSize;
        double closestDistSq = maxDistSq;
        Entity hitEntity = null;
        Vec3 hitVec = null;

        for (Entity candidate : level.getEntities(source, searchBox, filter)) {
            AABB box = candidate.getBoundingBox();
            double padX = Math.max(0.0, (minSize - box.getXsize()) / 2.0);
            double padY = Math.max(0.0, (minSize - box.getYsize()) / 2.0);
            double padZ = Math.max(0.0, (minSize - box.getZsize()) / 2.0);
            box = box.inflate(padX, padY, padZ).inflate(candidate.getPickRadius());

            Optional<Vec3> clip = box.clip(startVec, endVec);
            if (box.contains(startVec)) {
                if (closestDistSq >= 0.0) {
                    hitEntity = candidate;
                    hitVec = clip.orElse(startVec);
                    closestDistSq = 0.0;
                }
            } else if (clip.isPresent()) {
                Vec3 point = clip.get();
                double distSq = startVec.distanceToSqr(point);
                if (distSq < closestDistSq || closestDistSq == 0.0) {
                    if (candidate.getRootVehicle() == source.getRootVehicle()) {
                        if (closestDistSq == 0.0) {
                            hitEntity = candidate;
                            hitVec = point;
                        }
                    } else {
                        hitEntity = candidate;
                        hitVec = point;
                        closestDistSq = distSq;
                    }
                }
            }
        }

        return hitEntity == null ? null : new EntityHitResult(hitEntity, hitVec);
    }
}
