package com.lightstep.flashlight;

import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class FlashlightMain {

    private static final int HIGH_METHOD_INSTRUCTION_COUNT = 10;
    private static final int HIGH_BRANCH_INSTRUCTION_COUNT = 5;
    private static final MethodNameFilter EXCLUDED_METHODS =
            new MethodNameFilter("<init>", "<clinit>", "toString", "hashCode", "equals");

    private FlashlightMain() {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("java -jar flashlight.jar [list of jars or class roots]");
            System.out.println("Gradle Example:");
            System.out.println("java -jar flashlight.jar **/build/classes/java/main/");
            System.out.println("Maven Example:");
            System.out.println("java -jar flashlight.jar **/target/classes/");
            return;
        }
        List<String> paths = Arrays.stream(args)
                .map(arg -> arg.replace("production", "test"))
                .collect(Collectors.toList());

        ClassScanner classScanner = new ClassScanner(paths);
        List<AnalyzedClass> analyzedClasses = classScanner.scan();

        System.out.println("Synchronized methods:");
        List<Map.Entry<String, Set<String>>> methodsWithSynchronize = analyzedClasses.stream()
                .map(analyzedClass ->
                        entry(analyzedClass.getClassName(), analyzedClass.methodsWithSynchronize(EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();
        methodsWithSynchronize.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("Client invocation methods:");
        List<Map.Entry<String, Set<String>>> methodsWithClientCalls = analyzedClasses.stream()
                .map(analyzedClass ->
                        entry(analyzedClass.getClassName(), analyzedClass.methodsWithClientCalls(EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();
        methodsWithClientCalls.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("Repository invocation methods:");
        List<Map.Entry<String, Set<String>>> methodsWithRepositoryCalls = analyzedClasses.stream()
                .map(analyzedClass ->
                        entry(analyzedClass.getClassName(), analyzedClass.methodsWithRepositoryCalls(EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();
        methodsWithRepositoryCalls.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("High call count methods:");
        List<Map.Entry<String, Set<String>>> methodsWithHighMethodCount = analyzedClasses.stream()
                .map(analyzedClass -> entry(
                        analyzedClass.getClassName(),
                        analyzedClass.methodsWithHighMethodCount(HIGH_METHOD_INSTRUCTION_COUNT, EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();
        methodsWithHighMethodCount.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("High branch count methods:");
        List<Map.Entry<String, Set<String>>> methodsWithHighBranchCount = analyzedClasses.stream()
                .map(analyzedClass -> entry(
                        analyzedClass.getClassName(),
                        analyzedClass.methodsWithHighBranchCount(HIGH_BRANCH_INSTRUCTION_COUNT, EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();
        methodsWithHighBranchCount.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        TreeMap<String, Set<String>> methods = new TreeMap<>();
        methodsWithSynchronize.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithClientCalls.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithRepositoryCalls.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithHighMethodCount.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithHighBranchCount.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));

        System.out.println("System Property:");
        System.out.print("-Dotel.instrumentation.methods.include=");
        for (Map.Entry<String, Set<String>> analyzedClass : methods.entrySet()) {
            System.out.print(
                    analyzedClass.getKey() + analyzedClass.getValue().toString().replace(" ", "") + ";");
        }

        System.out.println("\n\nEnvironment Variable:");
        System.out.println("OTEL_INSTRUMENTATION_METHODS_INCLUDE=\"\\");
        for (Map.Entry<String, Set<String>> analyzedClass : methods.entrySet()) {
            System.out.print(
                    analyzedClass.getKey() + analyzedClass.getValue().toString().replace(" ", "") + ";\\");
        }
        System.out.println("\"");
    }
}
