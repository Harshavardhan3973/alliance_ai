package com.alliance.quizmanagement.ui;

import com.alliance.quizmanagement.analytics.ConceptAnalyticsService;
import com.alliance.quizmanagement.analytics.ConceptPerformance;
import com.alliance.quizmanagement.exception.*;
import com.alliance.quizmanagement.model.*;
import com.alliance.quizmanagement.repository.*;
import com.alliance.quizmanagement.service.*;
import com.alliance.quizmanagement.util.QuizTimer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

/**
 * Main Java Swing Desktop Application Window for Alliance University Online Quiz Management System.
 * Houses all 20 screens with card layout navigation, synchronized countdown timer,
 * question-based revision inspector, and concept-wise analytics.
 */
public class MainFrame extends JFrame {

    // Services
    private final AuthenticationService authService;
    private final QuestionService questionService;
    private final QuizService quizService;
    private final ResultService resultService;
    private final ConceptAnalyticsService analyticsService;

    // Navigation Card Layout
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // Active Quiz Session State
    private Quiz activeQuiz;
    private List<Question> activeQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private final Map<Integer, Integer> activeStudentAnswers = new HashMap<>();
    private final Set<Integer> flaggedQuestions = new HashSet<>();
    private QuizTimer activeTimer;
    private String quizStartTimeStr;
    private boolean isSubmitted = false;

    // Latest Result and Revision State
    private QuizAttempt lastAttempt;

    // UI Header & Status Bar
    private JLabel statusLabel;
    private JLabel currentUserLabel;

    // Question Bank & Add/Edit Form Components
    private DefaultTableModel questionBankTableModel;
    private JTable questionBankTable;
    private String editingQuestionId = null;
    private JLabel addEditQuestionTitleLabel;
    private JComboBox<String> cmbConcept;
    private JComboBox<String> cmbCO;
    private JComboBox<Difficulty> cmbDifficulty;
    private JTextArea txtQuestion;
    private JTextField opt1;
    private JTextField opt2;
    private JTextField opt3;
    private JTextField opt4;
    private JComboBox<String> cmbCorrect;
    private JTextField txtExp;

    // Student Dashboard Components
    private DefaultTableModel studentQuizzesTableModel;
    private JTable studentQuizzesTable;

    // Profile Screen Components
    private JPanel profileCard;

    public MainFrame(
            AuthenticationService authService,
            QuestionService questionService,
            QuizService quizService,
            ResultService resultService,
            ConceptAnalyticsService analyticsService) {

        this.authService = authService;
        this.questionService = questionService;
        this.quizService = quizService;
        this.resultService = resultService;
        this.analyticsService = analyticsService;

        initUI();
    }

    private void initUI() {
        setTitle("Alliance University — Online Quiz Management System (ASAC CS204)");
        setSize(1100, 750);
        setMinimumSize(new Dimension(850, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Layout
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(createTopMenuBar(), BorderLayout.NORTH);
        rootPanel.add(cardsPanel, BorderLayout.CENTER);
        rootPanel.add(createStatusBar(), BorderLayout.SOUTH);

        // Register All 20 Screens
        buildScreens();

        setContentPane(rootPanel);
        showScreen("WELCOME");
    }

    private JMenuBar createTopMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu navMenu = new JMenu("Navigation");
        JMenuItem itemHome = new JMenuItem("Home");
        itemHome.addActionListener(e -> showScreen("WELCOME"));
        JMenuItem itemQuizzes = new JMenuItem("Quizzes");
        itemQuizzes.addActionListener(e -> {
            User u = authService.getCurrentUser();
            if (u == null) {
                JOptionPane.showMessageDialog(this, "Please sign in to view scheduled quizzes.", "Sign In Required", JOptionPane.INFORMATION_MESSAGE);
                showScreen("LOGIN");
            } else if (u.getRole() == Role.INSTRUCTOR) {
                showScreen("INSTRUCTOR_DASHBOARD");
            } else {
                showScreen("STUDENT_DASHBOARD");
            }
        });
        JMenuItem itemHistory = new JMenuItem("History");
        itemHistory.addActionListener(e -> {
            if (!authService.isLoggedIn()) {
                JOptionPane.showMessageDialog(this, "Please sign in to view exam history.", "Sign In Required", JOptionPane.INFORMATION_MESSAGE);
                showScreen("LOGIN");
            } else {
                showScreen("HISTORY");
            }
        });
        JMenuItem itemAnalytics = new JMenuItem("Concept Analytics");
        itemAnalytics.addActionListener(e -> {
            if (!authService.isLoggedIn()) {
                JOptionPane.showMessageDialog(this, "Please sign in to view concept analytics.", "Sign In Required", JOptionPane.INFORMATION_MESSAGE);
                showScreen("LOGIN");
            } else {
                showScreen("CONCEPT_ANALYTICS");
            }
        });
        JMenuItem itemLogin = new JMenuItem("Sign In");
        itemLogin.addActionListener(e -> showScreen("LOGIN"));
        JMenuItem itemLogout = new JMenuItem("Logout");
        itemLogout.addActionListener(e -> handleLogout());

        navMenu.add(itemHome);
        navMenu.add(itemQuizzes);
        navMenu.add(itemHistory);
        navMenu.add(itemAnalytics);
        navMenu.addSeparator();
        navMenu.add(itemLogin);
        navMenu.add(itemLogout);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem itemAbout = new JMenuItem("About");
        itemAbout.addActionListener(e -> showScreen("ABOUT"));
        helpMenu.add(itemAbout);

        menuBar.add(navMenu);
        menuBar.add(helpMenu);

        menuBar.add(Box.createHorizontalGlue());
        currentUserLabel = new JLabel("");
        currentUserLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
        menuBar.add(currentUserLabel);

        return menuBar;
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(4, 10, 4, 10));
        statusPanel.setBackground(new Color(245, 247, 250));

        statusLabel = new JLabel("Alliance School of Advanced Computing");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        JLabel copyrightLabel = new JLabel("© " + currentYear + " Alliance University");
        copyrightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(copyrightLabel, BorderLayout.EAST);
        return statusPanel;
    }

    private void buildScreens() {
        cardsPanel.add(createWelcomeScreen(), "WELCOME");
        cardsPanel.add(createLoginScreen(), "LOGIN");
        cardsPanel.add(createRegisterScreen(), "REGISTER");
        cardsPanel.add(createRecoveryScreen(), "RECOVERY");
        cardsPanel.add(createStudentDashboardScreen(), "STUDENT_DASHBOARD");
        cardsPanel.add(createInstructorDashboardScreen(), "INSTRUCTOR_DASHBOARD");
        cardsPanel.add(createQuestionBankScreen(), "QUESTION_BANK");
        cardsPanel.add(createAddEditQuestionScreen(), "ADD_EDIT_QUESTION");
        cardsPanel.add(createQuizCreateScreen(), "CREATE_QUIZ");
        cardsPanel.add(createQuizInstructionsScreen(), "QUIZ_INSTRUCTIONS");
        cardsPanel.add(createExamScreen(), "EXAM");
        cardsPanel.add(createResultsScreen(), "RESULTS");
        cardsPanel.add(createRevisionScreen(), "REVISION");
        cardsPanel.add(createHistoryScreen(), "HISTORY");
        cardsPanel.add(createConceptAnalyticsScreen(), "CONCEPT_ANALYTICS");
        cardsPanel.add(createProfileScreen(), "PROFILE");
        cardsPanel.add(createSettingsScreen(), "SETTINGS");
        cardsPanel.add(createAboutScreen(), "ABOUT");
    }

    public void showScreen(String screenName) {
        // Enforce role-based access control
        if ("INSTRUCTOR_DASHBOARD".equals(screenName) || "QUESTION_BANK".equals(screenName) ||
            "ADD_EDIT_QUESTION".equals(screenName) || "CREATE_QUIZ".equals(screenName) || "SETTINGS".equals(screenName)) {
            User u = authService.getCurrentUser();
            if (u == null || u.getRole() != Role.INSTRUCTOR) {
                JOptionPane.showMessageDialog(this, "Access Denied: Instructor privileges required.", "Authorization Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        if ("STUDENT_DASHBOARD".equals(screenName) || "EXAM".equals(screenName) || "QUIZ_INSTRUCTIONS".equals(screenName)) {
            User u = authService.getCurrentUser();
            if (u == null) {
                JOptionPane.showMessageDialog(this, "Please sign in to access this page.", "Sign In Required", JOptionPane.WARNING_MESSAGE);
                cardLayout.show(cardsPanel, "LOGIN");
                return;
            }
        }

        if ("RESULTS".equals(screenName)) {
            populateResultsCard();
        } else if ("REVISION".equals(screenName)) {
            populateRevisionScreen();
        } else if ("CONCEPT_ANALYTICS".equals(screenName)) {
            populateConceptAnalytics();
        } else if ("HISTORY".equals(screenName)) {
            populateHistoryTable();
        } else if ("QUESTION_BANK".equals(screenName)) {
            populateQuestionBankTable();
        } else if ("STUDENT_DASHBOARD".equals(screenName)) {
            populateStudentDashboardQuizzes();
        } else if ("PROFILE".equals(screenName)) {
            populateProfileScreen();
        }

        cardLayout.show(cardsPanel, screenName);
        updateUserStatus();
    }

    private void updateUserStatus() {
        User u = authService.getCurrentUser();
        if (u != null) {
            currentUserLabel.setText(u.getFullName() + " (" + u.getRole() + ") ");
        } else {
            currentUserLabel.setText("");
        }
    }

    // =========================================================================
    // SCREEN 1: WELCOME SCREEN
    // =========================================================================
    private JPanel createWelcomeScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(40, 60, 40, 60));

        JPanel header = new JPanel(new GridLayout(3, 1, 6, 6));
        header.setOpaque(false);
        JLabel title = new JLabel("ALLIANCE UNIVERSITY", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(new Color(30, 58, 138)); // Deep Navy

        JLabel subtitle = new JLabel("Alliance School of Advanced Computing", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitle.setForeground(new Color(71, 85, 105));

        JLabel project = new JLabel("Online Quiz Management System with Synchronized Timer & Question-Based Revision", SwingConstants.CENTER);
        project.setFont(new Font("SansSerif", Font.BOLD, 15));
        project.setForeground(new Color(15, 118, 110)); // Teal

        header.add(title);
        header.add(subtitle);
        header.add(project);

        JPanel center = new JPanel(new GridLayout(1, 2, 30, 30));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(30, 0, 30, 0));

        // Student Card
        JPanel studentCard = new JPanel(new BorderLayout(15, 15));
        studentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 2),
                new EmptyBorder(25, 25, 25, 25)
        ));
        studentCard.setBackground(Color.WHITE);
        JLabel sTitle = new JLabel("Student Portal", SwingConstants.CENTER);
        sTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        JTextArea sDesc = new JTextArea("Take timed multiple-choice examinations with real-time synchronized timer countdown, automatic submission on deadline, question-based revision, and concept-wise analytics.");
        sDesc.setWrapStyleWord(true);
        sDesc.setLineWrap(true);
        sDesc.setEditable(false);
        sDesc.setOpaque(false);
        sDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel sBtnBox = new JPanel(new GridLayout(1, 2, 10, 10));
        sBtnBox.setOpaque(false);
        JButton btnLoginStudent = new JButton("Student Login");
        btnLoginStudent.setBackground(new Color(37, 99, 235));
        btnLoginStudent.setForeground(Color.WHITE);
        btnLoginStudent.addActionListener(e -> showScreen("LOGIN"));

        JButton btnRegStudent = new JButton("Register Now");
        btnRegStudent.addActionListener(e -> showScreen("REGISTER"));
        sBtnBox.add(btnLoginStudent);
        sBtnBox.add(btnRegStudent);

        studentCard.add(sTitle, BorderLayout.NORTH);
        studentCard.add(sDesc, BorderLayout.CENTER);
        studentCard.add(sBtnBox, BorderLayout.SOUTH);

        // Instructor Card
        JPanel insCard = new JPanel(new BorderLayout(15, 15));
        insCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 2),
                new EmptyBorder(25, 25, 25, 25)
        ));
        insCard.setBackground(Color.WHITE);
        JLabel iTitle = new JLabel("Instructor Portal", SwingConstants.CENTER);
        iTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        JTextArea iDesc = new JTextArea("Manage multiple-choice question banks tagged with Java concepts (Inheritance, Polymorphism, Multithreading, Exception Handling), configure timed quizzes, and review class performance analytics.");
        iDesc.setWrapStyleWord(true);
        iDesc.setLineWrap(true);
        iDesc.setEditable(false);
        iDesc.setOpaque(false);
        iDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton btnLoginIns = new JButton("Instructor Login");
        btnLoginIns.setBackground(new Color(15, 118, 110));
        btnLoginIns.setForeground(Color.WHITE);
        btnLoginIns.addActionListener(e -> showScreen("LOGIN"));

        insCard.add(iTitle, BorderLayout.NORTH);
        insCard.add(iDesc, BorderLayout.CENTER);
        insCard.add(btnLoginIns, BorderLayout.SOUTH);

        center.add(studentCard);
        center.add(insCard);

        panel.add(header, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // SCREEN 2: LOGIN SCREEN
    // =========================================================================
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(241, 245, 249));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("Alliance Exam Portal Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(30, 58, 138));
        formCard.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        formCard.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField txtUsername = new JTextField(18);
        formCard.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formCard.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField txtPassword = new JPasswordField(18);
        formCard.add(txtPassword, gbc);

        gbc.gridx = 1; gbc.gridy++;
        JCheckBox chkShowPass = new JCheckBox("Show Password");
        chkShowPass.setOpaque(false);
        chkShowPass.addActionListener(e -> {
            if (chkShowPass.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });
        formCard.add(chkShowPass, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JButton btnLogin = new JButton("Sign In");
        btnLogin.setBackground(new Color(37, 99, 235));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.addActionListener(e -> {
            String u = txtUsername.getText().trim();
            String p = new String(txtPassword.getPassword());
            try {
                User user = authService.login(u, p);
                txtPassword.setText("");
                statusLabel.setText("Authenticated successfully as: " + user.getFullName());
                if (user.getRole() == Role.INSTRUCTOR) {
                    showScreen("INSTRUCTOR_DASHBOARD");
                } else {
                    showScreen("STUDENT_DASHBOARD");
                }
            } catch (AuthenticationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Authentication Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        formCard.add(btnLogin, gbc);

        gbc.gridy++;
        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER));
        links.setOpaque(false);
        JButton btnToRegister = new JButton("New Student Registration");
        btnToRegister.setBorderPainted(false);
        btnToRegister.setContentAreaFilled(false);
        btnToRegister.setForeground(new Color(37, 99, 235));
        btnToRegister.addActionListener(e -> showScreen("REGISTER"));

        JButton btnRecovery = new JButton("Account Recovery");
        btnRecovery.setBorderPainted(false);
        btnRecovery.setContentAreaFilled(false);
        btnRecovery.setForeground(new Color(100, 116, 139));
        btnRecovery.addActionListener(e -> showScreen("RECOVERY"));

        links.add(btnToRegister);
        links.add(new JLabel("|"));
        links.add(btnRecovery);
        formCard.add(links, gbc);

        panel.add(formCard);
        return panel;
    }

    // =========================================================================
    // SCREEN 3: STUDENT REGISTRATION
    // =========================================================================
    private JPanel createRegisterScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(241, 245, 249));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("Alliance University Student Registration", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(30, 58, 138));
        formCard.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        formCard.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        JTextField txtFullName = new JTextField(20);
        formCard.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formCard.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField txtUsername = new JTextField(20);
        formCard.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formCard.add(new JLabel("University Email:"), gbc);
        gbc.gridx = 1;
        JTextField txtEmail = new JTextField(20);
        formCard.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formCard.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> cmbDept = new JComboBox<>(new String[]{
                "School of Advanced Computing (CS)",
                "Information Science & Engineering",
                "Artificial Intelligence & ML",
                "Data Science & Analytics"
        });
        formCard.add(cmbDept, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formCard.add(new JLabel("Password (min 6 chars, letter+num):"), gbc);
        gbc.gridx = 1;
        JPasswordField txtPass = new JPasswordField(20);
        formCard.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formCard.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField txtConf = new JPasswordField(20);
        formCard.add(txtConf, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JButton btnSubmit = new JButton("Complete Registration");
        btnSubmit.setBackground(new Color(16, 185, 129));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnSubmit.addActionListener(e -> {
            try {
                authService.registerStudent(
                        txtUsername.getText(),
                        txtFullName.getText(),
                        txtEmail.getText(),
                        new String(txtPass.getPassword()),
                        new String(txtConf.getPassword()),
                        (String) cmbDept.getSelectedItem()
                );
                JOptionPane.showMessageDialog(this,
                        "Registration successful! You may now log in with your credentials.",
                        "Registration Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                showScreen("LOGIN");
            } catch (ValidationException | StorageException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        formCard.add(btnSubmit, gbc);

        gbc.gridy++;
        JButton btnBack = new JButton("Already registered? Back to Login");
        btnBack.setBorderPainted(false);
        btnBack.setContentAreaFilled(false);
        btnBack.setForeground(new Color(37, 99, 235));
        btnBack.addActionListener(e -> showScreen("LOGIN"));
        formCard.add(btnBack, gbc);

        panel.add(formCard);
        return panel;
    }

    // =========================================================================
    // SCREEN 4: LOCAL ACCOUNT RECOVERY
    // =========================================================================
    private JPanel createRecoveryScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(241, 245, 249));

        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(30, 30, 30, 30)
        ));

        JLabel title = new JLabel("Local Offline Account Recovery", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(30, 58, 138));

        JTextArea info = new JTextArea(
                "Because this system operates as a secure offline desktop application, account password resets " +
                "are administered locally by the course instructor / laboratory in-charge.\n\n" +
                "Default Demonstration Accounts:\n" +
                "• Student: harsha_v (Password: Password123)\n" +
                "• Student: monisha_r (Password: Password123)\n" +
                "• Student: thanushree_d (Password: Password123)\n" +
                "• Faculty: instructor_admin (Password: Password123)\n\n" +
                "Faculty advisors can reset student passwords via the Instructor Dashboard."
        );
        info.setWrapStyleWord(true);
        info.setLineWrap(true);
        info.setEditable(false);
        info.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton btnBack = new JButton("Return to Login");
        btnBack.addActionListener(e -> showScreen("LOGIN"));

        card.add(title, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        card.add(btnBack, BorderLayout.SOUTH);

        panel.add(card);
        return panel;
    }

    // =========================================================================
    // SCREEN 5: STUDENT DASHBOARD
    // =========================================================================
    private JPanel createStudentDashboardScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Student Examination Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(30, 58, 138));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBar.setOpaque(false);
        JButton btnAnalytics = new JButton("Concept Analytics");
        btnAnalytics.addActionListener(e -> showScreen("CONCEPT_ANALYTICS"));
        JButton btnHistory = new JButton("Past Attempts");
        btnHistory.addActionListener(e -> showScreen("HISTORY"));
        JButton btnProfile = new JButton("My Profile");
        btnProfile.addActionListener(e -> showScreen("PROFILE"));
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> handleLogout());

        btnBar.add(btnAnalytics);
        btnBar.add(btnHistory);
        btnBar.add(btnProfile);
        btnBar.add(btnLogout);

        header.add(title, BorderLayout.WEST);
        header.add(btnBar, BorderLayout.EAST);

        // Center: Available Quizzes List
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        JLabel lblList = new JLabel("Available Scheduled Examinations:");
        lblList.setFont(new Font("SansSerif", Font.BOLD, 15));

        String[] cols = {"Quiz ID", "Title", "Duration", "Pass %", "Course Outcomes", "Action"};
        studentQuizzesTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        studentQuizzesTable = new JTable(studentQuizzesTableModel);
        studentQuizzesTable.setRowHeight(32);
        populateStudentDashboardQuizzes();
        JScrollPane scroll = new JScrollPane(studentQuizzesTable);

        JPanel bottomBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBtn.setOpaque(false);
        JButton btnStart = new JButton("Begin Selected Quiz →");
        btnStart.setBackground(new Color(37, 99, 235));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnStart.addActionListener(e -> {
            int row = studentQuizzesTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an examination from the table to begin.");
                return;
            }
            String qId = (String) studentQuizzesTableModel.getValueAt(row, 0);
            Optional<Quiz> opt = quizService.getQuizById(qId);
            if (opt.isPresent()) {
                activeQuiz = opt.get();
                showScreen("QUIZ_INSTRUCTIONS");
            }
        });
        bottomBtn.add(btnStart);

        center.add(lblList, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        center.add(bottomBtn, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void populateStudentDashboardQuizzes() {
        if (studentQuizzesTableModel == null) return;
        studentQuizzesTableModel.setRowCount(0);
        for (Quiz q : quizService.getAllActiveQuizzes()) {
            studentQuizzesTableModel.addRow(new Object[]{
                    q.getId(),
                    q.getTitle(),
                    q.getDurationMinutes() + " mins",
                    q.getPassPercentage() + "%",
                    q.getCourseOutcome(),
                    "Select & Start"
            });
        }
    }

    // =========================================================================
    // SCREEN 6: INSTRUCTOR DASHBOARD
    // =========================================================================
    private JPanel createInstructorDashboardScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Instructor Administration Console");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(15, 118, 110));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBar.setOpaque(false);
        JButton btnSettings = new JButton("Data Backup / Settings");
        btnSettings.addActionListener(e -> showScreen("SETTINGS"));
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> handleLogout());
        btnBar.add(btnSettings);
        btnBar.add(btnLogout);

        header.add(title, BorderLayout.WEST);
        header.add(btnBar, BorderLayout.EAST);

        // Grid of Action Cards
        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setOpaque(false);

        // 1. Question Bank
        JPanel card1 = createDashboardActionCard(
                "Question Bank Management",
                "Create, modify, and curate multiple-choice questions tagged with Java concepts (Inheritance, Polymorphism, Interfaces, Exception Handling, Multithreading).",
                "Manage Questions",
                () -> showScreen("QUESTION_BANK")
        );

        // 2. Quiz Creator
        JPanel card2 = createDashboardActionCard(
                "Quiz Scheduling & Config",
                "Configure new examinations, select questions from the bank, specify time limits, and set passing thresholds.",
                "Create Quiz",
                () -> showScreen("CREATE_QUIZ")
        );

        // 3. Class Analytics
        JPanel card3 = createDashboardActionCard(
                "Concept Analytics & Weakness Audit",
                "Analyze aggregate student mistakes by Java concept using HashMaps to identify topics requiring faculty revision.",
                "View Concept Analytics",
                () -> showScreen("CONCEPT_ANALYTICS")
        );

        // 4. Student Examination History
        JPanel card4 = createDashboardActionCard(
                "Historical Exam Submissions",
                "Inspect student attempts, exact question snapshots, submitted answers, and timestamps.",
                "Inspect Attempts",
                () -> showScreen("HISTORY")
        );

        grid.add(card1);
        grid.add(card2);
        grid.add(card3);
        grid.add(card4);

        panel.add(header, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDashboardActionCard(String title, String desc, String btnText, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 2),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTitle.setForeground(new Color(30, 41, 59));

        JTextArea txtDesc = new JTextArea(desc);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setLineWrap(true);
        txtDesc.setEditable(false);
        txtDesc.setOpaque(false);
        txtDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton btn = new JButton(btnText);
        btn.setBackground(new Color(30, 58, 138));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.addActionListener(e -> action.run());

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(txtDesc, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================================
    // SCREEN 7: QUESTION BANK MANAGEMENT
    // =========================================================================
    private JPanel createQuestionBankScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Question Bank Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        JButton btnAdd = new JButton("+ Add New Question");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> {
            clearQuestionForm();
            showScreen("ADD_EDIT_QUESTION");
        });

        JButton btnBack = new JButton("Back to Dashboard");
        btnBack.addActionListener(e -> showScreen("INSTRUCTOR_DASHBOARD"));

        actions.add(btnAdd);
        actions.add(btnBack);

        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        String[] cols = {"ID", "Java Concept", "Course Outcome", "Difficulty", "Question Text Preview"};
        questionBankTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        questionBankTable = new JTable(questionBankTableModel);
        questionBankTable.setRowHeight(28);
        populateQuestionBankTable();
        JScrollPane scroll = new JScrollPane(questionBankTable);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        footer.setOpaque(false);

        JButton btnEdit = new JButton("Edit Selected Question");
        btnEdit.setBackground(new Color(30, 58, 138));
        btnEdit.setForeground(Color.WHITE);
        btnEdit.addActionListener(e -> {
            int row = questionBankTable.getSelectedRow();
            if (row >= 0) {
                String id = (String) questionBankTableModel.getValueAt(row, 0);
                Optional<Question> opt = questionService.getQuestionById(id);
                if (opt.isPresent()) {
                    loadQuestionForEdit(opt.get());
                    showScreen("ADD_EDIT_QUESTION");
                } else {
                    JOptionPane.showMessageDialog(this, "Question " + id + " not found in repository.", "Not Found", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a question from the table to edit.");
            }
        });

        JButton btnDelete = new JButton("Delete Selected Question");
        btnDelete.setForeground(Color.RED);
        btnDelete.addActionListener(e -> {
            int row = questionBankTable.getSelectedRow();
            if (row >= 0) {
                String id = (String) questionBankTableModel.getValueAt(row, 0);
                int conf = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete question " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.YES_OPTION) {
                    try {
                        questionService.deleteQuestion(id);
                        populateQuestionBankTable();
                        JOptionPane.showMessageDialog(this, "Question " + id + " deleted successfully.");
                    } catch (StorageException ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a question from the table to delete.");
            }
        });

        footer.add(btnEdit);
        footer.add(btnDelete);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private void populateQuestionBankTable() {
        if (questionBankTableModel == null) return;
        questionBankTableModel.setRowCount(0);
        for (Question q : questionService.getAllQuestions()) {
            String preview = q.getQuestionText().length() > 60 ? q.getQuestionText().substring(0, 60) + "..." : q.getQuestionText();
            questionBankTableModel.addRow(new Object[]{q.getId(), q.getConcept(), q.getCourseOutcome(), q.getDifficulty(), preview});
        }
    }

    // =========================================================================
    // SCREEN 8: ADD / EDIT QUESTION FORM
    // =========================================================================
    private JPanel createAddEditQuestionScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        addEditQuestionTitleLabel = new JLabel("Create / Edit Multiple Choice Question");
        addEditQuestionTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0; gbc.gridy = 0;

        form.add(new JLabel("Java Concept:"), gbc);
        gbc.gridx = 1;
        cmbConcept = new JComboBox<>(new String[]{
                "Inheritance", "Polymorphism", "Interfaces", "Exception Handling",
                "Multithreading", "Collections & HashMap", "Classes & Objects", "Packages & Encapsulation"
        });
        form.add(cmbConcept, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Course Outcome:"), gbc);
        gbc.gridx = 1;
        cmbCO = new JComboBox<>(new String[]{"CO-1", "CO-2", "CO-3"});
        form.add(cmbCO, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Difficulty:"), gbc);
        gbc.gridx = 1;
        cmbDifficulty = new JComboBox<>(Difficulty.values());
        cmbDifficulty.setSelectedItem(Difficulty.MEDIUM);
        form.add(cmbDifficulty, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Question Text:"), gbc);
        gbc.gridx = 1;
        txtQuestion = new JTextArea(3, 30);
        txtQuestion.setLineWrap(true);
        form.add(new JScrollPane(txtQuestion), gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Option 1 (A):"), gbc);
        gbc.gridx = 1;
        opt1 = new JTextField(30);
        form.add(opt1, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Option 2 (B):"), gbc);
        gbc.gridx = 1;
        opt2 = new JTextField(30);
        form.add(opt2, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Option 3 (C):"), gbc);
        gbc.gridx = 1;
        opt3 = new JTextField(30);
        form.add(opt3, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Option 4 (D):"), gbc);
        gbc.gridx = 1;
        opt4 = new JTextField(30);
        form.add(opt4, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Correct Option:"), gbc);
        gbc.gridx = 1;
        cmbCorrect = new JComboBox<>(new String[]{"Option 1 (A)", "Option 2 (B)", "Option 3 (C)", "Option 4 (D)"});
        form.add(cmbCorrect, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Explanation / Rationale:"), gbc);
        gbc.gridx = 1;
        txtExp = new JTextField(30);
        form.add(txtExp, gbc);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBox.setOpaque(false);
        JButton btnSave = new JButton("Save Question to Bank");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                List<String> options = Arrays.asList(opt1.getText().trim(), opt2.getText().trim(), opt3.getText().trim(), opt4.getText().trim());
                int correctIdx = cmbCorrect.getSelectedIndex();
                String qId = editingQuestionId;
                if (qId == null || qId.trim().isEmpty()) {
                    qId = "Q-" + (System.currentTimeMillis() % 10000);
                }
                Question q = new Question(
                        qId,
                        txtQuestion.getText().trim(),
                        options,
                        correctIdx,
                        (String) cmbConcept.getSelectedItem(),
                        (Difficulty) cmbDifficulty.getSelectedItem(),
                        (String) cmbCO.getSelectedItem(),
                        txtExp.getText().trim()
                );
                questionService.saveOrUpdateQuestion(q);
                JOptionPane.showMessageDialog(this, "Question " + q.getId() + " saved successfully to question bank!");
                clearQuestionForm();
                populateQuestionBankTable();
                showScreen("QUESTION_BANK");
            } catch (ValidationException | StorageException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> {
            clearQuestionForm();
            showScreen("QUESTION_BANK");
        });

        btnBox.add(btnCancel);
        btnBox.add(btnSave);

        panel.add(addEditQuestionTitleLabel, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btnBox, BorderLayout.SOUTH);
        return panel;
    }

    private void loadQuestionForEdit(Question q) {
        editingQuestionId = q.getId();
        if (addEditQuestionTitleLabel != null) {
            addEditQuestionTitleLabel.setText("Edit Multiple Choice Question (" + q.getId() + ")");
        }
        if (cmbConcept != null) cmbConcept.setSelectedItem(q.getConcept());
        if (cmbCO != null) cmbCO.setSelectedItem(q.getCourseOutcome());
        if (cmbDifficulty != null) cmbDifficulty.setSelectedItem(q.getDifficulty());
        if (txtQuestion != null) txtQuestion.setText(q.getQuestionText());
        List<String> opts = q.getOptions();
        if (opt1 != null) opt1.setText(opts.size() > 0 ? opts.get(0) : "");
        if (opt2 != null) opt2.setText(opts.size() > 1 ? opts.get(1) : "");
        if (opt3 != null) opt3.setText(opts.size() > 2 ? opts.get(2) : "");
        if (opt4 != null) opt4.setText(opts.size() > 3 ? opts.get(3) : "");
        int cIdx = q.getCorrectAnswerIndex();
        if (cmbCorrect != null && cIdx >= 0 && cIdx < 4) {
            cmbCorrect.setSelectedIndex(cIdx);
        }
        if (txtExp != null) txtExp.setText(q.getExplanation() != null ? q.getExplanation() : "");
    }

    private void clearQuestionForm() {
        editingQuestionId = null;
        if (addEditQuestionTitleLabel != null) {
            addEditQuestionTitleLabel.setText("Create Multiple Choice Question");
        }
        if (txtQuestion != null) txtQuestion.setText("");
        if (opt1 != null) opt1.setText("");
        if (opt2 != null) opt2.setText("");
        if (opt3 != null) opt3.setText("");
        if (opt4 != null) opt4.setText("");
        if (cmbCorrect != null) cmbCorrect.setSelectedIndex(0);
        if (txtExp != null) txtExp.setText("");
    }

    // =========================================================================
    // SCREEN 9: QUIZ CREATION SCREEN
    // =========================================================================
    private JPanel createQuizCreateScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Schedule New Examination");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        form.add(new JLabel("Exam Title:"));
        JTextField txtTitle = new JTextField("Java Multithreading & OOP Midterm Exam");
        form.add(txtTitle);

        form.add(new JLabel("Duration (Minutes):"));
        JSpinner spinDuration = new JSpinner(new SpinnerNumberModel(5, 1, 120, 1));
        form.add(spinDuration);

        form.add(new JLabel("Passing Percentage:"));
        JSpinner spinPass = new JSpinner(new SpinnerNumberModel(60, 40, 100, 5));
        form.add(spinPass);

        form.add(new JLabel("Course Outcomes:"));
        JTextField txtCO = new JTextField("CO-1, CO-2, CO-3");
        form.add(txtCO);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBox.setOpaque(false);
        JButton btnCreate = new JButton("Schedule Examination");
        btnCreate.setBackground(new Color(30, 58, 138));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.addActionListener(e -> {
            try {
                List<String> qIds = new ArrayList<>();
                for (Question q : questionService.getAllQuestions()) {
                    qIds.add(q.getId());
                }
                Quiz quiz = new Quiz(
                        null,
                        txtTitle.getText(),
                        "Timed academic examination with synchronized timer.",
                        (Integer) spinDuration.getValue(),
                        (Integer) spinPass.getValue(),
                        authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "instructor_admin",
                        qIds,
                        txtCO.getText(),
                        Arrays.asList(
                                "The exam has a synchronized timer running on a separate thread.",
                                "Answers are auto-submitted immediately upon timeout.",
                                "Exact question snapshots are preserved for revision."
                        )
                );
                quizService.createQuiz(quiz);
                JOptionPane.showMessageDialog(this, "Quiz created and scheduled successfully!");
                showScreen("INSTRUCTOR_DASHBOARD");
            } catch (ValidationException | StorageException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnBack = new JButton("Back");
        btnBack.addActionListener(e -> showScreen("INSTRUCTOR_DASHBOARD"));

        btnBox.add(btnBack);
        btnBox.add(btnCreate);

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btnBox, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // SCREEN 10: QUIZ INSTRUCTIONS SCREEN
    // =========================================================================
    private JPanel createQuizInstructionsScreen() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Examination Rules & Instructions", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(30, 58, 138));

        JTextArea area = new JTextArea(
                "Please read all instructions carefully before starting the examination:\n\n" +
                "1. Synchronized Timer:\n" +
                "   The examination timer is strictly synchronized on a dedicated background thread.\n" +
                "   A live countdown is displayed throughout your attempt.\n\n" +
                "2. Automatic Submission:\n" +
                "   If the countdown reaches 00:00 before you click 'Submit Exam', the system will\n" +
                "   AUTOMATICALLY submit and evaluate all answered questions.\n\n" +
                "3. Scoring Policy:\n" +
                "   • Correct Answer: +1 mark\n" +
                "   • Incorrect Answer: 0 marks\n" +
                "   • Unattempted: 0 marks\n\n" +
                "4. Question-Based Revision & Concept Analytics:\n" +
                "   Following submission, every exact question and answer choice will be preserved for your review,\n" +
                "   and concept-wise performance will identify your strengths and topics needing revision."
        );
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        area.setBorder(new EmptyBorder(20, 20, 20, 20));
        area.setBackground(Color.WHITE);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnBox.setOpaque(false);
        JButton btnCancel = new JButton("Return to Dashboard");
        btnCancel.addActionListener(e -> showScreen("STUDENT_DASHBOARD"));

        JButton btnStart = new JButton("I Agree & Begin Examination →");
        btnStart.setBackground(new Color(16, 185, 129));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnStart.addActionListener(e -> startActiveExam());

        btnBox.add(btnCancel);
        btnBox.add(btnStart);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        panel.add(btnBox, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // SCREEN 11: ACTIVE EXAMINATION SCREEN (WITH SYNCHRONIZED TIMER)
    // =========================================================================
    private JLabel examTimerLabel;
    private JLabel examQuestionNumLabel;
    private JLabel examConceptBadge;
    private JTextArea examQuestionArea;
    private JRadioButton[] examOptionRadios;
    private ButtonGroup examOptionGroup;
    private JPanel examPalettePanel;
    private JButton btnFlag;

    private JPanel createExamScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Top Bar: Title & Timer
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel examTitle = new JLabel("Online Examination in Progress");
        examTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

        examTimerLabel = new JLabel("Time Remaining: 00:00", SwingConstants.RIGHT);
        examTimerLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        examTimerLabel.setForeground(new Color(220, 38, 38)); // Red countdown

        topBar.add(examTitle, BorderLayout.WEST);
        topBar.add(examTimerLabel, BorderLayout.EAST);

        // Center: Question Content
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel qMeta = new JPanel(new BorderLayout());
        qMeta.setOpaque(false);
        examQuestionNumLabel = new JLabel("Question 1 of 10");
        examQuestionNumLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        examConceptBadge = new JLabel("Concept: Inheritance");
        examConceptBadge.setFont(new Font("SansSerif", Font.PLAIN, 13));
        examConceptBadge.setForeground(new Color(15, 118, 110));

        qMeta.add(examQuestionNumLabel, BorderLayout.WEST);
        qMeta.add(examConceptBadge, BorderLayout.EAST);

        examQuestionArea = new JTextArea(4, 50);
        examQuestionArea.setWrapStyleWord(true);
        examQuestionArea.setLineWrap(true);
        examQuestionArea.setEditable(false);
        examQuestionArea.setFont(new Font("SansSerif", Font.BOLD, 14));
        examQuestionArea.setOpaque(false);

        // Options
        JPanel optionsBox = new JPanel(new GridLayout(4, 1, 8, 8));
        optionsBox.setOpaque(false);
        examOptionGroup = new ButtonGroup();
        examOptionRadios = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            final int optIdx = i;
            examOptionRadios[i] = new JRadioButton("Option " + (i + 1));
            examOptionRadios[i].setFont(new Font("SansSerif", Font.PLAIN, 13));
            examOptionRadios[i].setOpaque(false);
            examOptionRadios[i].addActionListener(e -> {
                activeStudentAnswers.put(currentQuestionIndex, optIdx);
                refreshPaletteButtons();
            });
            examOptionGroup.add(examOptionRadios[i]);
            optionsBox.add(examOptionRadios[i]);
        }

        // Navigation Buttons inside question box
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        navButtons.setOpaque(false);

        btnFlag = new JButton("Flag for Review");
        btnFlag.addActionListener(e -> {
            if (flaggedQuestions.contains(currentQuestionIndex)) {
                flaggedQuestions.remove(currentQuestionIndex);
            } else {
                flaggedQuestions.add(currentQuestionIndex);
            }
            refreshPaletteButtons();
        });

        JButton btnPrev = new JButton("← Previous");
        btnPrev.addActionListener(e -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayCurrentQuestion();
            }
        });

        JButton btnNext = new JButton("Next →");
        btnNext.addActionListener(e -> {
            if (currentQuestionIndex < activeQuestions.size() - 1) {
                currentQuestionIndex++;
                displayCurrentQuestion();
            }
        });

        JButton btnSubmit = new JButton("Submit Exam");
        btnSubmit.setBackground(new Color(220, 38, 38));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSubmit.addActionListener(e -> confirmAndSubmitQuiz(false));

        navButtons.add(btnFlag);
        navButtons.add(btnPrev);
        navButtons.add(btnNext);
        navButtons.add(btnSubmit);

        JPanel qBody = new JPanel(new BorderLayout(10, 10));
        qBody.setOpaque(false);
        qBody.add(examQuestionArea, BorderLayout.NORTH);
        qBody.add(optionsBox, BorderLayout.CENTER);
        qBody.add(navButtons, BorderLayout.SOUTH);

        center.add(qMeta, BorderLayout.NORTH);
        center.add(qBody, BorderLayout.CENTER);

        // Right: Question Palette
        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setPreferredSize(new Dimension(180, 0));
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel palTitle = new JLabel("Question Palette", SwingConstants.CENTER);
        palTitle.setFont(new Font("SansSerif", Font.BOLD, 13));

        examPalettePanel = new JPanel(new GridLayout(0, 3, 6, 6));
        examPalettePanel.setOpaque(false);

        right.add(palTitle, BorderLayout.NORTH);
        right.add(new JScrollPane(examPalettePanel), BorderLayout.CENTER);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void startActiveExam() {
        if (activeQuiz == null) return;
        try {
            activeQuestions = quizService.getQuestionsForQuiz(activeQuiz);
            currentQuestionIndex = 0;
            activeStudentAnswers.clear();
            flaggedQuestions.clear();
            isSubmitted = false;
            quizStartTimeStr = LocalDateTime.now().toString();

            // Initialize Synchronized QuizTimer
            long durationSeconds = (long) activeQuiz.getDurationMinutes() * 60L;
            activeTimer = new QuizTimer(durationSeconds, new QuizTimer.TimerListener() {
                @Override
                public void onTick(long remainingSeconds, String formattedTime) {
                    examTimerLabel.setText("Time Remaining: " + formattedTime);
                    if (remainingSeconds <= 60) {
                        examTimerLabel.setForeground(Color.RED);
                    }
                }

                @Override
                public void onTimeout() {
                    examTimerLabel.setText("Time Remaining: 00:00 (TIME EXPIRED)");
                    handleAutomaticTimeoutSubmission();
                }
            });

            activeTimer.start();
            buildPaletteButtons();
            displayCurrentQuestion();
            showScreen("EXAM");

        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Starting Quiz", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayCurrentQuestion() {
        if (activeQuestions.isEmpty() || currentQuestionIndex >= activeQuestions.size()) return;
        Question q = activeQuestions.get(currentQuestionIndex);

        examQuestionNumLabel.setText("Question " + (currentQuestionIndex + 1) + " of " + activeQuestions.size());
        examConceptBadge.setText("Concept: " + q.getConcept() + " (" + q.getCourseOutcome() + ")");
        examQuestionArea.setText(q.getQuestionText());

        examOptionGroup.clearSelection();
        List<String> opts = q.getOptions();
        for (int i = 0; i < 4; i++) {
            if (i < opts.size()) {
                examOptionRadios[i].setText((char)('A' + i) + ") " + opts.get(i));
                examOptionRadios[i].setVisible(true);
            } else {
                examOptionRadios[i].setVisible(false);
            }
        }

        // Restore saved answer if any
        Integer savedChoice = activeStudentAnswers.get(currentQuestionIndex);
        if (savedChoice != null && savedChoice >= 0 && savedChoice < 4) {
            examOptionRadios[savedChoice].setSelected(true);
        }

        btnFlag.setText(flaggedQuestions.contains(currentQuestionIndex) ? "Unflag" : "Flag for Review");
        refreshPaletteButtons();
    }

    private void buildPaletteButtons() {
        examPalettePanel.removeAll();
        for (int i = 0; i < activeQuestions.size(); i++) {
            final int idx = i;
            JButton btn = new JButton(String.valueOf(i + 1));
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            btn.addActionListener(e -> {
                currentQuestionIndex = idx;
                displayCurrentQuestion();
            });
            examPalettePanel.add(btn);
        }
        examPalettePanel.revalidate();
        examPalettePanel.repaint();
    }

    private void refreshPaletteButtons() {
        Component[] comps = examPalettePanel.getComponents();
        for (int i = 0; i < comps.length && i < activeQuestions.size(); i++) {
            if (comps[i] instanceof JButton) {
                JButton b = (JButton) comps[i];
                if (flaggedQuestions.contains(i)) {
                    b.setBackground(new Color(254, 240, 138)); // Yellow flagged
                } else if (activeStudentAnswers.containsKey(i)) {
                    b.setBackground(new Color(187, 247, 208)); // Green answered
                } else {
                    b.setBackground(Color.WHITE);
                }
                if (i == currentQuestionIndex) {
                    b.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                } else {
                    b.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                }
            }
        }
    }

    private synchronized void confirmAndSubmitQuiz(boolean isTimeout) {
        if (isSubmitted) return;

        if (!isTimeout) {
            int answered = activeStudentAnswers.size();
            int total = activeQuestions.size();
            int unans = total - answered;
            int conf = JOptionPane.showConfirmDialog(
                    this,
                    "You have answered " + answered + " of " + total + " questions.\n" +
                    (unans > 0 ? "You have " + unans + " unattempted questions.\n" : "") +
                    "Are you sure you want to finish and submit your examination?",
                    "Confirm Submission",
                    JOptionPane.YES_NO_OPTION
            );
            if (conf != JOptionPane.YES_OPTION) {
                return;
            }
        }

        executeSubmission(isTimeout ? "AUTOMATIC_TIMEOUT" : "MANUAL");
    }

    private synchronized void handleAutomaticTimeoutSubmission() {
        if (isSubmitted) return;
        JOptionPane.showMessageDialog(this,
                "Examination time has expired! Your answers are being automatically submitted and evaluated.",
                "Time Expired",
                JOptionPane.WARNING_MESSAGE);
        executeSubmission("AUTOMATIC_TIMEOUT");
    }

    private synchronized void executeSubmission(String type) {
        if (isSubmitted) return;
        isSubmitted = true;

        if (activeTimer != null) {
            activeTimer.stop();
        }

        try {
            User student = authService.getCurrentUser();
            if (student == null) {
                student = new Student("TEMP", "guest", "Student Candidate", "", "", "", "CS");
            }
            long timeTaken = activeTimer != null ? activeTimer.getElapsedSeconds() : 0;

            lastAttempt = resultService.evaluateAndSaveAttempt(
                    activeQuiz,
                    student,
                    activeQuestions,
                    activeStudentAnswers,
                    quizStartTimeStr,
                    timeTaken,
                    type
            );

            statusLabel.setText("Quiz submitted successfully. Score: " + lastAttempt.getScore() + "/" + lastAttempt.getMaxScore());
            showScreen("RESULTS");

        } catch (ValidationException | StorageException ex) {
            JOptionPane.showMessageDialog(this, "Submission error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // SCREEN 12: RESULTS DASHBOARD
    // =========================================================================
    private JPanel resultsContentPanel;

    private JPanel createResultsScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(25, 35, 25, 35));

        JLabel title = new JLabel("Official Examination Results", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(30, 58, 138));

        resultsContentPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        resultsContentPanel.setBackground(Color.WHITE);
        resultsContentPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        btnBox.setOpaque(false);

        JButton btnRevision = new JButton("Open Question-Based Revision →");
        btnRevision.setBackground(new Color(15, 118, 110)); // Teal
        btnRevision.setForeground(Color.WHITE);
        btnRevision.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRevision.addActionListener(e -> showScreen("REVISION"));

        JButton btnAnalytics = new JButton("Concept-Wise Analytics");
        btnAnalytics.setBackground(new Color(30, 58, 138));
        btnAnalytics.setForeground(Color.WHITE);
        btnAnalytics.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnAnalytics.addActionListener(e -> showScreen("CONCEPT_ANALYTICS"));

        JButton btnDash = new JButton("Return to Dashboard");
        btnDash.addActionListener(e -> showScreen("STUDENT_DASHBOARD"));

        btnBox.add(btnRevision);
        btnBox.add(btnAnalytics);
        btnBox.add(btnDash);

        panel.add(title, BorderLayout.NORTH);
        panel.add(resultsContentPanel, BorderLayout.CENTER);
        panel.add(btnBox, BorderLayout.SOUTH);
        return panel;
    }

    private void populateResultsCard() {
        if (lastAttempt == null) return;
        resultsContentPanel.removeAll();

        resultsContentPanel.add(new JLabel("Examination:"));
        resultsContentPanel.add(new JLabel(lastAttempt.getQuizTitle()));

        resultsContentPanel.add(new JLabel("Candidate:"));
        resultsContentPanel.add(new JLabel(lastAttempt.getStudentFullName() + " (" + lastAttempt.getStudentUsername() + ")"));

        resultsContentPanel.add(new JLabel("Score / Total Marks:"));
        JLabel scoreLbl = new JLabel(lastAttempt.getScore() + " / " + lastAttempt.getMaxScore() + "  (" + lastAttempt.getPercentage() + "%)");
        scoreLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        scoreLbl.setForeground(lastAttempt.getPercentage() >= 60 ? new Color(16, 185, 129) : new Color(220, 38, 38));
        resultsContentPanel.add(scoreLbl);

        resultsContentPanel.add(new JLabel("Breakdown:"));
        String breakdownStr = lastAttempt.getCorrectCount() + " Correct (+1), " +
                lastAttempt.getWrongCount() + " Incorrect (0), " +
                lastAttempt.getUnansweredCount() + " Unanswered (0)";
        if (lastAttempt.getInvalidCount() > 0) {
            breakdownStr += ", " + lastAttempt.getInvalidCount() + " Invalid (0)";
        }
        resultsContentPanel.add(new JLabel(breakdownStr));

        resultsContentPanel.add(new JLabel("Submission Mechanism:"));
        JLabel subType = new JLabel(lastAttempt.getSubmissionType());
        if ("AUTOMATIC_TIMEOUT".equals(lastAttempt.getSubmissionType())) {
            subType.setForeground(new Color(220, 38, 38));
            subType.setText("AUTOMATIC (Timer Expired)");
        }
        resultsContentPanel.add(subType);

        resultsContentPanel.add(new JLabel("Time Taken:"));
        resultsContentPanel.add(new JLabel(QuizTimer.formatTime(lastAttempt.getTimeTakenSeconds())));

        resultsContentPanel.revalidate();
        resultsContentPanel.repaint();
    }

    // =========================================================================
    // SCREEN 13: QUESTION-BASED REVISION SCREEN (KEY INNOVATION)
    // =========================================================================
    private JPanel revisionBox;

    private JPanel createRevisionScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Question-Based Revision & Mistake Analysis");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(30, 58, 138));

        JButton btnBack = new JButton("Back to Dashboard");
        btnBack.addActionListener(e -> showScreen("STUDENT_DASHBOARD"));
        top.add(title, BorderLayout.WEST);
        top.add(btnBack, BorderLayout.EAST);

        revisionBox = new JPanel();
        revisionBox.setLayout(new BoxLayout(revisionBox, BoxLayout.Y_AXIS));
        revisionBox.setOpaque(false);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(revisionBox), BorderLayout.CENTER);
        return panel;
    }

    private void populateRevisionScreen() {
        if (lastAttempt == null || lastAttempt.getSnapshots() == null) return;
        revisionBox.removeAll();

        List<QuestionSnapshot> snaps = lastAttempt.getSnapshots();
        for (int i = 0; i < snaps.size(); i++) {
            QuestionSnapshot s = snaps.get(i);
            JPanel card = new JPanel(new BorderLayout(8, 8));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240)),
                    new EmptyBorder(12, 16, 12, 16)
            ));
            card.setMaximumSize(new Dimension(Short.MAX_VALUE, 180));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            JLabel num = new JLabel("Q" + (i + 1) + ". Java Concept: " + s.getConcept() + " (" + s.getCourseOutcome() + ")");
            num.setFont(new Font("SansSerif", Font.BOLD, 13));

            JLabel status = new JLabel(s.getStatus());
            status.setFont(new Font("SansSerif", Font.BOLD, 12));
            if ("CORRECT".equalsIgnoreCase(s.getStatus())) {
                status.setForeground(new Color(16, 185, 129));
                status.setText("CORRECT (+1)");
            } else if ("INCORRECT".equalsIgnoreCase(s.getStatus())) {
                status.setForeground(new Color(220, 38, 38));
                status.setText("INCORRECT (0)");
            } else if ("INVALID".equalsIgnoreCase(s.getStatus())) {
                status.setForeground(new Color(234, 88, 12));
                status.setText("INVALID (0)");
            } else {
                status.setForeground(Color.GRAY);
                status.setText("UNANSWERED (0)");
            }
            header.add(num, BorderLayout.WEST);
            header.add(status, BorderLayout.EAST);

            JTextArea text = new JTextArea(s.getQuestionText());
            text.setWrapStyleWord(true);
            text.setLineWrap(true);
            text.setEditable(false);
            text.setOpaque(false);
            text.setFont(new Font("SansSerif", Font.PLAIN, 13));

            JPanel footer = new JPanel(new GridLayout(2, 1, 4, 4));
            footer.setOpaque(false);
            JLabel ans = new JLabel("Your Answer: " + s.getStudentAnswerText() + " | Correct: " + s.getCorrectAnswerText());
            ans.setFont(new Font("SansSerif", Font.BOLD, 12));

            JLabel exp = new JLabel("Rationale: " + s.getExplanation());
            exp.setFont(new Font("SansSerif", Font.ITALIC, 11));
            exp.setForeground(new Color(71, 85, 105));

            footer.add(ans);
            footer.add(exp);

            card.add(header, BorderLayout.NORTH);
            card.add(text, BorderLayout.CENTER);
            card.add(footer, BorderLayout.SOUTH);

            revisionBox.add(card);
            revisionBox.add(Box.createVerticalStrut(10));
        }

        revisionBox.revalidate();
        revisionBox.repaint();
    }

    // =========================================================================
    // SCREEN 14: CONCEPT-WISE PERFORMANCE ANALYTICS (USING HASHMAP)
    // =========================================================================
    private JPanel conceptTablePanel;

    private JPanel createConceptAnalyticsScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Concept-Wise Performance & Weakness Analytics");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(30, 58, 138));

        JButton btnBack = new JButton("Back to Dashboard");
        btnBack.addActionListener(e -> {
            User u = authService.getCurrentUser();
            if (u != null && u.getRole() == Role.INSTRUCTOR) {
                showScreen("INSTRUCTOR_DASHBOARD");
            } else {
                showScreen("STUDENT_DASHBOARD");
            }
        });
        top.add(title, BorderLayout.WEST);
        top.add(btnBack, BorderLayout.EAST);

        conceptTablePanel = new JPanel(new BorderLayout());
        conceptTablePanel.setOpaque(false);

        panel.add(top, BorderLayout.NORTH);
        panel.add(conceptTablePanel, BorderLayout.CENTER);
        return panel;
    }

    private void populateConceptAnalytics() {
        conceptTablePanel.removeAll();

        List<QuizAttempt> attempts = resultService.getAllAttempts();
        String currentStudent = authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : null;

        Map<String, ConceptPerformance> map = analyticsService.analyzeAggregateStudentAttempts(attempts, currentStudent);

        String[] cols = {"Java Concept", "Total Seen", "Attempted", "Correct", "Wrong", "Accuracy %", "Faculty Recommendation"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (ConceptPerformance cp : map.values()) {
            model.addRow(new Object[]{
                    cp.getConcept(),
                    cp.getTotalQuestions(),
                    cp.getAttempted(),
                    cp.getCorrect(),
                    cp.getWrong(),
                    cp.getAccuracyPercentage() + "%",
                    cp.getRecommendation()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);

        conceptTablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        conceptTablePanel.revalidate();
        conceptTablePanel.repaint();
    }

    // =========================================================================
    // SCREEN 15: QUIZ ATTEMPT HISTORY
    // =========================================================================
    private JPanel historyTablePanel;

    private JPanel createHistoryScreen() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Past Examination Attempts");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JButton btnBack = new JButton("Back to Dashboard");
        btnBack.addActionListener(e -> {
            User u = authService.getCurrentUser();
            if (u != null && u.getRole() == Role.INSTRUCTOR) {
                showScreen("INSTRUCTOR_DASHBOARD");
            } else {
                showScreen("STUDENT_DASHBOARD");
            }
        });
        top.add(title, BorderLayout.WEST);
        top.add(btnBack, BorderLayout.EAST);

        historyTablePanel = new JPanel(new BorderLayout());
        historyTablePanel.setOpaque(false);

        panel.add(top, BorderLayout.NORTH);
        panel.add(historyTablePanel, BorderLayout.CENTER);
        return panel;
    }

    private void populateHistoryTable() {
        historyTablePanel.removeAll();
        List<QuizAttempt> list;
        User u = authService.getCurrentUser();
        if (u != null && u.getRole() == Role.INSTRUCTOR) {
            list = resultService.getAllAttempts();
        } else if (u != null) {
            list = resultService.getStudentAttempts(u.getUsername());
        } else {
            list = resultService.getAllAttempts();
        }

        String[] cols = {"Attempt ID", "Examination", "Candidate", "Score", "Percentage", "Type", "Submission Date"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (QuizAttempt a : list) {
            model.addRow(new Object[]{
                    a.getId(),
                    a.getQuizTitle(),
                    a.getStudentUsername(),
                    a.getScore() + "/" + a.getMaxScore(),
                    a.getPercentage() + "%",
                    a.getSubmissionType(),
                    a.getSubmissionTime()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        JButton btnReview = new JButton("Review Selected Attempt Questions →");
        btnReview.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String id = (String) model.getValueAt(row, 0);
                Optional<QuizAttempt> opt = resultService.getAttemptById(id);
                if (opt.isPresent()) {
                    lastAttempt = opt.get();
                    showScreen("REVISION");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an attempt from the table.");
            }
        });
        bottom.add(btnReview);

        historyTablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        historyTablePanel.add(bottom, BorderLayout.SOUTH);
        historyTablePanel.revalidate();
        historyTablePanel.repaint();
    }

    // =========================================================================
    // SCREEN 16: STUDENT PROFILE
    // =========================================================================
    private JPanel createProfileScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(241, 245, 249));

        profileCard = new JPanel(new GridLayout(6, 2, 12, 12));
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(30, 40, 30, 40)
        ));

        JLabel title = new JLabel("Candidate Academic Profile", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(30, 58, 138));

        JButton btnBack = new JButton("Back to Dashboard");
        btnBack.addActionListener(e -> {
            User u = authService.getCurrentUser();
            if (u != null && u.getRole() == Role.INSTRUCTOR) {
                showScreen("INSTRUCTOR_DASHBOARD");
            } else {
                showScreen("STUDENT_DASHBOARD");
            }
        });

        JPanel wrap = new JPanel(new BorderLayout(15, 15));
        wrap.setOpaque(false);
        wrap.add(title, BorderLayout.NORTH);
        wrap.add(profileCard, BorderLayout.CENTER);
        wrap.add(btnBack, BorderLayout.SOUTH);

        panel.add(wrap);
        return panel;
    }

    private void populateProfileScreen() {
        if (profileCard == null) return;
        profileCard.removeAll();
        User u = authService.getCurrentUser();
        if (u != null) {
            JLabel lblNameKey = new JLabel("Full Name:");
            lblNameKey.setFont(new Font("SansSerif", Font.BOLD, 13));
            JLabel lblNameVal = new JLabel(u.getFullName());

            JLabel lblUserKey = new JLabel("Username:");
            lblUserKey.setFont(new Font("SansSerif", Font.BOLD, 13));
            JLabel lblUserVal = new JLabel(u.getUsername());

            JLabel lblRoleKey = new JLabel("System Role:");
            lblRoleKey.setFont(new Font("SansSerif", Font.BOLD, 13));
            JLabel lblRoleVal = new JLabel(u.getDisplayRoleTitle());

            JLabel lblEmailKey = new JLabel("University Email:");
            lblEmailKey.setFont(new Font("SansSerif", Font.BOLD, 13));
            JLabel lblEmailVal = new JLabel(u.getEmail() != null && !u.getEmail().isEmpty() ? u.getEmail() : "Not specified");

            JLabel lblDeptKey = new JLabel("Department / School:");
            lblDeptKey.setFont(new Font("SansSerif", Font.BOLD, 13));
            JLabel lblDeptVal = new JLabel(u.getDepartment() != null && !u.getDepartment().isEmpty() ? u.getDepartment() : "School of Advanced Computing");

            JLabel lblAttemptsKey = new JLabel("Completed Examinations:");
            lblAttemptsKey.setFont(new Font("SansSerif", Font.BOLD, 13));
            int attemptCount = resultService.getStudentAttempts(u.getUsername()).size();
            JLabel lblAttemptsVal = new JLabel(attemptCount + " attempts recorded");

            profileCard.add(lblNameKey);
            profileCard.add(lblNameVal);
            profileCard.add(lblUserKey);
            profileCard.add(lblUserVal);
            profileCard.add(lblRoleKey);
            profileCard.add(lblRoleVal);
            profileCard.add(lblEmailKey);
            profileCard.add(lblEmailVal);
            profileCard.add(lblDeptKey);
            profileCard.add(lblDeptVal);
            profileCard.add(lblAttemptsKey);
            profileCard.add(lblAttemptsVal);
        }
        profileCard.revalidate();
        profileCard.repaint();
    }

    // =========================================================================
    // SCREEN 17: SETTINGS & BACKUP
    // =========================================================================
    private JPanel createSettingsScreen() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("System Persistence & Data Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JTextArea area = new JTextArea(
                "Local Storage Management:\n\n" +
                "All user accounts, question banks, quizzes, and historical examination snapshots\n" +
                "are stored locally in the 'data/' directory using atomic file replacement and\n" +
                "automatic .bak file preservation.\n\n" +
                "Storage Files:\n" +
                "• users.json: Encrypted/salted PBKDF2 student and faculty credentials\n" +
                "• questions.json: Curated question bank with Java concepts & course outcomes\n" +
                "• quizzes.json: Timed examinations and scheduling rules\n" +
                "• attempts.json: Immutable snapshots of past student submissions"
        );
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBox.setOpaque(false);
        JButton btnBack = new JButton("Back to Dashboard");
        btnBack.addActionListener(e -> showScreen("INSTRUCTOR_DASHBOARD"));
        btnBox.add(btnBack);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        panel.add(btnBox, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // SCREEN 18: HELP / ABOUT & PROPOSAL DETAILS
    // =========================================================================
    private JPanel createAboutScreen() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("ALLIANCE UNIVERSITY — Micro-Project Details", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(30, 58, 138));

        JTextArea text = new JTextArea(
                "Alliance School of Advanced Computing\n" +
                "Online Quiz Management System with Synchronized Timer & Question-Based Revision\n\n" +
                "Student Team Members:\n" +
                "• Monisha R\n" +
                "• D Thanushree\n" +
                "• K Dheeraj Chowdary\n" +
                "• Harsha Vardhan E\n" +
                "• Kadalinga\n\n" +
                "Core Innovations:\n" +
                "1. Synchronized Multithreaded Quiz Timer on a separate background thread with automatic deadline submission.\n" +
                "2. Question-Based Revision: Preserves the exact questions, choices, and explanations from each student attempt.\n" +
                "3. Concept-Wise Performance Analytics using HashMaps to identify weaknesses in Inheritance, Polymorphism, etc.\n\n" +
                "Course Outcomes:\n" +
                "• CO-1: Classes, objects, and inheritance concepts.\n" +
                "• CO-2: Polymorphism, interfaces, and packages.\n" +
                "• CO-3: Exception handling and multithreading."
        );
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setEditable(false);
        text.setFont(new Font("SansSerif", Font.PLAIN, 14));
        text.setBorder(new EmptyBorder(20, 20, 20, 20));
        text.setBackground(Color.WHITE);

        JButton btnBack = new JButton("Close / Back");
        btnBack.addActionListener(e -> showScreen("WELCOME"));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(text), BorderLayout.CENTER);
        panel.add(btnBack, BorderLayout.SOUTH);
        return panel;
    }

    private void showCourseOutcomesDialog() {
        JOptionPane.showMessageDialog(this,
                "Alliance School of Advanced Computing — Course Outcomes (CS204):\n\n" +
                "• CO-1: Evolve Java programs incorporating classes, objects, and inheritance concepts.\n" +
                "• CO-2: Develop Java programs incorporating polymorphism, interfaces, and packages.\n" +
                "• CO-3: Create Java programs incorporating exception handling and multithreading.",
                "Course Outcomes Mapping",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleLogout() {
        authService.logout();
        updateUserStatus();
        statusLabel.setText("Session closed. Returned to Welcome screen.");
        showScreen("WELCOME");
    }
}
