package com.lightstep.flashlight;

import com.google.common.reflect.ClassPath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class ClassScanner {
    private final List<URL> targetPaths;

    public ClassScanner(Collection<File> targetPaths) {
        this.targetPaths = targetPaths.stream()
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
                try (InputStream resource = classLoader.getResourceAsStream(info.getResourceName())) {
                    AnalyzedClass analyzedClass =
                            new AnalyzingClassVisitor(info.getName()).analyze(Objects.requireNonNull(resource));
                    analyzedClasses.add(analyzedClass);
                }
            }
            return analyzedClasses;
        }
    }
}
