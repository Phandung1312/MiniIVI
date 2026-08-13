package com.miniivi.bootanimation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class BootAnimationGeneratorTest {
    @ClassRule public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static Path generatedOutputDirectory;

    @BeforeClass
    public static void generateSharedArchives() throws Exception {
        generatedOutputDirectory = temporaryFolder.newFolder("generated").toPath();
        BootAnimationGenerator.generate(generatedOutputDirectory);
    }

    @Test
    public void generate_createsValidStandardAndDarkArchives() throws Exception {
        Path standardArchive = generatedOutputDirectory.resolve(BootAnimationGenerator.STANDARD_ARCHIVE);
        Path darkArchive = generatedOutputDirectory.resolve(BootAnimationGenerator.DARK_ARCHIVE);
        assertTrue(Files.isRegularFile(standardArchive));
        assertTrue(Files.isRegularFile(darkArchive));
        BootAnimationVerifier.verify(standardArchive);
        BootAnimationVerifier.verify(darkArchive);
        assertArrayEquals(Files.readAllBytes(standardArchive), Files.readAllBytes(darkArchive));
    }

    @Test
    public void generate_isDeterministicAcrossRuns() throws Exception {
        Path secondDirectory = temporaryFolder.newFolder("second").toPath();

        BootAnimationGenerator.generate(secondDirectory);

        assertArrayEquals(
                Files.readAllBytes(
                        generatedOutputDirectory.resolve(BootAnimationGenerator.STANDARD_ARCHIVE)),
                Files.readAllBytes(secondDirectory.resolve(BootAnimationGenerator.STANDARD_ARCHIVE)));
    }

    @Test
    public void description_usesExpectedCanvasTimingAndParts() {
        assertEquals(
                "1920 1080 30\n"
                        + "c 1 0 intro #15121C\n"
                        + "f 0 0 shimmer 12 #15121C\n",
                BootAnimationGenerator.DESCRIPTION);
        assertEquals(90, BootAnimationGenerator.INTRO_FRAMES);
        assertEquals(72, BootAnimationGenerator.SHIMMER_FRAMES);
    }

    @Test
    public void archive_containsSequentialStoredFramesAndMatchingTrimData() throws Exception {
        try (ZipFile zip = new ZipFile(
                generatedOutputDirectory.resolve(BootAnimationGenerator.STANDARD_ARCHIVE).toFile())) {
            for (int index = 0; index < BootAnimationGenerator.INTRO_FRAMES; index++) {
                ZipEntry frame = zip.getEntry(String.format("intro/frame%03d.png", index));
                assertTrue(frame != null);
                assertEquals(ZipEntry.STORED, frame.getMethod());
            }
            for (int index = 0; index < BootAnimationGenerator.SHIMMER_FRAMES; index++) {
                ZipEntry frame = zip.getEntry(String.format("shimmer/frame%03d.png", index));
                assertTrue(frame != null);
                assertEquals(ZipEntry.STORED, frame.getMethod());
            }

            String introTrim = new String(
                    zip.getInputStream(zip.getEntry("intro/trim.txt")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertEquals(BootAnimationGenerator.INTRO_FRAMES, introTrim.lines().count());
            assertTrue(introTrim.lines().allMatch("900x420+510+330"::equals));
        }
    }

    @Test
    public void shimmer_movesFromLeftToRightWithoutDrawingTheFormerRing() {
        BufferedImage baseline = BootAnimationGenerator.renderShimmerFrame(0);
        BufferedImage leftSweep = BootAnimationGenerator.renderShimmerFrame(20);
        BufferedImage rightSweep = BootAnimationGenerator.renderShimmerFrame(52);

        assertTrue(changedPixelCentroidX(baseline, leftSweep) < 450.0);
        assertTrue(changedPixelCentroidX(baseline, rightSweep) > 450.0);
        assertEquals(0, visiblePixelsBelow(leftSweep, 285));
        assertEquals(0, visiblePixelsBelow(rightSweep, 285));
    }

    private static double changedPixelCentroidX(BufferedImage baseline, BufferedImage frame) {
        long xTotal = 0;
        long changed = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if (baseline.getRGB(x, y) != frame.getRGB(x, y)) {
                    xTotal += x;
                    changed++;
                }
            }
        }
        assertTrue(changed > 0);
        return xTotal / (double) changed;
    }

    private static int visiblePixelsBelow(BufferedImage image, int firstY) {
        int visible = 0;
        for (int y = firstY; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) {
                    visible++;
                }
            }
        }
        return visible;
    }
}
