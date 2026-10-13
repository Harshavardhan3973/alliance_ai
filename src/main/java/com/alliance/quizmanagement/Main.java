package com.alliance.quizmanagement;

import com.alliance.quizmanagement.analytics.ConceptAnalyticsService;
import com.alliance.quizmanagement.repository.*;
import com.alliance.quizmanagement.service.*;
import com.alliance.quizmanagement.ui.MainFrame;
import javax.swing.*;
import java.io.File;

/**
 * Main application launcher for the Alliance University Online Quiz Management System.
 * Bootstraps repositories, services, and launches the Swing desktop user interface on EDT.
 */
public class Main {
    public static void main(String[] args) {
        if (java.awt.GraphicsEnvironment.isHeadless() || (args.length > 0 && "--cli".equalsIgnoreCase(args[0]))) {
            System.out.println("=== Alliance University Online Quiz Management System ===");
            try {
                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                FileStorageService storageService = new FileStorageService(dataDir);
                UserRepository userRepo = new UserRepository(storageService);
                QuestionRepository questionRepo = new QuestionRepository(storageService);
                QuizRepository quizRepo = new QuizRepository(storageService);
                AttemptRepository attemptRepo = new AttemptRepository(storageService);

                System.out.println("Data repository initialized at: " + dataDir.getAbsolutePath());
                System.out.println("Loaded Users: " + userRepo.findAll().size());
                System.out.println("Loaded Questions: " + questionRepo.findAll().size());
                System.out.println("Loaded Quizzes: " + quizRepo.findAll().size());
                System.out.println("Loaded Attempts: " + attemptRepo.findAll().size());
                System.out.println("System readiness verified successfully.");
                return;
            } catch (Exception e) {
                System.err.println("Headless verification failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Configure Swing look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                FileStorageService storageService = new FileStorageService(dataDir);
                UserRepository userRepo = new UserRepository(storageService);
                QuestionRepository questionRepo = new QuestionRepository(storageService);
                QuizRepository quizRepo = new QuizRepository(storageService);
                AttemptRepository attemptRepo = new AttemptRepository(storageService);

                AuthenticationService authService = new AuthenticationService(userRepo);
                QuestionService questionService = new QuestionService(questionRepo);
                QuizService quizService = new QuizService(quizRepo, questionRepo);
                ResultService resultService = new ResultService(attemptRepo);
                ConceptAnalyticsService analyticsService = new ConceptAnalyticsService();

                MainFrame frame = new MainFrame(
                        authService,
                        questionService,
                        quizService,
                        resultService,
                        analyticsService
                );
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize application: " + e.getMessage(),
                        "Initialization Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
