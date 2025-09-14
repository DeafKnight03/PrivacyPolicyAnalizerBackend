package com.example.myapp.dto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class ProcessRunner {
    private ProcessRunner() {}

    public static void runScript(List<String> cmd, File cwd) throws IOException, InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null) pb.directory(cwd);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);  // don't capture stdout
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);   // don't capture stderr
        Process p = pb.start();
        try (var in = p.getOutputStream()) { /* close stdin so the script won't wait for input */ }
        int exit = p.waitFor();               // wait until finished
        if (exit != 0) throw new RuntimeException("Script failed with exit code " + exit);
    }

}
