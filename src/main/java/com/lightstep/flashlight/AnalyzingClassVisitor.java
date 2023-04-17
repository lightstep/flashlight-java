package com.lightstep.flashlight;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM9;

class AnalyzingClassVisitor extends ClassVisitor {
    final List<AnalyzingMethodVisitor> analyzedMethods = new ArrayList<>();
    private final String className;

    protected AnalyzingClassVisitor(String className) {
        super(ASM9);
        this.className = className;
    }

    AnalyzedClass analyze(InputStream resourceAsStream) throws IOException {
        ClassReader classReader = new ClassReader(resourceAsStream);
        classReader.accept(this, 0);
        return new AnalyzedClass(className, analyzedMethods);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        AnalyzingMethodVisitor methodVisitor = new AnalyzingMethodVisitor(name, access);
        analyzedMethods.add(methodVisitor);
        return methodVisitor;
    }
}
