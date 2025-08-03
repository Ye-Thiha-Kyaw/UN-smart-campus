package com.unsmart.campus.attendance;

import com.unsmart.campus.jmdns.ServiceRegistration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AttendanceServer extends AttendanceServiceGrpc.AttendanceServiceImplBase {
    private static final Logger logger = Logger.getLogger(AttendanceServer.class.getName());
    private static final int PORT = 50051;
    private Server server;
    private final Set<Student> attendedStudents = Collections.synchronizedSet(new HashSet<>());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void checkInStudent(CheckInRequest request, StreamObserver<CheckInResponse> responseObserver) {
        Student student = request.getStudent();
        attendedStudents.add(student);
        
        CheckInResponse response = CheckInResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Student " + student.getStudentName() + " checked in successfully")
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        
        logger.info("Student checked in: " + student.getStudentName());
    }

//    @Override
//    public void streamAttendanceRecords(RollCallRequest request, StreamObserver<AttendanceRecord> responseObserver) {
//        scheduler.scheduleAtFixedRate(() -> {
//            try {
//                if (attendedStudents.isEmpty()) {
//                    logger.info("No students have checked in yet");
//                    return;
//                }
//                
//                for (Student student : attendedStudents) {
//                    AttendanceRecord record = AttendanceRecord.newBuilder()
//                        .setStudentId(student.getStudentId())
//                        .setStudentName(student.getStudentName())
//                        .setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME))
//                        .build();
//                    responseObserver.onNext(record);
//                }
//            } catch (Exception e) {
//                logger.warning("Error streaming records: " + e.getMessage());
//                scheduler.shutdown();
//                responseObserver.onError(e);
//            }
//        }, 0, 5, TimeUnit.SECONDS);
//    }
    @Override
public void streamAttendanceRecords(RollCallRequest request, 
    StreamObserver<AttendanceRecord> responseObserver) {
    
    // Send only new check-ins since last stream
    Set<Student> alreadySent = new HashSet<>();
    
    scheduler.scheduleAtFixedRate(() -> {
        try {
            synchronized (attendedStudents) {
                for (Student student : attendedStudents) {
                    if (!alreadySent.contains(student)) {
                        AttendanceRecord record = AttendanceRecord.newBuilder()
                            .setStudentId(student.getStudentId())
                            .setStudentName(student.getStudentName())
                            .setTimestamp(LocalDateTime.now()
                                .format(DateTimeFormatter.ISO_LOCAL_TIME))
                            .build();
                        responseObserver.onNext(record);
                        alreadySent.add(student);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Error streaming records: " + e.getMessage());
            scheduler.shutdown();
        }
    }, 0, 5, TimeUnit.SECONDS);
}

    public void start() throws IOException {
        server = ServerBuilder.forPort(PORT)
            .addService(this)
            .build()
            .start();
        
        ServiceRegistration.registerService("AttendanceService", "_grpc._tcp.local.", PORT, 
            "Attendance service for SDG 4: Quality Education");
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        logger.info("Attendance Server started, listening on port " + PORT);
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        logger.info("Attendance Server stopped");
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        AttendanceServer server = new AttendanceServer();
        server.start();
        server.blockUntilShutdown();
    }
}