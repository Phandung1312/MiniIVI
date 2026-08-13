package com.miniivi.bootanimation;

import com.miniivi.bootbrand.BootBrandSpec;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/** Generates deterministic Android boot animation archives for MiniIVI. */
public final class BootAnimationGenerator {
    public static final int CANVAS_WIDTH = BootBrandSpec.CANVAS_WIDTH;
    public static final int CANVAS_HEIGHT = BootBrandSpec.CANVAS_HEIGHT;
    public static final int FPS = BootBrandSpec.FPS;
    public static final int INTRO_FRAMES = BootBrandSpec.INTRO_FRAMES;
    public static final int SHIMMER_FRAMES = BootBrandSpec.SHIMMER_FRAMES;
    public static final int FADE_FRAMES = BootBrandSpec.FADE_FRAMES;
    public static final int FRAME_WIDTH = BootBrandSpec.DESIGN_WIDTH;
    public static final int FRAME_HEIGHT = BootBrandSpec.DESIGN_HEIGHT;
    public static final int FRAME_X = (CANVAS_WIDTH - FRAME_WIDTH) / 2;
    public static final int FRAME_Y = (CANVAS_HEIGHT - FRAME_HEIGHT) / 2;
    public static final String BACKGROUND_HEX = "#15121C";
    public static final String STANDARD_ARCHIVE = "bootanimation.zip";
    public static final String DARK_ARCHIVE = "bootanimation-dark.zip";
    public static final String DESCRIPTION =
            CANVAS_WIDTH + " " + CANVAS_HEIGHT + " " + FPS + "\n"
                    + "c 1 0 intro " + BACKGROUND_HEX + "\n"
                    + "f 0 0 shimmer " + FADE_FRAMES + " " + BACKGROUND_HEX + "\n";

    private static final Color PRIMARY = new Color(BootBrandSpec.PRIMARY_COLOR, true);
    private static final Color SECONDARY = new Color(BootBrandSpec.SECONDARY_COLOR, true);
    private static final Color TEXT = new Color(BootBrandSpec.TEXT_COLOR, true);
    private static final Color TRANSPARENT_PRIMARY = new Color(
            PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 0);
    private static final Color TRANSPARENT_SECONDARY = new Color(
            SECONDARY.getRed(), SECONDARY.getGreen(), SECONDARY.getBlue(), 0);
    private static final long ZIP_TIMESTAMP_MILLIS = 0L;

    private BootAnimationGenerator() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory argument.");
        }
        Path outputDirectory = Path.of(arguments[0]);
        generate(outputDirectory);
        System.out.println("Generated MiniIVI boot animations in " + outputDirectory.toAbsolutePath());
    }

    public static void generate(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Map<String, byte[]> entries = createArchiveEntries();
        writeArchive(outputDirectory.resolve(STANDARD_ARCHIVE), entries);
        writeArchive(outputDirectory.resolve(DARK_ARCHIVE), entries);
    }

    private static Map<String, byte[]> createArchiveEntries() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("desc.txt", DESCRIPTION.getBytes(StandardCharsets.UTF_8));

        for (int index = 0; index < INTRO_FRAMES; index++) {
            entries.put(frameName("intro", index), encodePng(renderIntroFrame(index)));
        }
        entries.put("intro/trim.txt", createTrimFile(INTRO_FRAMES));

        for (int index = 0; index < SHIMMER_FRAMES; index++) {
            entries.put(frameName("shimmer", index), encodePng(renderShimmerFrame(index)));
        }
        entries.put("shimmer/trim.txt", createTrimFile(SHIMMER_FRAMES));
        return entries;
    }

    private static String frameName(String part, int index) {
        return String.format("%s/frame%03d.png", part, index);
    }

    private static byte[] createTrimFile(int frameCount) {
        String trim = FRAME_WIDTH + "x" + FRAME_HEIGHT + "+" + FRAME_X + "+" + FRAME_Y + "\n";
        return trim.repeat(frameCount).getBytes(StandardCharsets.UTF_8);
    }

    static BufferedImage renderIntroFrame(int index) {
        BufferedImage image = transparentFrame();
        Graphics2D graphics = createGraphics(image);
        double progress = index / (double) (INTRO_FRAMES - 1);
        double reveal = easeOutCubic(clamp((progress - 0.06) / 0.70));

        AffineTransform originalTransform = graphics.getTransform();
        double scale = 0.94 + (0.06 * reveal);
        graphics.translate(FRAME_WIDTH / 2.0, FRAME_HEIGHT / 2.0);
        graphics.scale(scale, scale);
        graphics.translate(-FRAME_WIDTH / 2.0, -FRAME_HEIGHT / 2.0);
        double sweep = clamp((progress - 0.18) / 0.68);
        drawIviMark(graphics, (float) reveal, (float) sweep);
        graphics.setTransform(originalTransform);
        graphics.dispose();
        return image;
    }

    static BufferedImage renderShimmerFrame(int index) {
        BufferedImage image = transparentFrame();
        Graphics2D graphics = createGraphics(image);
        float progress = index / (float) SHIMMER_FRAMES;
        drawIviMark(graphics, 1.0f, progress);
        graphics.dispose();
        return image;
    }

    private static BufferedImage transparentFrame() {
        return new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D createGraphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return graphics;
    }

    private static void drawIviMark(Graphics2D graphics, float alpha, float shimmerProgress) {
        if (alpha <= 0.0f) {
            return;
        }

        Graphics2D shadow = (Graphics2D) graphics.create();
        shadow.translate(10.0, 0.0);
        shadow.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.12f));
        drawSegments(shadow, PRIMARY, BootBrandSpec.SHADOW_STROKE_WIDTH);
        shadow.dispose();

        Graphics2D base = (Graphics2D) graphics.create();
        base.setComposite(AlphaComposite.SrcOver.derive(alpha));
        drawSegments(base, TEXT, BootBrandSpec.TEXT_STROKE_WIDTH);
        base.dispose();

        float center = BootBrandSpec.shimmerCenter(shimmerProgress);
        Paint glowPaint = shimmerPaint(center, true);
        Paint highlightPaint = shimmerPaint(center, false);

        Graphics2D glow = (Graphics2D) graphics.create();
        glow.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.58f));
        drawSegments(glow, glowPaint, BootBrandSpec.SHADOW_STROKE_WIDTH);
        glow.dispose();

        Graphics2D highlight = (Graphics2D) graphics.create();
        highlight.setComposite(AlphaComposite.SrcOver.derive(alpha));
        drawSegments(highlight, highlightPaint, BootBrandSpec.TEXT_STROKE_WIDTH + 2.0f);
        highlight.dispose();
    }

    private static Paint shimmerPaint(float center, boolean glow) {
        float halfWidth = BootBrandSpec.SHIMMER_HALF_WIDTH;
        Color peakPrimary = withAlpha(PRIMARY, glow ? 150 : 235);
        Color peakSecondary = withAlpha(SECONDARY, glow ? 135 : 245);
        return new LinearGradientPaint(
                center - halfWidth,
                0.0f,
                center + halfWidth,
                0.0f,
                new float[] {0.0f, 0.30f, 0.50f, 0.70f, 1.0f},
                new Color[] {
                    TRANSPARENT_PRIMARY,
                    peakPrimary,
                    peakSecondary,
                    peakPrimary,
                    TRANSPARENT_SECONDARY,
                });
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static void drawSegments(Graphics2D graphics, Paint paint, float strokeWidth) {
        graphics.setPaint(paint);
        graphics.setStroke(new BasicStroke(
                strokeWidth,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        float[] segments = BootBrandSpec.copyStrokeSegments();
        for (int index = 0; index < segments.length; index += 4) {
            graphics.draw(new Line2D.Float(
                    segments[index],
                    segments[index + 1],
                    segments[index + 2],
                    segments[index + 3]));
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double easeOutCubic(double value) {
        double inverse = 1.0 - value;
        return 1.0 - (inverse * inverse * inverse);
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) {
            throw new IOException("No PNG encoder is available in the current JDK.");
        }
        return output.toByteArray();
    }

    private static void writeArchive(Path archive, Map<String, byte[]> entries) throws IOException {
        Path temporaryArchive = Files.createTempFile(archive.getParent(), archive.getFileName().toString(), ".tmp");
        try {
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporaryArchive))) {
                for (Map.Entry<String, byte[]> item : entries.entrySet()) {
                    byte[] data = item.getValue();
                    CRC32 crc = new CRC32();
                    crc.update(data);

                    ZipEntry entry = new ZipEntry(item.getKey());
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(data.length);
                    entry.setCompressedSize(data.length);
                    entry.setCrc(crc.getValue());
                    entry.setTime(ZIP_TIMESTAMP_MILLIS);
                    output.putNextEntry(entry);
                    output.write(data);
                    output.closeEntry();
                }
            }
            moveAtomically(temporaryArchive, archive);
        } finally {
            Files.deleteIfExists(temporaryArchive);
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
