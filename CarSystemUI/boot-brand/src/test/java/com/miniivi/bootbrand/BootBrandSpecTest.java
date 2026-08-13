package com.miniivi.bootbrand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public final class BootBrandSpecTest {
    @Test
    public void strokeSegmentsDescribeOnlyThreeCenteredGlyphs() {
        float[] segments = BootBrandSpec.copyStrokeSegments();

        assertEquals(8 * 4, segments.length);
        assertEquals(230.0f, BootBrandSpec.GLYPH_LEFT, 0.0f);
        assertEquals(670.0f, BootBrandSpec.GLYPH_RIGHT, 0.0f);
        assertEquals(
                BootBrandSpec.DESIGN_WIDTH / 2.0f,
                (BootBrandSpec.GLYPH_LEFT + BootBrandSpec.GLYPH_RIGHT) / 2.0f,
                0.0f);
    }

    @Test
    public void segmentCopiesCannotMutateSharedGeometry() {
        float[] first = BootBrandSpec.copyStrokeSegments();
        float[] second = BootBrandSpec.copyStrokeSegments();

        assertNotSame(first, second);
        first[0] = -1.0f;
        assertEquals(230.0f, second[0], 0.0f);
    }

    @Test
    public void shimmerMovesAcrossAndBeyondGlyphBounds() {
        assertEquals(1920, BootBrandSpec.CANVAS_WIDTH);
        assertEquals(1080, BootBrandSpec.CANVAS_HEIGHT);
        assertEquals(60.0f, BootBrandSpec.shimmerCenter(0.0f), 0.0f);
        assertEquals(840.0f, BootBrandSpec.shimmerCenter(1.0f), 0.0f);
        assertEquals(450.0f, BootBrandSpec.shimmerCenter(0.5f), 0.0f);
    }
}
