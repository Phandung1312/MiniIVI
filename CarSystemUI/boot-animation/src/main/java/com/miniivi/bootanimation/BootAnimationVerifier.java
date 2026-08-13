package com.miniivi.bootanimation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Validates MiniIVI boot animation archives before they are deployed. */
public final class BootAnimationVerifier {
    private BootAnimationVerifier() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 0) {
            throw new IllegalArgumentException("Expected at least one boot animation archive.");
        }
        for (String argument : arguments) {
            Path archive = Path.of(argument);
            verify(archive);
            System.out.println("Verified MiniIVI boot animation: " + archive.toAbsolutePath());
        }
    }

    public static void verify(Path archive) throws IOException {
        if (!Files.isRegularFile(archive)) {
            throw new IOException("Boot animation archive does not exist: " + archive);
        }

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            List<? extends ZipEntry> entries = listEntries(zip);
            verifyEntrySet(entries);
            verifyStorageMethod(entries);
            verifyTextEntry(zip, "desc.txt", BootAnimationGenerator.DESCRIPTION);
            verifyTextEntry(
                    zip,
                    "intro/trim.txt",
                    expectedTrimFile(BootAnimationGenerator.INTRO_FRAMES));
            verifyTextEntry(
                    zip,
                    "shimmer/trim.txt",
                    expectedTrimFile(BootAnimationGenerator.SHIMMER_FRAMES));
            verifyFrames(zip, "intro", BootAnimationGenerator.INTRO_FRAMES, false);
            verifyFrames(zip, "shimmer", BootAnimationGenerator.SHIMMER_FRAMES, true);
            verifyFormerLoadingRingAreaIsEmpty(zip);
            verifyShimmerLoopBoundary(zip);
        }
    }

    private static List<? extends ZipEntry> listEntries(ZipFile zip) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            entries.add(enumeration.nextElement());
        }
        return entries;
    }

    private static void verifyEntrySet(List<? extends ZipEntry> entries) throws IOException {
        List<String> expected = new ArrayList<>();
        expected.add("desc.txt");
        addExpectedFrames(expected, "intro", BootAnimationGenerator.INTRO_FRAMES);
        expected.add("intro/trim.txt");
        addExpectedFrames(expected, "shimmer", BootAnimationGenerator.SHIMMER_FRAMES);
        expected.add("shimmer/trim.txt");

        List<String> actual = new ArrayList<>();
        Set<String> uniqueNames = new HashSet<>();
        for (ZipEntry entry : entries) {
            if (entry.isDirectory()) {
                throw new IOException("Directory entries are not allowed: " + entry.getName());
            }
            if (!uniqueNames.add(entry.getName())) {
                throw new IOException("Duplicate ZIP entry: " + entry.getName());
            }
            actual.add(entry.getName());
        }
        if (!actual.equals(expected)) {
            throw new IOException(
                    "Unexpected ZIP entries or ordering. Expected " + expected + " but found " + actual);
        }
    }

    private static void addExpectedFrames(List<String> entries, String part, int count) {
        for (int index = 0; index < count; index++) {
            entries.add(String.format("%s/frame%03d.png", part, index));
        }
    }

    private static void verifyStorageMethod(List<? extends ZipEntry> entries) throws IOException {
        for (ZipEntry entry : entries) {
            if (entry.getMethod() != ZipEntry.STORED) {
                throw new IOException("ZIP entry is compressed instead of stored: " + entry.getName());
            }
            if (entry.getSize() != entry.getCompressedSize()) {
                throw new IOException("Stored ZIP entry has mismatched sizes: " + entry.getName());
            }
        }
    }

    private static void verifyTextEntry(ZipFile zip, String name, String expected) throws IOException {
        ZipEntry entry = requiredEntry(zip, name);
        try (InputStream input = zip.getInputStream(entry)) {
            String actual = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (!expected.equals(actual)) {
                throw new IOException("Unexpected content in " + name + ": " + actual);
            }
        }
    }

    private static String expectedTrimFile(int frameCount) {
        String line = BootAnimationGenerator.FRAME_WIDTH
                + "x"
                + BootAnimationGenerator.FRAME_HEIGHT
                + "+"
                + BootAnimationGenerator.FRAME_X
                + "+"
                + BootAnimationGenerator.FRAME_Y
                + "\n";
        return line.repeat(frameCount);
    }

    private static void verifyFrames(ZipFile zip, String part, int count, boolean requireEveryFrame)
            throws IOException {
        int lastVisiblePixelCount = 0;
        for (int index = 0; index < count; index++) {
            String name = String.format("%s/frame%03d.png", part, index);
            ZipEntry entry = requiredEntry(zip, name);
            BufferedImage image;
            try (InputStream input = zip.getInputStream(entry)) {
                image = ImageIO.read(input);
            }
            if (image == null) {
                throw new IOException("Frame is not a readable PNG: " + name);
            }
            if (image.getWidth() != BootAnimationGenerator.FRAME_WIDTH
                    || image.getHeight() != BootAnimationGenerator.FRAME_HEIGHT) {
                throw new IOException(
                        "Unexpected frame dimensions for "
                                + name
                                + ": "
                                + image.getWidth()
                                + "x"
                                + image.getHeight());
            }

            int visiblePixelCount = countVisiblePixels(image);
            if (requireEveryFrame && visiblePixelCount < 1_000) {
                throw new IOException("Frame has insufficient visible content: " + name);
            }
            lastVisiblePixelCount = visiblePixelCount;
        }
        if (lastVisiblePixelCount < 1_000) {
            throw new IOException("The final frame of " + part + " has insufficient visible content.");
        }
    }

    private static int countVisiblePixels(BufferedImage image) {
        int visible = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) {
                    visible++;
                }
            }
        }
        return visible;
    }

    private static void verifyFormerLoadingRingAreaIsEmpty(ZipFile zip) throws IOException {
        for (int index = 0; index < BootAnimationGenerator.SHIMMER_FRAMES; index++) {
            String name = String.format("shimmer/frame%03d.png", index);
            BufferedImage image = readImage(zip, name);
            for (int y = 285; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) {
                        throw new IOException("Former loading-ring area contains pixels in " + name);
                    }
                }
            }
        }
    }

    private static void verifyShimmerLoopBoundary(ZipFile zip) throws IOException {
        BufferedImage first = readImage(zip, "shimmer/frame000.png");
        BufferedImage last = readImage(
                zip,
                String.format("shimmer/frame%03d.png", BootAnimationGenerator.SHIMMER_FRAMES - 1));
        int changedPixels = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != last.getRGB(x, y)) {
                    changedPixels++;
                }
            }
        }
        if (changedPixels > 250) {
            throw new IOException("Shimmer loop boundary changes too many pixels: " + changedPixels);
        }
    }

    private static BufferedImage readImage(ZipFile zip, String name) throws IOException {
        ZipEntry entry = requiredEntry(zip, name);
        try (InputStream input = zip.getInputStream(entry)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IOException("Frame is not a readable PNG: " + name);
            }
            return image;
        }
    }

    private static ZipEntry requiredEntry(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("Missing ZIP entry: " + name);
        }
        return entry;
    }
}
