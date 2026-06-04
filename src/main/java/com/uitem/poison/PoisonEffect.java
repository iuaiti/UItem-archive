package com.uitem.poison;

/**
 * Cấu hình của một lần áp độc.
 *
 * <p>Object này là <b>immutable</b> — mọi giá trị được set tại thời điểm tạo
 * và không thay đổi trong suốt vòng đời. Sử dụng {@link Builder} để xây dựng.
 *
 * <p>Thiết kế: tách biệt hoàn toàn với state runtime (stack đang tích lũy,
 * thời gian còn lại…) — state đó được giữ trong {@link PoisonState} bên trong
 * {@link PoisonManager}.
 */
public final class PoisonEffect {

    // ── Defaults ──────────────────────────────────────────────────────────────
    public static final int  DEFAULT_DURATION_TICKS = 100;   // 5 giây
    public static final int  DEFAULT_AMPLIFIER       = 1;
    public static final int  DEFAULT_MAX_STACKS      = 5;
    public static final int  DEFAULT_TICK_INTERVAL   = 20;   // 1 giây / lần
    public static final double DEFAULT_DAMAGE_PER_TICK = 0.5; // ½ tim

    // ── Fields ────────────────────────────────────────────────────────────────
    private final int    durationTicks;
    private final int    amplifier;
    private final int    maxStacks;
    private final int    tickInterval;
    private final double damagePerTick;

    private PoisonEffect(Builder b) {
        this.durationTicks  = b.durationTicks;
        this.amplifier      = b.amplifier;
        this.maxStacks      = b.maxStacks;
        this.tickInterval   = b.tickInterval;
        this.damagePerTick  = b.damagePerTick;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Thời gian tồn tại của một lần áp (đơn vị tick, 20 tick = 1 giây). */
    public int getDurationTicks()  { return durationTicks; }

    /**
     * Hệ số khuếch đại — nhân thêm vào damage theo công thức:
     * {@code effectiveDamage = damagePerTick * amplifier * currentStacks}.
     */
    public int getAmplifier()      { return amplifier; }

    /** Số stack tối đa có thể cộng dồn trên cùng 1 player. */
    public int getMaxStacks()      { return maxStacks; }

    /** Cứ bao nhiêu tick thì tick damage 1 lần. */
    public int getTickInterval()   { return tickInterval; }

    /** Sát thương cơ bản mỗi lần tick (trước khi nhân amplifier và stacks). */
    public double getDamagePerTick() { return damagePerTick; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    /** Builder tiện lợi với giá trị mặc định an toàn. */
    public static final class Builder {

        private int    durationTicks = DEFAULT_DURATION_TICKS;
        private int    amplifier     = DEFAULT_AMPLIFIER;
        private int    maxStacks     = DEFAULT_MAX_STACKS;
        private int    tickInterval  = DEFAULT_TICK_INTERVAL;
        private double damagePerTick = DEFAULT_DAMAGE_PER_TICK;

        private Builder() {}

        public Builder durationTicks(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("durationTicks must be >= 1, got " + ticks);
            this.durationTicks = ticks; return this;
        }

        public Builder amplifier(int amplifier) {
            if (amplifier < 1) throw new IllegalArgumentException("amplifier must be >= 1, got " + amplifier);
            this.amplifier = amplifier; return this;
        }

        public Builder maxStacks(int max) {
            if (max < 1) throw new IllegalArgumentException("maxStacks must be >= 1, got " + max);
            this.maxStacks = max; return this;
        }

        public Builder tickInterval(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("tickInterval must be >= 1, got " + ticks);
            this.tickInterval = ticks; return this;
        }

        public Builder damagePerTick(double dmg) {
            if (dmg < 0) throw new IllegalArgumentException("damagePerTick must be >= 0, got " + dmg);
            this.damagePerTick = dmg; return this;
        }

        public PoisonEffect build() {
            return new PoisonEffect(this);
        }
    }

    @Override
    public String toString() {
        return "PoisonEffect{dur=" + durationTicks + "t, amp=" + amplifier
                + ", maxStacks=" + maxStacks + ", interval=" + tickInterval
                + "t, dmg=" + damagePerTick + "}";
    }
}
