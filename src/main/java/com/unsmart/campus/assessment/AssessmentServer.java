package com.unsmart.campus.assessment;

import com.unsmart.campus.jmdns.ServiceRegistration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class AssessmentServer extends AssessmentServiceGrpc.AssessmentServiceImplBase {
    private static final Logger logger = Logger.getLogger(AssessmentServer.class.getName());
    private static final int PORT = 50053;
    private Server server;
    private final ConcurrentMap<String, SetQuizRequest> activeQuizzes = new ConcurrentHashMap<>();

    @Override
    public void setQuiz(SetQuizRequest request, StreamObserver<SetQuizResponse> responseObserver) {
        activeQuizzes.put(request.getQuizId(), request);
        responseObserver.onNext(SetQuizResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Quiz set successfully")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<StudentAnswer> getQuizResults(StreamObserver<AssessmentResult> responseObserver) {
        return new StreamObserver<StudentAnswer>() {
            @Override
            public void onNext(StudentAnswer answer) {
                SetQuizRequest quiz = activeQuizzes.get(answer.getQuizId());
                if (quiz != null) {
                    int score = 0;
                    for (QuizQuestion q : quiz.getQuestionsList()) {
                        if (q.getQuestionId().equals(answer.getQuestionId()) && 
                            q.getCorrectAnswer().equals(answer.getSubmittedAnswer())) {
                            score++;
                        }
                    }
                    responseObserver.onNext(AssessmentResult.newBuilder()
                            .setStudentId(answer.getStudentId())
                            .setQuizId(answer.getQuizId())
                            .setScore(score)
                            .setTotalQuestions(quiz.getQuestionsCount())
                            .build());
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error processing answers: " + t.getMessage());
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
        ServiceRegistration.registerService("AssessmentService", "_grpc._tcp.local.", PORT, "Assessment service");
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
        AssessmentServer server = new AssessmentServer();
        server.start();
        server.blockUntilShutdown();
    }
}