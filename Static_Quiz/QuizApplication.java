import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class QuizApplication extends JFrame {
    // Quiz data structures
    private List<Question> questions;
    private int currentQuestionIndex;
    private int correctAnswers;
    private int totalQuestions;

    // UI Components
    private JLabel questionLabel;
    private JLabel timerLabel;
    private ButtonGroup optionsGroup;
    private JRadioButton[] optionButtons;
    private JButton submitButton;
    private JPanel questionPanel;
    private JPanel optionsPanel;

    // Timer components
    private Timer timer;
    private int timeLeft;
    private final int TIME_PER_QUESTION = 15; // seconds

    public QuizApplication() {
        initializeQuestions();
        totalQuestions = questions.size();
        currentQuestionIndex = 0;
        correctAnswers = 0;

        // Setup UI
        setTitle("Quiz Application");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Timer panel at top
        timerLabel = new JLabel("Time Left: " + TIME_PER_QUESTION + " seconds");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JPanel timerPanel = new JPanel();
        timerPanel.add(timerLabel);
        add(timerPanel, BorderLayout.NORTH);

        // Question panel in center
        questionPanel = new JPanel(new BorderLayout());
        questionLabel = new JLabel();
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        questionPanel.add(questionLabel, BorderLayout.NORTH);

        // Options panel
        optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        optionButtons = new JRadioButton[4];
        optionsGroup = new ButtonGroup();

        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 16));
            optionsGroup.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }

        questionPanel.add(optionsPanel, BorderLayout.CENTER);
        add(questionPanel, BorderLayout.CENTER);

        // Submit button at bottom
        submitButton = new JButton("Submit Answer");
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        submitButton.addActionListener(e -> submitAnswer());
        JPanel submitPanel = new JPanel();
        submitPanel.add(submitButton);
        add(submitPanel, BorderLayout.SOUTH);

        // Display first question
        displayQuestion();
        startTimer();

        // Center on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeQuestions() {
        questions = new ArrayList<>();

        // Add sample questions
        questions.add(new Question(
                "What is the capital of France?",
                new String[] { "London", "Paris", "Berlin", "Madrid" },
                1));

        questions.add(new Question(
                "Which planet is known as the Red Planet?",
                new String[] { "Jupiter", "Venus", "Mars", "Saturn" },
                2));

        questions.add(new Question(
                "Who wrote 'Romeo and Juliet'?",
                new String[] { "Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain" },
                1));

        questions.add(new Question(
                "What is the largest ocean on Earth?",
                new String[] { "Atlantic Ocean", "Indian Ocean", "Arctic Ocean", "Pacific Ocean" },
                3));

        questions.add(new Question(
                "What is the chemical symbol for gold?",
                new String[] { "Go", "Gd", "Au", "Ag" },
                2));
    }

    private void displayQuestion() {
        if (currentQuestionIndex < totalQuestions) {
            Question current = questions.get(currentQuestionIndex);
            questionLabel.setText("Question " + (currentQuestionIndex + 1) + ": " + current.getQuestion());

            String[] options = current.getOptions();
            for (int i = 0; i < options.length; i++) {
                optionButtons[i].setText(options[i]);
                optionButtons[i].setSelected(false);
            }

            // Reset timer
            timeLeft = TIME_PER_QUESTION;
            timerLabel.setText("Time Left: " + timeLeft + " seconds");
        } else {
            showResults();
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeLeft--;
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText("Time Left: " + timeLeft + " seconds");

                    if (timeLeft <= 5) {
                        timerLabel.setForeground(Color.RED);
                    } else {
                        timerLabel.setForeground(Color.BLACK);
                    }

                    if (timeLeft <= 0) {
                        timer.cancel();
                        JOptionPane.showMessageDialog(QuizApplication.this,
                                "Time's up! Moving to next question.",
                                "Time Expired",
                                JOptionPane.INFORMATION_MESSAGE);
                        moveToNextQuestion();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void submitAnswer() {
        // Check if any option is selected
        boolean answered = false;
        for (JRadioButton button : optionButtons) {
            if (button.isSelected()) {
                answered = true;
                break;
            }
        }

        if (!answered) {
            JOptionPane.showMessageDialog(this,
                    "Please select an answer!",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Find selected answer
        int selectedAnswer = -1;
        for (int i = 0; i < optionButtons.length; i++) {
            if (optionButtons[i].isSelected()) {
                selectedAnswer = i;
                break;
            }
        }

        // Check if answer is correct
        Question current = questions.get(currentQuestionIndex);
        if (selectedAnswer == current.getCorrectAnswerIndex()) {
            correctAnswers++;
        }

        // Move to next question
        moveToNextQuestion();
    }

    private void moveToNextQuestion() {
        if (timer != null) {
            timer.cancel();
        }

        currentQuestionIndex++;
        if (currentQuestionIndex < totalQuestions) {
            displayQuestion();
            startTimer();
        } else {
            showResults();
        }
    }

    private void showResults() {
        if (timer != null) {
            timer.cancel();
        }

        // Remove all components
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // Create results panel
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Quiz Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLabel = new JLabel("Your Score: " + correctAnswers + " out of " + totalQuestions);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Calculate percentage
        double percentage = (double) correctAnswers / totalQuestions * 100;
        JLabel percentageLabel = new JLabel(String.format("Percentage: %.1f%%", percentage));
        percentageLabel.setFont(new Font("Arial", Font.BOLD, 20));
        percentageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Performance message
        String performance;
        if (percentage >= 80) {
            performance = "Excellent!";
        } else if (percentage >= 60) {
            performance = "Good job!";
        } else if (percentage >= 40) {
            performance = "Not bad, keep practicing!";
        } else {
            performance = "Need more practice!";
        }

        JLabel performanceLabel = new JLabel(performance);
        performanceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        performanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Summary of questions and answers
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Question Summary"));

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            JLabel questionSummary = new JLabel((i + 1) + ". " + q.getQuestion());
            JLabel correctAnswer = new JLabel("   Correct Answer: " + q.getOptions()[q.getCorrectAnswerIndex()]);
            correctAnswer.setForeground(Color.GREEN.darker());

            summaryPanel.add(questionSummary);
            summaryPanel.add(correctAnswer);
            summaryPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Add components to results panel
        resultsPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        resultsPanel.add(titleLabel);
        resultsPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        resultsPanel.add(scoreLabel);
        resultsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        resultsPanel.add(percentageLabel);
        resultsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        resultsPanel.add(performanceLabel);
        resultsPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add components to main panel with scroll pane for summary
        JScrollPane scrollPane = new JScrollPane(summaryPanel);
        scrollPane.setPreferredSize(new Dimension(550, 200));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(resultsPanel);
        mainPanel.add(scrollPane);

        // Add restart button
        JButton restartButton = new JButton("Restart Quiz");
        restartButton.setFont(new Font("Arial", Font.BOLD, 16));
        restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        restartButton.addActionListener(e -> {
            dispose();
            new QuizApplication();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(restartButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    // Question class to store quiz questions and answers
    private static class Question {
        private String question;
        private String[] options;
        private int correctAnswerIndex;

        public Question(String question, String[] options, int correctAnswerIndex) {
            this.question = question;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }

        public String getQuestion() {
            return question;
        }

        public String[] getOptions() {
            return options;
        }

        public int getCorrectAnswerIndex() {
            return correctAnswerIndex;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizApplication());
    }
}