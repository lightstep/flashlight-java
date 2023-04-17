package com.lightstep.flashlight;

import java.util.Set;
import java.util.function.Predicate;

class MethodNameFilter implements Predicate<AnalyzingMethodVisitor> {
    private final Set<String> excludedMethodNames;

    public MethodNameFilter(String... excludedMethodNames) {
        this.excludedMethodNames = Set.of(excludedMethodNames);
    }

    @Override
    public boolean test(AnalyzingMethodVisitor analyzingMethodVisitor) {
        return !excludedMethodNames.contains(analyzingMethodVisitor.name());
    }
}
