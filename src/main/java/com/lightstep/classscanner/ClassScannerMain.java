package com.lightstep.classscanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassScannerMain {

    private ClassScannerMain() {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println(
                    "java -jar opentelemetry-instrumentation-class-scanner.jar [list of jars or class roots]");
            System.out.println("Gradle Example:");
            System.out.println("java -jar opentelemetry-instrumentation-class-scanner.jar **/build/classes/java/main/");
            System.out.println("Maven Example:");
            System.out.println("java -jar opentelemetry-instrumentation-class-scanner.jar **/target/classes/");
            return;
        }
        List<String> paths = Arrays.stream(args)
                .map(arg -> arg.replace("production", "test"))
                .collect(Collectors.toList());

        ClassScanner classScanner = new ClassScanner(paths);
        List<AnalyzedClass> analyzedClasses = classScanner.scan();

        System.out.println("Synchronized methods:");
        analyzedClasses.stream()
                .filter(analyzedClass -> !analyzedClass.methodsWithSynchronize.isEmpty())
                .forEach(analyzedClass ->
                        System.out.println("\t" + analyzedClass.className + analyzedClass.methodsWithSynchronize));

        System.out.println("Client invocation methods:");
        analyzedClasses.stream()
                .filter(analyzedClass -> !analyzedClass.methodsWithClientCalls.isEmpty())
                .forEach(analyzedClass ->
                        System.out.println("\t" + analyzedClass.className + analyzedClass.methodsWithClientCalls));

        System.out.println("Repository invocation methods:");
        analyzedClasses.stream()
                .filter(analyzedClass -> !analyzedClass.methodsWithRepositoryCalls.isEmpty())
                .forEach(analyzedClass ->
                        System.out.println("\t" + analyzedClass.className + analyzedClass.methodsWithRepositoryCalls));

        System.out.println("High call count methods:");
        analyzedClasses.stream()
                .filter(analyzedClass -> !analyzedClass.methodsWithHighMethodCount.isEmpty())
                .forEach(analyzedClass ->
                        System.out.println("\t" + analyzedClass.className + analyzedClass.methodsWithHighMethodCount));

        System.out.println("High branch count methods:");
        analyzedClasses.stream()
                .filter(analyzedClass -> !analyzedClass.methodsWithHighBranchCount.isEmpty())
                .forEach(analyzedClass ->
                        System.out.println("\t" + analyzedClass.className + analyzedClass.methodsWithHighBranchCount));

        System.out.println("System Property:");
        System.out.print("-Dotel.instrumentation.methods.include=");
        for (AnalyzedClass analyzedClass : analyzedClasses) {
            Set<String> methods = new HashSet<>();
            methods.addAll(analyzedClass.methodsWithSynchronize);
            methods.addAll(analyzedClass.methodsWithClientCalls);
            methods.addAll(analyzedClass.methodsWithRepositoryCalls);
            methods.addAll(analyzedClass.methodsWithHighMethodCount);
            methods.addAll(analyzedClass.methodsWithHighBranchCount);
            System.out.print(analyzedClass.className + methods.toString().replace(" ", "") + ";");
        }

        System.out.println("\n\nEnvironment Variable:");
        System.out.println("OTEL_INSTRUMENTATION_METHODS_INCLUDE=\"\\");
        for (AnalyzedClass analyzedClass : analyzedClasses) {
            Set<String> methods = new HashSet<>();
            methods.addAll(analyzedClass.methodsWithSynchronize);
            methods.addAll(analyzedClass.methodsWithClientCalls);
            methods.addAll(analyzedClass.methodsWithRepositoryCalls);
            methods.addAll(analyzedClass.methodsWithHighMethodCount);
            methods.addAll(analyzedClass.methodsWithHighBranchCount);
            System.out.println(analyzedClass.className + methods.toString().replace(" ", "") + ";\\");
        }
        System.out.println("\"");
    }
}
