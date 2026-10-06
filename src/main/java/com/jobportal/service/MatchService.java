package com.jobportal.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class MatchService {
    private MatchService() {}

    public static Set<String> split(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim).map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public static String normalize(String csv) {
        return String.join(",", split(csv));
    }

    /** Percentage of the job's required skills that the candidate has. */
    public static int score(String candidateSkills, String requiredSkills) {
        Set<String> required = split(requiredSkills);
        if (required.isEmpty()) return 0;
        Set<String> have = split(candidateSkills);
        long matched = required.stream().filter(have::contains).count();
        return (int) Math.round(matched * 100.0 / required.size());
    }

    /** Skills the job requires that the candidate does not have, as "a, b". */
    public static String missing(String candidateSkills, String requiredSkills) {
        Set<String> have = split(candidateSkills);
        return split(requiredSkills).stream()
                .filter(s -> !have.contains(s))
                .sorted()
                .collect(Collectors.joining(", "));
    }
}