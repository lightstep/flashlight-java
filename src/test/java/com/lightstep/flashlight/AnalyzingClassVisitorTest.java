package com.lightstep.flashlight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzingClassVisitorTest {

    private static final int HIGH_METHOD_INSTRUCTION_COUNT = 10;
    private static final int HIGH_BRANCH_INSTRUCTION_COUNT = 5;
    private static final MethodNameFilter EXCLUDED_METHODS = new MethodNameFilter();

    private static AnalyzedClass getAnalyzedClass(Class<?> subject) {
        var className = subject.getName();
        try (InputStream stream = subject.getResourceAsStream("/" + className.replace(".", "/") + ".class")) {
            return new AnalyzingClassVisitor(className).analyze(Objects.requireNonNull(stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final AnalyzedClass analyzedClass = getAnalyzedClass(Example.class);

    @Test
    void methodsWithClientCalls() {
        assertEquals(
                Set.of("methodWithClientCall", "methodWithStaticClientCall", "lambda$lambdaWithClientCall$1"),
                analyzedClass.methodsWithClientCalls(EXCLUDED_METHODS));
    }

    @Test
    void methodsWithRepositoryCalls() {
        assertEquals(
                Set.of("methodWithRepositoryCall", "methodWithStaticRepositoryCall", "lambda$lambdaWithRepositoryCall$2"),
                analyzedClass.methodsWithRepositoryCalls(EXCLUDED_METHODS));
    }

    @Test
    void methodsWithSynchronize() {
        assertEquals(
                Set.of("synchronizedMethod", "methodWithSynchronized", "lambda$lambdaWithSynchronized$0","staticMethodWithSynchronized"),
                analyzedClass.methodsWithSynchronize(EXCLUDED_METHODS));
    }

    @Test
    void methodsWithHighBranchCount() {
        assertEquals(
                Set.of("methodWithBranch"),
                analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, EXCLUDED_METHODS));
    }

    @Test
    void methodsWithHighMethodCount() {
        assertEquals(
                Set.of("methodWithMethodCalls"), analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, EXCLUDED_METHODS));
    }
}