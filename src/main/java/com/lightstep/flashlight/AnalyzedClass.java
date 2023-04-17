package com.lightstep.flashlight;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class AnalyzedClass {

    private String className;
    private List<AnalyzingMethodVisitor> analyzedMethods;

    public AnalyzedClass(String className, List<AnalyzingMethodVisitor> analyzedMethods) {
        this.className = className;
        this.analyzedMethods = analyzedMethods;
    }

    public Set<String> methodsWithSynchronize(MethodNameFilter filteredMethods) {
        return analyzedMethods.stream()
                .filter(filteredMethods)
                .filter(AnalyzingMethodVisitor::hasSynchronized)
                .map(AnalyzingMethodVisitor::name)
                .collect(Collectors.toSet());
    }

    public Set<String> methodsWithClientCalls(MethodNameFilter filteredMethods) {
        return className.endsWith("Client")
                ? Collections.emptySet()
                : analyzedMethods.stream()
                        .filter(filteredMethods)
                        .filter(AnalyzingMethodVisitor::hasClientCall)
                        .map(AnalyzingMethodVisitor::name)
                        .collect(Collectors.toSet());
    }

    public Set<String> methodsWithRepositoryCalls(MethodNameFilter filteredMethods) {
        return className.endsWith("Repository")
                ? Collections.emptySet()
                : analyzedMethods.stream()
                        .filter(filteredMethods)
                        .filter(AnalyzingMethodVisitor::hasRepositoryCall)
                        .map(AnalyzingMethodVisitor::name)
                        .collect(Collectors.toSet());
    }

    public Set<String> methodsWithHighMethodCount(int methodInstructionCountLimit, MethodNameFilter filteredMethods) {
        return analyzedMethods.stream()
                .filter(filteredMethods)
                .filter(visitor -> visitor.methodCallCount >= methodInstructionCountLimit)
                .map(AnalyzingMethodVisitor::name)
                .collect(Collectors.toSet());
    }

    public Set<String> methodsWithHighBranchCount(int branchInstructionCountLimit, MethodNameFilter filteredMethods) {
        return analyzedMethods.stream()
                .filter(filteredMethods)
                .filter(visitor -> visitor.jumpCount >= branchInstructionCountLimit)
                .map(AnalyzingMethodVisitor::name)
                .collect(Collectors.toSet());
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(className);
        if (!analyzedMethods.isEmpty()) {
            builder.append(" analyzedMethods:").append(analyzedMethods);
        }
        return builder.toString();
    }
}
