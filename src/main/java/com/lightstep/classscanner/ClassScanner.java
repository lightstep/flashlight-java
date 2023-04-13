package com.lightstep.classscanner;

import com.google.common.reflect.ClassPath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassScanner {
    private final List<URL> targetPaths;

    public ClassScanner(List<String> targetPaths) {
        this.targetPaths = targetPaths.stream()
                .map(File::new)
                .peek(file -> {
                    if (!file.exists()) {
                        throw new IllegalArgumentException(new FileNotFoundException(file.getAbsolutePath()));
                    }
                })
                .map(File::toURI)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<AnalyzedClass> scan() throws IOException {
        try (URLClassLoader classLoader = new URLClassLoader(targetPaths.toArray(new URL[0]), null)) {
            List<AnalyzedClass> analyzedClasses = new ArrayList<>();

            for (ClassPath.ClassInfo info : ClassPath.from(classLoader).getTopLevelClasses()) {
                //        System.out.println(info.getName());
                AnalyzedClass analyzedClass = new AnalyzedClass(
                        info.getName(),
                        Objects.requireNonNull(classLoader.getResourceAsStream(info.getResourceName())));
                if (analyzedClass.isInteresting()) {
                    analyzedClasses.add(analyzedClass);
                }
            }
            return analyzedClasses;
        }
    }
}
