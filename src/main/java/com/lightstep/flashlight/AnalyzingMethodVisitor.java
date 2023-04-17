package com.lightstep.flashlight;

import net.shadew.asm.descriptor.MethodDescriptor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static org.objectweb.asm.Opcodes.ASM9;

class AnalyzingMethodVisitor extends MethodVisitor {
    private final String name;
    private boolean hasSynchronized;
    private boolean hasClientCall = false;
    private boolean hasRepositoryCall = false;
    int methodCallCount = 0;
    int jumpCount = 0;

    protected AnalyzingMethodVisitor(String name, int access) {
        super(ASM9);
        this.name = name;
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
