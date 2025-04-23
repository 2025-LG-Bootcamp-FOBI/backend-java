package com.example.fobiserver.util;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

import static com.example.fobiserver.constant.Constant.*;

@Component
public class PythonExecutor {

    public String executePythonScript(String filePath, String[] args) throws IOException {
        String[] command = new String[args.length + 3];
        command[0] = PYTHON_PATH;
        command[1] = SCRIPT_PATH;
        command[2] = filePath;
        System.arraycopy(args, 0, command, 2, args.length);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // 출력 스트림 처리
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return output.toString();
            }
        });

        // 프로세스 종료 코드 확인 (타임아웃 추가)
        try {
            boolean finished = process.waitFor(60, TimeUnit.SECONDS); // 60초 타임아웃
            if (!finished) {
                process.destroy(); // 프로세스 강제 종료
                throw new IOException("Python script execution timed out");
            }

//            int exitCode = process.exitValue();
//            if (exitCode != 0) {
//                throw new IOException("Python script execution failed with exit code " + exitCode);
//            }

            return outputFuture.get(); // 출력 결과 반환
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Python script execution interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("Error reading script output", e);
        } finally {
            executor.shutdown();
        }
    }
}