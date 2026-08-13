package com.miniivi.bootbrand;

/** Shared geometry, palette, and timing for the MiniIVI boot experience. */
public final class BootBrandSpec {
    public static final int CANVAS_WIDTH = 1920;
    public static final int CANVAS_HEIGHT = 1080;
    public static final int DESIGN_WIDTH = 900;
    public static final int DESIGN_HEIGHT = 420;
    public static final int FPS = 30;
    public static final int INTRO_FRAMES = 45;
    public static final int SHIMMER_FRAMES = 36;
    public static final int FADE_FRAMES = 12;
    public static final long SHIMMER_DURATION_MILLIS = 1_200L;

    public static final int BACKGROUND_COLOR = 0xFF15121C;
    public static final int TEXT_COLOR = 0xFFF8F4FC;
    public static final int PRIMARY_COLOR = 0xFFA98CF5;
    public static final int SECONDARY_COLOR = 0xFFF0A8D8;

    public static final float TEXT_STROKE_WIDTH = 18.0f;
    public static final float SHADOW_STROKE_WIDTH = 42.0f;
    public static final float GLYPH_LEFT = 230.0f;
    public static final float GLYPH_RIGHT = 670.0f;
    public static final float GLYPH_TOP = 112.0f;
    public static final float GLYPH_BOTTOM = 248.0f;
    public static final float SHIMMER_HALF_WIDTH = 112.0f;
    public static final float SHIMMER_TRAVEL_PADDING = 170.0f;

    private static final float[] STROKE_SEGMENTS = {
        230.0f, 112.0f, 330.0f, 112.0f,
        280.0f, 112.0f, 280.0f, 248.0f,
        230.0f, 248.0f, 330.0f, 248.0f,
        385.0f, 112.0f, 450.0f, 248.0f,
        450.0f, 248.0f, 515.0f, 112.0f,
        570.0f, 112.0f, 670.0f, 112.0f,
        620.0f, 112.0f, 620.0f, 248.0f,
        570.0f, 248.0f, 670.0f, 248.0f,
    };

    private BootBrandSpec() {}

    /** Returns defensive copy of line segments encoded as x1, y1, x2, y2 groups. */
    public static float[] copyStrokeSegments() {
        return STROKE_SEGMENTS.clone();
    }

    public static float shimmerCenter(float progress) {
        float bounded = Math.max(0.0f, Math.min(1.0f, progress));
        float start = GLYPH_LEFT - SHIMMER_TRAVEL_PADDING;
        float end = GLYPH_RIGHT + SHIMMER_TRAVEL_PADDING;
        return start + ((end - start) * bounded);
    }
}
