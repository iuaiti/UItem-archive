package com.uitem.poison;

/**
 * Trạng thái runtime của độc trên 1 player.
 *
 * <p>Package-private — chỉ {@link PoisonManager} được đọc/ghi trực tiếp.
 * Mọi access từ bên ngoài đi qua các phương thức public của PoisonManager.
 *
 * <p>Không thread-safe riêng lẻ — PoisonManager chịu trách nhiệm
 * đồng bộ qua ConcurrentHashMap (compute/merge) hoặc synchronized block.
 */
final class PoisonState {

    /** Số stack hiện tại đang tích lũy (1 → maxStacks). */
    int currentStacks;

    /**
     * Tick còn lại trước khi stack hiện tại hết hạn.
     * Mỗi lần applyPoison() refresh về durationTicks của effect mới nhất.
     */
    int remainingTicks;

    /**
     * Tick tích lũy kể từ lần damage gần nhất.
     * Reset về 0 khi tickInterval được thoả mãn.
     */
    int tickAccumulator;

    /** Cấu hình của lần áp gần nhất (dùng cho damage & interval). */
    PoisonEffect lastEffect;

    PoisonState(PoisonEffect effect) {
        this.currentStacks   = 1;
        this.remainingTicks  = effect.getDurationTicks();
        this.tickAccumulator = 0;
        this.lastEffect      = effect;
    }

    /** Có đang active không (còn tick, còn stack). */
    boolean isActive() {
        return currentStacks > 0 && remainingTicks > 0;
    }

    /**
     * Tính damage thực tế của lần tick hiện tại.
     * Công thức: damagePerTick × amplifier × currentStacks
     */
    double computeDamage() {
        return lastEffect.getDamagePerTick()
                * lastEffect.getAmplifier()
                * currentStacks;
    }
}
