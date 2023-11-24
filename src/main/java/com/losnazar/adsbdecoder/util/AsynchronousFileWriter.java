package com.losnazar.adsbdecoder.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.*;

@Component
public class AsynchronousFileWriter {
    private static final int THREAD_NUMBER = 5;
    private static final String RESULT_FILE = "src/main/resources/result.txt";

    public void write(List<String> data) {
        Path path = Paths.get(RESULT_FILE);
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NUMBER);
        List<CompletableFuture<Void>> writeFutures = data.stream()
            .map(line -> CompletableFuture.runAsync(() -> writeToFile(path, line), executorService))
            .toList();

        CompletableFuture<Void> allWrites =
                CompletableFuture.allOf(writeFutures.toArray(new CompletableFuture[0]));

        try {
            allWrites.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        } finally {
        executorService.shutdown();
    }
}

    private static void writeToFile(Path filePath, String line) {
        byte[] byteData = (line + System.lineSeparator()).getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(byteData);

        try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        )) {
            synchronized (AsynchronousFileWriter.class) {
                Future<Integer> writeResult = fileChannel.write(buffer, fileChannel.size());
                writeResult.get();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }
}
