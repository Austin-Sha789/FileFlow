package com.austin.fileflow.update;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/** Release precedence; build metadata does not turn an equal version into an update. */
public record SemanticVersion(BigInteger major, BigInteger minor, BigInteger patch, List<String> pre) implements Comparable<SemanticVersion> {
    private static final Pattern VERSION = Pattern.compile("^[vV]?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\.(0|[1-9][0-9]*))?(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$");
    public SemanticVersion { pre = List.copyOf(pre); }
    public static SemanticVersion parse(String text) {
        var match = VERSION.matcher(Objects.requireNonNull(text).trim());
        if (!match.matches()) throw new IllegalArgumentException("Invalid release version");
        if (match.group(5) != null && Arrays.stream(match.group(5).split("\\.", -1)).anyMatch(String::isEmpty)) throw new IllegalArgumentException("Invalid build metadata");
        List<String> pre = match.group(4) == null ? List.of() : List.of(match.group(4).split("\\.", -1));
        for (String part : pre) if (part.isEmpty() || (part.matches("[0-9]+") && part.length() > 1 && part.startsWith("0"))) throw new IllegalArgumentException("Invalid prerelease version");
        return new SemanticVersion(new BigInteger(match.group(1)), new BigInteger(match.group(2)), new BigInteger(match.group(3) == null ? "0" : match.group(3)), pre);
    }
    public String core() { return major + "." + minor + "." + patch; }
    @Override public int compareTo(SemanticVersion other) {
        for (int compared : new int[]{major.compareTo(other.major), minor.compareTo(other.minor), patch.compareTo(other.patch)}) if (compared != 0) return compared;
        if (pre.isEmpty() || other.pre.isEmpty()) return pre.isEmpty() ? (other.pre.isEmpty() ? 0 : 1) : -1;
        for (int index = 0; index < Math.min(pre.size(), other.pre.size()); index++) {
            String left = pre.get(index), right = other.pre.get(index);
            boolean ln = left.matches("[0-9]+"), rn = right.matches("[0-9]+");
            int compared = ln && rn ? new BigInteger(left).compareTo(new BigInteger(right)) : ln != rn ? (ln ? -1 : 1) : left.compareTo(right);
            if (compared != 0) return compared;
        }
        return Integer.compare(pre.size(), other.pre.size());
    }
}
