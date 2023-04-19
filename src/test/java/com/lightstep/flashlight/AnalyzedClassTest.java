package com.lightstep.flashlight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzedClassTest {

    private static final int HIGH_METHOD_INSTRUCTION_COUNT = 10;
    private static final int HIGH_BRANCH_INSTRUCTION_COUNT = 5;
    private static final MethodNameFilter FILTER = new MethodNameFilter();

    private static AnalyzedClass getAnalyzedClass(Class<?> subject) {
        var className = subject.getName();
        try (InputStream stream = subject.getResourceAsStream("/" + className.replace(".", "/") + ".class")) {
            return new AnalyzingClassVisitor(className).analyze(Objects.requireNonNull(stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void baseCase() {
        Runnable subject = new Runnable() {
            @Override
            public void run() {}
        };
        var analyzedClass = getAnalyzedClass(subject.getClass());
        assertEquals(Set.of(), analyzedClass.methodsWithClientCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithRepositoryCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithSynchronize(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, FILTER));
    }

    @Test
    void ignoredMethods() {
        Runnable subject = new Runnable() {
            // Requires Java 16+
            // static {
            //     new SomeClient().call();
            // }

            {
                new SomeClient().call();
            }

            @Override
            public void run() {}

            @Override
            public int hashCode() {
                new SomeClient().call();
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                new SomeClient().call();
                return super.equals(obj);
            }

            @Override
            public String toString() {
                new SomeClient().call();
                return super.toString();
            }
        };
        var analyzedClass = getAnalyzedClass(subject.getClass());
        assertEquals(
                Set.of(),
                analyzedClass.methodsWithClientCalls(
                        new MethodNameFilter("<init>", "<clinit>", "toString", "hashCode", "equals")));
        assertEquals(
                Set.of("<init>", /*"<clinit>",*/ "toString", "hashCode", "equals"),
                analyzedClass.methodsWithClientCalls(FILTER));
    }

    @Test
    void methodsWithSynchronize() {
        Runnable subject = new Runnable() {
            @Override
            public void run() {
                synchronized (new Object()) {
                    new Object();
                }
            }
        };
        var analyzedClass = getAnalyzedClass(subject.getClass());
        assertEquals(Set.of(), analyzedClass.methodsWithClientCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithRepositoryCalls(FILTER));
        assertEquals(Set.of("run"), analyzedClass.methodsWithSynchronize(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, FILTER));
    }

    @Test
    void methodsWithSynchronizeSignature() {
        Runnable subject = new Runnable() {
            @Override
            public synchronized void run() {}
        };
        var analyzedClass = getAnalyzedClass(subject.getClass());
        assertEquals(Set.of(), analyzedClass.methodsWithClientCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithRepositoryCalls(FILTER));
        assertEquals(Set.of("run"), analyzedClass.methodsWithSynchronize(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, FILTER));
    }

    @Test
    void methodsWithClientCalls() {
        Runnable subject = new Runnable() {
            @Override
            public void run() {
                new SomeClient().call();
            }
        };
        var analyzedClass = getAnalyzedClass(subject.getClass());
        assertEquals(Set.of("run"), analyzedClass.methodsWithClientCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithRepositoryCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithSynchronize(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, FILTER));
    }

    @Test
    void methodsWithRepositoryCalls() {
        Runnable subject = new Runnable() {
            @Override
            public void run() {
                new SomeRepository().call();
            }
        };
        var analyzedClass = getAnalyzedClass(subject.getClass());
        assertEquals(Set.of(), analyzedClass.methodsWithClientCalls(FILTER));
        assertEquals(Set.of("run"), analyzedClass.methodsWithRepositoryCalls(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithSynchronize(FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, FILTER));
        assertEquals(Set.of(), analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, FILTER));
    }

    private static class SomeClient implements Callable<Void> {
        @Override
        public Void call() {
            return null;
        }
    }

    private static class SomeRepository implements Callable<Void> {
        @Override
        public Void call() {
            return null;
        }
    }
}
