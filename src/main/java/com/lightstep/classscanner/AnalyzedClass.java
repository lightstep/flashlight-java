package com.lightstep.classscanner;

import net.shadew.asm.descriptor.MethodDescriptor;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static org.objectweb.asm.Opcodes.ASM9;

public class AnalyzedClass {
    private static final int HIGH_METHOD_INSTRUCTION_COUNT = 10;
    private static final int HIGH_BRANCH_INSTRUCTION_COUNT = 5;

    private static final Set<String> BASIC_METHOD =
            new HashSet<>(Arrays.asList("<init>", "<clinit>", "toString", "hashCode", "equals"));
    public final String className;
    private final List<AnalyzingMethodVisitor> analyzedMethods = new ArrayList<>();
    public final Set<String> methodsWithSynchronize;
    public final Set<String> methodsWithClientCalls;
    public final Set<String> methodsWithRepositoryCalls;
    public final Set<String> methodsWithHighMethodCount;
    public final Set<String> methodsWithHighBranchCount;

    public AnalyzedClass(String className, InputStream resourceAsStream) throws IOException {
        this.className = className;
        ClassReader classReader = new ClassReader(resourceAsStream);
        classReader.accept(new AnalyzingClassVisitor(), 0);
        methodsWithSynchronize = analyzedMethods.stream()
                .filter(AnalyzingMethodVisitor::notBasicMethod)
                .filter(AnalyzingMethodVisitor::hasSynchronized)
                .map(AnalyzingMethodVisitor::name)
                .collect(Collectors.toSet());
        methodsWithClientCalls = className.endsWith("Client")
                ? Collections.emptySet()
                : analyzedMethods.stream()
                        .filter(AnalyzingMethodVisitor::notBasicMethod)
                        .filter(AnalyzingMethodVisitor::hasClientCall)
                        .map(AnalyzingMethodVisitor::name)
                        .collect(Collectors.toSet());
        methodsWithRepositoryCalls = className.endsWith("Repository")
                ? Collections.emptySet()
                : analyzedMethods.stream()
                        .filter(AnalyzingMethodVisitor::notBasicMethod)
                        .filter(AnalyzingMethodVisitor::hasRepositoryCall)
                        .map(AnalyzingMethodVisitor::name)
                        .collect(Collectors.toSet());
        methodsWithHighMethodCount = analyzedMethods.stream()
                .filter(AnalyzingMethodVisitor::notBasicMethod)
                .filter(visitor -> visitor.methodCallCount >= HIGH_METHOD_INSTRUCTION_COUNT)
                .map(AnalyzingMethodVisitor::name)
                .collect(Collectors.toSet());
        methodsWithHighBranchCount = analyzedMethods.stream()
                .filter(AnalyzingMethodVisitor::notBasicMethod)
                .filter(visitor -> visitor.jumpCount >= HIGH_BRANCH_INSTRUCTION_COUNT)
                .map(AnalyzingMethodVisitor::name)
                .collect(Collectors.toSet());
        return;
    }

    public boolean isInteresting() {
        return !(methodsWithSynchronize.isEmpty()
                && methodsWithClientCalls.isEmpty()
                && methodsWithRepositoryCalls.isEmpty()
                && methodsWithHighMethodCount.isEmpty()
                && methodsWithHighBranchCount.isEmpty());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(className);
        if (!methodsWithSynchronize.isEmpty()) {
            builder.append(" methodsWithSynchronize:").append(methodsWithSynchronize);
        }
        if (!methodsWithHighMethodCount.isEmpty()) {
            builder.append(" methodsWithHighMethodCount:").append(methodsWithHighMethodCount);
        }
        if (!methodsWithHighBranchCount.isEmpty()) {
            builder.append(" methodsWithHighBranchCount:").append(methodsWithHighBranchCount);
        }
        return builder.toString();
    }

    private class AnalyzingClassVisitor extends ClassVisitor {
        protected AnalyzingClassVisitor() {
            super(ASM9);
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
            AnalyzingMethodVisitor methodVisitor = new AnalyzingMethodVisitor(name, access);
            analyzedMethods.add(methodVisitor);
            return methodVisitor;
        }
    }

    private static class AnalyzingMethodVisitor extends MethodVisitor {
        private final String name;
        private final boolean isBasicMethod;
        private boolean hasSynchronized;
        private boolean hasClientCall = false;
        private boolean hasRepositoryCall = false;
        int methodCallCount = 0;
        int jumpCount = 0;

        protected AnalyzingMethodVisitor(String name, int access) {
            super(ASM9);
            this.name = name;
            isBasicMethod = BASIC_METHOD.contains(name)
                    || name.startsWith("get")
                    || name.startsWith("set")
                    || name.startsWith("is")
                    || name.startsWith("lambda$");
            hasSynchronized = (access & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED;
        }

        @Override
        public String toString() {
            return "AnalyzingMethodVisitor{"
                    + "name='"
                    + name
                    + '\''
                    + ", hasSynchronized="
                    + hasSynchronized
                    + ", methodCallCount="
                    + methodCallCount
                    + ", jumpCount="
                    + jumpCount
                    + '}';
        }

        @Override
        public void visitInsn(int opcode) {
            hasSynchronized |= opcode == Opcodes.MONITORENTER;
            super.visitInsn(opcode);
        }

        @Override
        public void visitInvokeDynamicInsn(
                String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            methodCallCount++;
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            methodCallCount++;
            MethodDescriptor methodDescriptor = MethodDescriptor.parse(descriptor);
            String returnType = methodDescriptor.returnType().toAsm().getInternalName();

            // check if this looks like a builder method
            if (!owner.equals(returnType)) {
                hasClientCall |=
                        owner.endsWith("Client") && !returnType.endsWith("Client") && !returnType.endsWith("Builder");
                hasRepositoryCall |= owner.endsWith("Repository")
                        && !returnType.endsWith("Repository")
                        && !returnType.endsWith("Builder");
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            jumpCount++;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            jumpCount++;
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            jumpCount++;
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        public String name() {
            return name;
        }

        public boolean notBasicMethod() {
            return !isBasicMethod;
        }

        public boolean hasSynchronized() {
            return hasSynchronized;
        }

        public boolean hasClientCall() {
            return hasClientCall;
        }

        public boolean hasRepositoryCall() {
            return hasRepositoryCall;
        }
    }
}
