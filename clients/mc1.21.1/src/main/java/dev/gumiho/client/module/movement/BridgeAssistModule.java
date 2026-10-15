package dev.gumiho.client.module.movement;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;

public final class BridgeAssistModule extends GumihoModule {
    private boolean forcedSneak;
    private int placeCooldown;
    private Object trackedPlayer;
    private Object trackedLevel;
    private int lastTick = Integer.MIN_VALUE;
    private BridgeRules.Mode lastMode;
    public BridgeAssistModule() {
        super("bridge_assist", "Bridge Assist", "다리 놓기 보조",
                "Bridge, jump-to-tower, or hold Use for aimed walls; right-click for options",
                "다리, 점프 기둥, 사용 키로 조준 벽 설치. 우클릭으로 설정",
                ModuleCategory.MOVEMENT, false, true);
    }
    private BridgeRules.Mode mode() { return BridgeRules.mode(GumihoClient.CONFIG.config().bridgeMode); }
    private boolean active(Minecraft client) {
        return enabled() && runtimeReady(client) && client.player != null && client.level != null && client.gameMode != null
                && client.player.isAlive() && !client.player.isSpectator() && client.screen == null
                && client.isWindowActive() && !client.isPaused()
                && canRun(client, GumihoClient.SERVER_POLICY);
    }
    private boolean wallOwnsUse(Minecraft client) {
        return active(client) && mode() == BridgeRules.Mode.WALL && !client.player.isUsingItem()
                && client.player.getMainHandItem().getItem() instanceof BlockItem
                && MovementKeys.physical(client, client.options.keyUse)
                && !MovementKeys.physical(client, client.options.keyAttack);
    }
    /** One placement owner: prevent vanilla Use from also placing/swinging that tick. */
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isCanceled() && event.isUseItem() && wallOwnsUse(Minecraft.getInstance())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
    @Override public void tick(Minecraft client) {
        if (!active(client)) { suspend(client); return; }
        BridgeRules.Mode mode = mode();
        if (trackedPlayer != client.player || trackedLevel != client.level || lastMode != mode) {
            suspend(client);
            trackedPlayer = client.player;
            trackedLevel = client.level;
            lastMode = mode;
        }
        if (lastTick == client.player.tickCount) return;
        lastTick = client.player.tickCount;
        if (placeCooldown > 0) placeCooldown--;
        if (client.player.isUsingItem() || MovementKeys.physical(client, client.options.keyAttack)
                || !(client.player.getMainHandItem().getItem() instanceof BlockItem)) {
            releaseSneak(client); return;
        }
        if (mode == BridgeRules.Mode.WALL) {
            releaseSneak(client);
            if (!wallOwnsUse(client) || placeCooldown > 0) return;
            Vec3 eye = client.player.getEyePosition();
            BlockHitResult hit = client.level.clip(new ClipContext(eye,
                    eye.add(client.player.getLookAngle().scale(client.player.blockInteractionRange())),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
            if (hit.getType() == HitResult.Type.BLOCK) place(client, hit, null);
            return;
        }
        // Manual item use takes priority outside Wall mode.
        if (MovementKeys.physical(client, client.options.keyUse)) { releaseSneak(client); return; }
        Vec3 motion = client.player.getDeltaMovement();
        BlockPos target;
        if (mode == BridgeRules.Mode.TOWER) {
            releaseSneak(client);
            if (!BridgeRules.tower(MovementKeys.physical(client, client.options.keyJump),
                    client.player.onGround(), motion.y)) return;
            target = BlockPos.containing(client.player.getX(), client.player.getY() - 1, client.player.getZ());
        } else {
            target = BlockPos.containing(client.player.getX() + motion.x * 2,
                    client.player.getY() - 0.5, client.player.getZ() + motion.z * 2);
            if (!client.player.onGround() || !client.level.getBlockState(target).canBeReplaced()) {
                releaseSneak(client); return;
            }
            client.options.keyShift.setDown(true);
            forcedSneak = true;
        }
        if (placeCooldown > 0 || !client.level.getBlockState(target).canBeReplaced()) return;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;
            BlockPos neighbor = target.relative(direction);
            var state = client.level.getBlockState(neighbor);
            // Avoid opening containers or triggering common interactive blocks.
            if (!state.isCollisionShapeFullBlock(client.level, neighbor)
                    || state.hasBlockEntity() || state.getMenuProvider(client.level, neighbor) != null) continue;
            Direction face = direction.getOpposite();
            Vec3 location = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5,
                    face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (place(client, new BlockHitResult(location, face, neighbor, false), target)) return;
        }
    }
    /** Returns true on an attempted interaction, even if the server refuses it. */
    private boolean place(Minecraft client, BlockHitResult hit, BlockPos expected) {
        var context = new BlockPlaceContext(client.player, InteractionHand.MAIN_HAND,
                client.player.getMainHandItem(), hit);
        BlockPos target = context.getClickedPos();
        if ((expected != null && !expected.equals(target)) || !context.canPlace()
                || client.level.isOutsideBuildHeight(target)
                || !client.level.getWorldBorder().isWithinBounds(target)
                || client.player.getEyePosition().distanceTo(hit.getLocation()) > client.player.blockInteractionRange()) return false;
        if (!client.level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(target),
                entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).isEmpty()) return false;
        // Verify a visible support face instead of reaching through walls.
        Direction face = hit.getDirection();
        Vec3 inside = hit.getLocation().add(-face.getStepX() * 0.001,
                -face.getStepY() * 0.001, -face.getStepZ() * 0.001);
        BlockHitResult visible = client.level.clip(new ClipContext(client.player.getEyePosition(), inside,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        if (visible.getType() != HitResult.Type.BLOCK || !visible.getBlockPos().equals(hit.getBlockPos())
                || visible.getDirection() != face) return false;
        placeCooldown = BridgeRules.delay(GumihoClient.CONFIG.config().bridgePlaceDelay);
        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction() && result.shouldSwing()) client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }
    @Override protected void onDisable(Minecraft client) { suspend(client); }
    @Override public void suspend(Minecraft client) {
        releaseSneak(client);
        placeCooldown = 0;
        lastTick = Integer.MIN_VALUE;
        trackedPlayer = null;
        trackedLevel = null;
        lastMode = null;
    }
    private void releaseSneak(Minecraft client) {
        if (forcedSneak) {
            client.options.keyShift.setDown(MovementKeys.physical(client, client.options.keyShift));
            forcedSneak = false;
        }
    }
}
