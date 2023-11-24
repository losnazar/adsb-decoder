package com.losnazar.adsbdecoder.util;

import de.serosystems.lib1090.StatefulModeSDecoder;
import de.serosystems.lib1090.exceptions.BadFormatException;
import de.serosystems.lib1090.exceptions.UnspecifiedFormatError;
import de.serosystems.lib1090.msgs.ModeSDownlinkMsg;
import de.serosystems.lib1090.msgs.adsb.AirspeedHeadingMsg;
import de.serosystems.lib1090.msgs.adsb.IdentificationMsg;
import de.serosystems.lib1090.msgs.adsb.SurfacePositionV0Msg;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class AsynchronousBinaryDecoder {
    private static final String BINARY_FILE_PATH = "src/main/resources/websocket_messages_binary";
    private static final int CHUNK_SIZE = 14;
    private static final long THREAD_POOL_SIZE = 3;

    private final AsynchronousFileWriter writer;

    public AsynchronousBinaryDecoder(AsynchronousFileWriter writer) {
        this.writer = writer;
    }

    @PostConstruct
    public void decode() {
        long start = System.currentTimeMillis();
        Path path = Paths.get(BINARY_FILE_PATH);

        try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ)) {
            ExecutorService executorService = Executors.newFixedThreadPool((int) THREAD_POOL_SIZE);
            List<CompletableFuture<List<ModeSDownlinkMsg>>> futures = new ArrayList<>();

            long fileSize = fileChannel.size();
            long threadPortionSize = fileSize / THREAD_POOL_SIZE;

            long startPosition;
            long endPosition;

            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                startPosition = i * threadPortionSize;
                endPosition = (i == THREAD_POOL_SIZE - 1) ? fileSize : (i + 1) * threadPortionSize;

                CompletableFuture<List<ModeSDownlinkMsg>> future =
                        readAsync(fileChannel, startPosition, endPosition - startPosition);
                futures.add(future);
            }

            List<ModeSDownlinkMsg> data =
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(ignore -> futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(List::stream)
                            .collect(Collectors.toList()))
                    .join();

            executorService.shutdown();

            Map<String, List<ModeSDownlinkMsg>> map = data.stream()
                    .collect(Collectors.groupingBy(msg -> msg.getAddress().getHexAddress()));

            List<String> dataForWriting = new ArrayList<>();
            for (Map.Entry<String, List<ModeSDownlinkMsg>> entry : map.entrySet()) {
                Optional<IdentificationMsg> identityOptional = entry.getValue().stream()
                        .filter(msg -> msg.getType().equals(ModeSDownlinkMsg.subtype.ADSB_IDENTIFICATION))
                        .map(msg -> (IdentificationMsg) msg)
                        .findFirst();
                String airSpeeds = entry.getValue().stream()
                        .filter(msg -> msg.getType().equals(ModeSDownlinkMsg.subtype.ADSB_AIRSPEED))
                        .map(msg -> (AirspeedHeadingMsg) msg)
                        .map(AirspeedHeadingMsg::getAirspeed)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                List<ModeSDownlinkMsg> surface = entry.getValue().stream().filter(msg -> switch (msg.getType()) {
                    case ADSB_SURFACE_POSITION_V0,
                            ADSB_SURFACE_POSITION_V1,
                            ADSB_SURFACE_POSITION_V2 -> true;
                    default -> false;
                }).toList();
                String groundSpeeds = surface.stream().map(msg -> (SurfacePositionV0Msg) msg)
                        .map(SurfacePositionV0Msg::getGroundSpeed)
                        .distinct()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                String chunk = "[ICAO]: " + entry.getKey() + System.lineSeparator() +
                        "[CallSign]: " + identityOptional.map(identificationMsg ->
                        new String(identificationMsg.getIdentity())).orElse("NO INFO") +
                        System.lineSeparator() +
                        "[Ground speed]: {" + (groundSpeeds.isEmpty() ? "NO INFO" : groundSpeeds)
                        + "}" + System.lineSeparator() +
                        "[Air speed]: {" + (airSpeeds.isEmpty() ? "NO INFO" : airSpeeds) + "}" +
                        System.lineSeparator() +
                        "----------------------------------------------------------";
                dataForWriting.add(chunk);
            }
            writer.write(dataForWriting);
            System.out.println("Overall duration: " + (System.currentTimeMillis() - start) + " mls");
            System.out.println("Messages quantity: " + data.size());
            System.out.println("Aircrafts quantity: " + map.keySet().size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<List<ModeSDownlinkMsg>> readAsync(AsynchronousFileChannel fileChannel,
                                                                       long position, long size) {
        StatefulModeSDecoder modeSDecoder = new StatefulModeSDecoder();
        CompletableFuture<List<ModeSDownlinkMsg>> future = new CompletableFuture<>();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) size);

        fileChannel.read(byteBuffer, position, byteBuffer, new CompletionHandler<>() {

            @Override
            public void completed(Integer bytesRead, ByteBuffer attachment) {
                if (bytesRead > 0) {
                    attachment.flip();
                    byte[] chunkData = new byte[14];
                    List<ModeSDownlinkMsg> result = new ArrayList<>();
                    for (int i = 0; i < bytesRead; i += CHUNK_SIZE) {
                        attachment.get(chunkData);
                        ModeSDownlinkMsg message;
                        try {
                            message = modeSDecoder.decode(DatatypeConverter.printHexBinary(chunkData), 1000);
                        } catch (BadFormatException | UnspecifiedFormatError e) {
                            throw new RuntimeException(e);
                        }
                        result.add(message);
                    }
                    future.complete(result);
                } else {
                    future.complete(new ArrayList<>());
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }
}
