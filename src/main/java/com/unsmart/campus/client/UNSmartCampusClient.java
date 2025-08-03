package com.unsmart.campus.client;

import com.google.protobuf.ByteString;
import com.unsmart.campus.jmdns.ServiceDiscovery;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Import generated gRPC classes
import com.unsmart.campus.attendance.*;
import com.unsmart.campus.content.*;
import com.unsmart.campus.assessment.*;

public class UNSmartCampusClient extends JFrame {
    private final JTextArea logArea;
    private final ServiceDiscovery serviceDiscovery;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    // Input fields
    private JTextField studentIdField;
    private JTextField studentNameField;
    private JTextField classIdField;
    private JTextField quizIdField;
    
    // Status labels
    private JLabel attendanceStatusLabel;
    private JLabel contentStatusLabel;
    private JLabel assessmentStatusLabel;

    public UNSmartCampusClient() {
        setTitle("UNSmart-Campus Client");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // SDG 4 Label
        JLabel sdgLabel = new JLabel("Supporting UN SDG 4: Quality Education", SwingConstants.CENTER);
        sdgLabel.setFont(new Font("Arial", Font.BOLD, 16));
        sdgLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(sdgLabel, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Attendance buttons
        JButton checkInButton = new JButton("Check In Student");
        checkInButton.setToolTipText("Register a student's attendance");
        JButton rollCallButton = new JButton("Start Roll Call Stream");
        rollCallButton.setToolTipText("Stream live attendance records every 5 seconds");

        // Content buttons
        JButton uploadContentButton = new JButton("Upload Presentation");
        uploadContentButton.setToolTipText("Upload a presentation in chunks");
        JButton collaborateButton = new JButton("Start Collaboration");
        collaborateButton.setToolTipText("Collaborate on a document in real-time");

        // Assessment buttons
        JButton setQuizButton = new JButton("Set Quiz");
        setQuizButton.setToolTipText("Create a quiz with questions");
        JButton getResultsButton = new JButton("Get Quiz Results");
        getResultsButton.setToolTipText("Stream quiz results for students");

        // Add buttons to panel
        buttonPanel.add(checkInButton);
        buttonPanel.add(rollCallButton);
        buttonPanel.add(uploadContentButton);
        buttonPanel.add(collaborateButton);
        buttonPanel.add(setQuizButton);
        buttonPanel.add(getResultsButton);

        add(buttonPanel, BorderLayout.EAST);

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        
        inputPanel.add(new JLabel("Class ID:"));
        classIdField = new JTextField("CS4001");
        inputPanel.add(classIdField);
        
        inputPanel.add(new JLabel("Student ID:"));
        studentIdField = new JTextField("S12345");
        inputPanel.add(studentIdField);
        
        inputPanel.add(new JLabel("Student Name:"));
        studentNameField = new JTextField("John Doe");
        inputPanel.add(studentNameField);
        
        inputPanel.add(new JLabel("Quiz ID:"));
        quizIdField = new JTextField("QUIZ1");
        inputPanel.add(quizIdField);
        
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        inputPanel.add(clearLogButton);

        add(inputPanel, BorderLayout.WEST);

        // Status panel
        JPanel statusPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        attendanceStatusLabel = new JLabel("Attendance: ❌", SwingConstants.CENTER);
        contentStatusLabel = new JLabel("Content: ❌", SwingConstants.CENTER);
        assessmentStatusLabel = new JLabel("Assessment: ❌", SwingConstants.CENTER);
        
        statusPanel.add(attendanceStatusLabel);
        statusPanel.add(contentStatusLabel);
        statusPanel.add(assessmentStatusLabel);
        
        add(statusPanel, BorderLayout.SOUTH);

        // Initialize service discovery
        try {
            serviceDiscovery = new ServiceDiscovery(message -> {
                appendLog(message);
                if (message.contains("AttendanceService")) {
                    attendanceStatusLabel.setText("Attendance: ✅");
                } else if (message.contains("ContentService")) {
                    contentStatusLabel.setText("Content: ✅");
                } else if (message.contains("AssessmentService")) {
                    assessmentStatusLabel.setText("Assessment: ✅");
                }
            });
            serviceDiscovery.startDiscovery();
        } catch (IOException e) {
            appendLog("Failed to start service discovery: " + e.getMessage());
            throw new RuntimeException("Failed to initialize service discovery", e);
        }

        // Button actions
        checkInButton.addActionListener(e -> handleCheckIn());
        rollCallButton.addActionListener(e -> handleRollCall());
        uploadContentButton.addActionListener(e -> handleUploadContent());
        collaborateButton.addActionListener(e -> handleCollaborate());
        setQuizButton.addActionListener(e -> handleSetQuiz());
        getResultsButton.addActionListener(e -> handleGetResults());
    }

    private ManagedChannel getChannel(String serviceName) {
        String serviceAddress = serviceDiscovery.getServiceAddress(serviceName);
        if (serviceAddress == null) {
            appendLog("Error: " + serviceName + " not found. Please ensure service is running.");
            return null;
        }
        String[] parts = serviceAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    private void handleCheckIn() {
        String studentId = studentIdField.getText();
        String studentName = studentNameField.getText();
        String classId = classIdField.getText();

        if (studentId.isEmpty() || studentName.isEmpty() || classId.isEmpty()) {
            appendLog("Error: All fields must be filled!");
            return;
        }

        ManagedChannel channel = getChannel("AttendanceService");
        if (channel == null) return;
        
        try {
            AttendanceServiceGrpc.AttendanceServiceBlockingStub stub = 
                AttendanceServiceGrpc.newBlockingStub(channel);
            
            CheckInRequest request = CheckInRequest.newBuilder()
                .setClassId(classId)
                .setStudent(Student.newBuilder()
                    .setStudentId(studentId)
                    .setStudentName(studentName)
                    .build())
                .build();
            
            CheckInResponse response = stub.checkInStudent(request);
            appendLog("Check-in response: " + response.getMessage());
        } catch (StatusRuntimeException e) {
            appendLog("Error: " + e.getStatus().getDescription());
        } finally {
            channel.shutdown();
        }
    }

    private void handleRollCall() {
        String classId = classIdField.getText();
        if (classId.isEmpty()) {
            appendLog("Error: Class ID must be specified!");
            return;
        }

        ManagedChannel channel = getChannel("AttendanceService");
        if (channel == null) return;

        try {
            AttendanceServiceGrpc.AttendanceServiceStub stub = 
                AttendanceServiceGrpc.newStub(channel);
            
            RollCallRequest request = RollCallRequest.newBuilder()
                .setClassId(classId)
                .build();
            
            stub.streamAttendanceRecords(request, new StreamObserver<AttendanceRecord>() {
                @Override
                public void onNext(AttendanceRecord record) {
                    appendLog("Live Roll Call: " + record.getStudentName() + 
                        " checked in at " + record.getTimestamp());
                }

                @Override
                public void onError(Throwable t) {
                    appendLog("Roll Call error: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    appendLog("Roll Call stream ended");
                }
            });
        } catch (Exception e) {
            appendLog("Error starting roll call: " + e.getMessage());
            channel.shutdown();
        }
    }

    private void handleUploadContent() {
        ManagedChannel channel = getChannel("ContentService");
        if (channel == null) return;

        try {
            ContentDeliveryServiceGrpc.ContentDeliveryServiceStub stub = 
                ContentDeliveryServiceGrpc.newStub(channel);
            
            StreamObserver<UploadContentResponse> responseObserver = 
                new StreamObserver<UploadContentResponse>() {
                    @Override
                    public void onNext(UploadContentResponse response) {
                        appendLog("Upload response: " + response.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        appendLog("Upload failed: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        appendLog("Upload completed");
                    }
                };
            
            StreamObserver<ContentChunk> requestObserver = 
                stub.uploadPresentation(responseObserver);
            
            // Simulate sending a file in chunks
            requestObserver.onNext(ContentChunk.newBuilder()
                .setFileName("lecture1.ppt")
                .setData(ByteString.copyFromUtf8("First chunk of data"))
                .build());
            
            requestObserver.onNext(ContentChunk.newBuilder()
                .setFileName("lecture1.ppt")
                .setData(ByteString.copyFromUtf8("Second chunk of data"))
                .build());
            
            requestObserver.onCompleted();
        } catch (Exception e) {
            appendLog("Error uploading content: " + e.getMessage());
            channel.shutdown();
        }
    }

    private void handleCollaborate() {
        ManagedChannel channel = getChannel("ContentService");
        if (channel == null) return;

        try {
            ContentDeliveryServiceGrpc.ContentDeliveryServiceStub stub = 
                ContentDeliveryServiceGrpc.newStub(channel);
            
            StreamObserver<DocumentEdit> responseObserver = 
                new StreamObserver<DocumentEdit>() {
                    @Override
                    public void onNext(DocumentEdit edit) {
                        appendLog("Collaboration update: " + edit.getUserId() + 
                            " - " + edit.getEditAction() + ": " + edit.getPayload());
                    }

                    @Override
                    public void onError(Throwable t) {
                        appendLog("Collaboration error: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        appendLog("Collaboration ended");
                    }
                };
            
            StreamObserver<DocumentEdit> requestObserver = 
                stub.collaborateOnDocument(responseObserver);
            
            // Simulate some edits
            requestObserver.onNext(DocumentEdit.newBuilder()
                .setDocumentId("doc1")
                .setUserId("teacher1")
                .setEditAction("insert")
                .setPayload("Hello class!")
                .build());
            
            requestObserver.onNext(DocumentEdit.newBuilder()
                .setDocumentId("doc1")
                .setUserId("student1")
                .setEditAction("insert")
                .setPayload("Great lesson!")
                .build());
            
            requestObserver.onCompleted();
        } catch (Exception e) {
            appendLog("Error collaborating: " + e.getMessage());
            channel.shutdown();
        }
    }

    private void handleSetQuiz() {
        String quizId = quizIdField.getText();
        String classId = classIdField.getText();

        if (quizId.isEmpty() || classId.isEmpty()) {
            appendLog("Error: Quiz ID and Class ID must be specified!");
            return;
        }

        ManagedChannel channel = getChannel("AssessmentService");
        if (channel == null) return;

        try {
            AssessmentServiceGrpc.AssessmentServiceBlockingStub stub = 
                AssessmentServiceGrpc.newBlockingStub(channel);
            
            SetQuizRequest request = SetQuizRequest.newBuilder()
                .setQuizId(quizId)
                .setClassId(classId)
                .addQuestions(QuizQuestion.newBuilder()
                    .setQuestionId("Q1")
                    .setQuestionText("What is 2+2?")
                    .setCorrectAnswer("4"))
                .addQuestions(QuizQuestion.newBuilder()
                    .setQuestionId("Q2")
                    .setQuestionText("Capital of France?")
                    .setCorrectAnswer("Paris"))
                .build();
            
            SetQuizResponse response = stub.setQuiz(request);
            appendLog("Set Quiz response: " + response.getMessage());
        } catch (StatusRuntimeException e) {
            appendLog("Error: " + e.getStatus().getDescription());
        } finally {
            channel.shutdown();
        }
    }

    private void handleGetResults() {
        ManagedChannel channel = getChannel("AssessmentService");
        if (channel == null) return;

        try {
            AssessmentServiceGrpc.AssessmentServiceStub stub = 
                AssessmentServiceGrpc.newStub(channel);
            
            StreamObserver<AssessmentResult> responseObserver = 
                new StreamObserver<AssessmentResult>() {
                    @Override
                    public void onNext(AssessmentResult result) {
                        appendLog("Quiz Result: Student " + result.getStudentId() + 
                            " scored " + result.getScore() + "/" + result.getTotalQuestions());
                    }

                    @Override
                    public void onError(Throwable t) {
                        appendLog("Results error: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        appendLog("Results stream completed");
                    }
                };
            
            StreamObserver<StudentAnswer> requestObserver = 
                stub.getQuizResults(responseObserver);
            
            // Simulate student answers
            requestObserver.onNext(StudentAnswer.newBuilder()
                .setStudentId("S12345")
                .setQuizId("QUIZ1")
                .setQuestionId("Q1")
                .setSubmittedAnswer("4")
                .build());
            
            requestObserver.onNext(StudentAnswer.newBuilder()
                .setStudentId("S12345")
                .setQuizId("QUIZ1")
                .setQuestionId("Q2")
                .setSubmittedAnswer("Paris")
                .build());
            
            requestObserver.onNext(StudentAnswer.newBuilder()
                .setStudentId("S67890")
                .setQuizId("QUIZ1")
                .setQuestionId("Q1")
                .setSubmittedAnswer("5")
                .build());
            
            requestObserver.onCompleted();
        } catch (Exception e) {
            appendLog("Error getting results: " + e.getMessage());
            channel.shutdown();
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UNSmartCampusClient client = new UNSmartCampusClient();
            client.setVisible(true);
        });
    }
}