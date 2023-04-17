package com.lightstep.flashlight;

import java.util.concurrent.Callable;

public class Example {
    public synchronized int synchronizedMethod() {
        int i = 1;
        i++;
        return i;
    }

    public Object methodWithSynchronized() {
        Object lock = new Object();
        synchronized (lock) {
            return lock;
        }
    }

    public Object lambdaWithSynchronized() throws Exception {
        Object lock = new Object();
        Callable<?> callable = () -> {
            synchronized (lock) {
                return lock;
            }
        };
        return callable.call();
    }

    public static Object staticMethodWithSynchronized() {
        Object lock = new Object();
        synchronized (lock) {
            return lock;
        }
    }

    public int methodWithBranch(int inc) {
        if (inc == 1) {
            return 1;
        }
        if (inc == 2) {
            return 2;
        }
        if (inc == 3) {
            return 3;
        }
        if (inc == 4) {
            return 4;
        }
        if (inc == 5) {
            return 5;
        } else {
            return -1;
        }
    }

    public int methodWithSwitch(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return -1;
        }
    }

    void methodWithMethodCalls() throws Exception {
        new Object();
        synchronizedMethod();
        methodWithSynchronized();
        staticMethodWithSynchronized();
        lambdaWithSynchronized();
        methodWithBranch(1);
        methodWithSwitch(1);
        methodWithClientCall();
        methodWithStaticClientCall();
        methodWithRepositoryCall();
        methodWithStaticRepositoryCall();
    }

    void methodWithClientCall() {
        new SomeClient().call();
    }

    void lambdaWithClientCall() {
        Runnable runnable = () -> {
            new SomeClient().call();
        };
        runnable.run();
    }

    void methodWithStaticClientCall() {
        SomeClient.staticCall();
    }

    void methodWithRepositoryCall() {
        new SomeRepository().call();
    }

    void lambdaWithRepositoryCall() {
        Runnable runnable = () -> {
            new SomeRepository().call();
        };
        runnable.run();
    }

    void methodWithStaticRepositoryCall() {
        SomeRepository.staticCall();
    }

    private static class SomeClient implements Callable<Void> {
        public static Void staticCall() {
            return null;
        }

        @Override
        public Void call() {
            return null;
        }
    }

    private static class SomeRepository implements Callable<Void> {
        public static Void staticCall() {
            return null;
        }

        @Override
        public Void call() {
            return null;
        }
    }
}
