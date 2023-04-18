package com.lightstep.flashlight;

import com.google.common.collect.Sets;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import static java.util.Map.entry;

@CommandLine.Command(
        name = "java -jar flashlight.jar",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersion.class,
        description =
                """
                Scans the provided classpath and identifies 'interesting' methods to instrument with OpenTelemetry.
                 Gradle: java -jar flashlight.jar **/build/classes/java/main/
                 Maven:  java -jar flashlight.jar **/target/classes/
                 """)
public class FlashlightMain implements Callable<Integer> {

    private static final MethodNameFilter EXCLUDED_METHODS =
            new MethodNameFilter("<init>", "<clinit>", "toString", "hashCode", "equals");

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec; // injected by picocli

    @CommandLine.Option(
            names = {"-c", "--call"},
            paramLabel = "NUMBER",
            description = "number of method call instructions considered interesting (default: ${DEFAULT-VALUE})")
    int methodInstructionCountThreshold = 10;

    @CommandLine.Option(
            names = {"-b", "--branch"},
            paramLabel = "NUMBER",
            description =
                    "number of branch (if/while/for) instructions considered interesting (default: ${DEFAULT-VALUE})")
    int branchInstructionCountThreshold = 5;

    Set<File> paths;

    @CommandLine.Parameters(arity = "1..*", description = "the folders or jar files to scan")
    public void setPaths(Set<File> paths) {
        for (File file : paths) {
            if (!file.exists()) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(), String.format("Path does not exist: '%s'", file));
            }
        }
        this.paths = paths;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new FlashlightMain()).execute(args);
        System.exit(exitCode);
    }

    private FlashlightMain() {}

    @Override
    public Integer call() throws Exception {
        ClassScanner classScanner = new ClassScanner(paths);
        List<AnalyzedClass> analyzedClasses = classScanner.scan();

        List<Map.Entry<String, Set<String>>> methodsWithSynchronize = analyzedClasses.stream()
                .map(analyzedClass ->
                        entry(analyzedClass.getClassName(), analyzedClass.methodsWithSynchronize(EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();

        List<Map.Entry<String, Set<String>>> methodsWithClientCalls = analyzedClasses.stream()
                .map(analyzedClass ->
                        entry(analyzedClass.getClassName(), analyzedClass.methodsWithClientCalls(EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();

        List<Map.Entry<String, Set<String>>> methodsWithRepositoryCalls = analyzedClasses.stream()
                .map(analyzedClass ->
                        entry(analyzedClass.getClassName(), analyzedClass.methodsWithRepositoryCalls(EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();

        List<Map.Entry<String, Set<String>>> methodsWithHighMethodCount = analyzedClasses.stream()
                .map(analyzedClass -> entry(
                        analyzedClass.getClassName(),
                        analyzedClass.methodsWithHighMethodCount(methodInstructionCountThreshold, EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();

        List<Map.Entry<String, Set<String>>> methodsWithHighBranchCount = analyzedClasses.stream()
                .map(analyzedClass -> entry(
                        analyzedClass.getClassName(),
                        analyzedClass.methodsWithHighBranchCount(branchInstructionCountThreshold, EXCLUDED_METHODS)))
                .filter(analyzedClass -> !analyzedClass.getValue().isEmpty())
                .toList();

        TreeMap<String, Set<String>> methods = new TreeMap<>();
        methodsWithSynchronize.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithClientCalls.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithRepositoryCalls.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithHighMethodCount.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));
        methodsWithHighBranchCount.forEach(entry -> methods.merge(entry.getKey(), entry.getValue(), Sets::union));

        System.out.println("Synchronized methods:");
        methodsWithSynchronize.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("Client invocation methods:");
        methodsWithClientCalls.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("Repository invocation methods:");
        methodsWithRepositoryCalls.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("High call count methods:");
        methodsWithHighMethodCount.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

        System.out.println("High branch count methods:");
        methodsWithHighBranchCount.forEach(
                analyzedClass -> System.out.println("\t" + analyzedClass.getKey() + analyzedClass.getValue()));

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
        return 0;
    }
}
