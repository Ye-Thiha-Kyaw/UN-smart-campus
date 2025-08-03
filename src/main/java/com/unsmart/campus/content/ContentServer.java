package com.unsmart.campus.content;

import com.unsmart.campus.jmdns.ServiceRegistration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class ContentServer extends ContentDeliveryServiceGrpc.ContentDeliveryServiceImplBase {
    private static final Logger logger = Logger.getLogger(ContentServer.class.getName());
    private static final int PORT = 50052;
    private Server server;
    private final ConcurrentMap<String, String> documentStore = new ConcurrentHashMap<>();

    @Override
    public StreamObserver<ContentChunk> uploadPresentation(StreamObserver<UploadContentResponse> responseObserver) {
        return new StreamObserver<ContentChunk>() {
            private String fileName;
            private StringBuilder content = new StringBuilder();

            @Override
            public void onNext(ContentChunk chunk) {
                fileName = chunk.getFileName();
                content.append(chunk.getData().toStringUtf8());
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Upload failed: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                documentStore.put(fileName, content.toString());
                responseObserver.onNext(UploadContentResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("File uploaded successfully")
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<DocumentEdit> collaborateOnDocument(StreamObserver<DocumentEdit> responseObserver) {
        return new StreamObserver<DocumentEdit>() {
            @Override
            public void onNext(DocumentEdit edit) {
                responseObserver.onNext(edit);
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Collaboration error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(PORT)
                .addService(this)
                .build()
                .start();
        ServiceRegistration.registerService("ContentService", "_grpc._tcp.local.", PORT, "Content service");
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ContentServer server = new ContentServer();
        server.start();
        server.blockUntilShutdown();
    }
}