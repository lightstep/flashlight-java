package com.lightstep.flashlight;

import picocli.CommandLine;

public class ManifestVersion implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String implementationVersion = ManifestVersion.class.getPackage().getImplementationVersion();
        return new String[] {implementationVersion};
    }
}
