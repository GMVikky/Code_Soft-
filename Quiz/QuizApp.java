import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class Question {
    private String questionText;
    private String[] options;
    private int correctAnswerIndex;
    private int timeLimit;

    public Question(String questionText, String[] options, int correctAnswerIndex, int timeLimit) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.timeLimit = timeLimit;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public String getCorrectAnswer() {
        return options[correctAnswerIndex].substring(3); 
    }
}

class QuizResult {
    private int totalQuestions;
    private int correctAnswers;
    private List<Integer> userAnswers;
    private List<Boolean> answerResults;
    private List<String> timedOutQuestions;

    public QuizResult() {
        this.totalQuestions = 0;
        this.correctAnswers = 0;
        this.userAnswers = new ArrayList<>();
        this.answerResults = new ArrayList<>();
        this.timedOutQuestions = new ArrayList<>();
    }

    public void addResult(int questionNumber, int userAnswer, boolean isCorrect, boolean isTimedOut) {
        totalQuestions++;

        if (isCorrect) {
            correctAnswers++;
        }

        userAnswers.add(userAnswer);
        answerResults.add(isCorrect);

        if (isTimedOut) {
            timedOutQuestions.add("Question " + (questionNumber + 1));
        }
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getScore() {
        return totalQuestions == 0 ? 0 : (int) (((double) correctAnswers / totalQuestions) * 100);
    }

    public List<String> getTimedOutQuestions() {
        return timedOutQuestions;
    }

    public List<Boolean> getAnswerResults() {
        return answerResults;
    }
}

class ApiClient {
    private static final String API_URL = "https://opentdb.com/api.php";

    /**
     * Fetches questions from the Open Trivia Database API
     * @param amount Number of questions to fetch
     * @param category Category ID (optional, use null for any)
     * @param difficulty Difficulty level (optional, use null for any)
     * @param type Type of questions (optional, use null for any)
     * @return List of Question objects
     */
    public static List<Question> fetchQuestions(int amount, Integer category, String difficulty, String type) {
        List<Question> questions = new ArrayList<>();

        try {
            StringBuilder urlBuilder = new StringBuilder(API_URL);
            urlBuilder.append("?amount=").append(amount);

            if (category != null) {
                urlBuilder.append("&category=").append(category);
            }

            if (difficulty != null && !difficulty.isEmpty()) {
                urlBuilder.append("&difficulty=").append(difficulty.toLowerCase());
            }

            if (type != null && !type.isEmpty()) {
                urlBuilder.append("&type=").append(type.toLowerCase());
            }

            System.out.println("Connecting to: " + urlBuilder.toString());

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.lines().collect(Collectors.joining());
                reader.close();

               questions = parseQuestions(response);
            } else {
                System.out.println("Failed to fetch questions. Response code: " + responseCode);
                questions = getSampleQuestions();
            }

            connection.disconnect();

        } catch (Exception e) {
            System.out.println("Error fetching questions from API: " + e.getMessage());
            e.printStackTrace();
            questions = getSampleQuestions();
        }

        return questions;
    }

    private static List<Question> parseQuestions(String jsonResponse) throws ParseException {
        List<Question> questions = new ArrayList<>();

        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);

        long responseCode = (long) jsonObject.get("response_code");
        if (responseCode != 0) {
            System.out.println("API returned error code: " + responseCode);
            return getSampleQuestions();
        }

        JSONArray results = (JSONArray) jsonObject.get("results");

        for (Object obj : results) {
            JSONObject questionObj = (JSONObject) obj;

            String questionText = (String) questionObj.get("question");

            questionText = decodeHtmlEntities(questionText);

            String correctAnswer = (String) questionObj.get("correct_answer");
            correctAnswer = decodeHtmlEntities(correctAnswer);

            JSONArray incorrectAnswers = (JSONArray) questionObj.get("incorrect_answers");

            String[] options = new String[incorrectAnswers.size() + 1];

            int correctAnswerIndex = incorrectAnswers.size();

            for (int i = 0; i < incorrectAnswers.size(); i++) {
                String incorrectAnswer = (String) incorrectAnswers.get(i);
                incorrectAnswer = decodeHtmlEntities(incorrectAnswer);
                options[i] = (char)('A' + i) + ". " + incorrectAnswer;
            }

            options[correctAnswerIndex] = (char)('A' + correctAnswerIndex) + ". " + correctAnswer;

            int timeLimit = 20 + (int)(Math.random() * 11);

            questions.add(new Question(questionText, options, correctAnswerIndex, timeLimit));
        }

        return questions;
    }

    private static String decodeHtmlEntities(String input) {
        return input.replaceAll("&quot;", "\"")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&#039;", "'")
                   .replaceAll("&rsquo;", "'")
                   .replaceAll("&lsquo;", "'")
                   .replaceAll("&rdquo;", "\"")
                   .replaceAll("&ldquo;", "\"");
    }

    private static List<Question> getSampleQuestions() {
        System.out.println("Using fallback sample questions...");
        List<Question> questions = new ArrayList<>();

        questions.add(new Question(
                "What is the capital of France?",
                new String[]{"A. London", "B. Berlin", "C. Paris", "D. Madrid"},
                2, 30)); 

        questions.add(new Question(
                "Which planet is known as the Red Planet?",
                new String[]{"A. Venus", "B. Mars", "C. Jupiter", "D. Saturn"},
                1, 30));

        questions.add(new Question(
                "What is the largest mammal in the world?",
                new String[]{"A. Elephant", "B. Giraffe", "C. Blue Whale", "D. Polar Bear"},
                2, 30));

        questions.add(new Question(
                "Who wrote 'Romeo and Juliet'?",
                new String[]{"A. Charles Dickens", "B. William Shakespeare", "C. Jane Austen", "D. Mark Twain"},
                1, 30)); 

        questions.add(new Question(
                "What is the chemical symbol for gold?",
                new String[]{"A. Go", "B. Gd", "C. Au", "D. Ag"},
                2, 30)); 

        return questions;
    }
}

class QuizApp extends JFrame {

    private static final Color PRIMARY_COLOR = new Color(70, 130, 180); 
    private static final Color SECONDARY_COLOR = new Color(240, 248, 255); 
    private static final Color ACCENT_COLOR = new Color(255, 69, 0); 
    private static final Color TEXT_COLOR = new Color(33, 33, 33); 
    private static final Color CORRECT_COLOR = new Color(46, 204, 113); 
    private static final Color INCORRECT_COLOR = new Color(231, 76, 60); 

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private List<Question> questions;
    private QuizResult result;
    private int currentQuestionIndex;
    private ExecutorService executorService;
    private ScheduledExecutorService timerExecutor;
    private Future<?> timerFuture;
    private AtomicBoolean questionAnswered;
    private int selectedDifficulty = 0;
    private int selectedCategory = 0;
    private int selectedNumQuestions = 5;

    private JPanel welcomePanel;

    private JPanel loadingPanel;
    private JLabel loadingLabel;
    private JProgressBar loadingProgressBar;

    private JPanel questionPanel;
    private JLabel questionNumberLabel;
    private JLabel timerLabel;
    private JProgressBar timerProgressBar;
    private JTextArea questionTextArea;
    private JPanel optionsPanel;
    private ButtonGroup optionsGroup;
    private Map<String, JRadioButton> optionButtons;
    private JButton nextButton;

    private JPanel resultsPanel;

    public QuizApp() {

        result = new QuizResult();
        currentQuestionIndex = 0;
        executorService = Executors.newCachedThreadPool();
        timerExecutor = Executors.newSingleThreadScheduledExecutor();
        questionAnswered = new AtomicBoolean(false);
        optionButtons = new HashMap<>();

        setTitle("Dynamic Quiz Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 500));
        setLocationRelativeTo(null);

        createUI();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void createUI() {

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        createWelcomeScreen();
        createLoadingScreen();
        createQuestionScreen();
        createResultsScreen();

        mainPanel.add(welcomePanel, "welcome");
        mainPanel.add(loadingPanel, "loading");
        mainPanel.add(questionPanel, "question");
        mainPanel.add(resultsPanel, "results");

        cardLayout.show(mainPanel, "welcome");

        add(mainPanel);
    }

    private void createWelcomeScreen() {
        welcomePanel = new JPanel();
        welcomePanel.setLayout(new BorderLayout());
        welcomePanel.setBackground(SECONDARY_COLOR);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Dynamic Quiz Application");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SECONDARY_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JLabel welcomeLabel = new JLabel("Welcome to the Quiz!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomeLabel.setForeground(TEXT_COLOR);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descriptionLabel = new JLabel("Test your knowledge with questions from various categories.");
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        descriptionLabel.setForeground(TEXT_COLOR);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(3, 2, 10, 15));
        optionsPanel.setBackground(SECONDARY_COLOR);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        optionsPanel.setMaximumSize(new Dimension(500, 200));
        optionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel numQuestionsLabel = new JLabel("Number of Questions:");
        numQuestionsLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JComboBox<String> numQuestionsCombo = new JComboBox<>(new String[]{"5", "10", "15", "20"});
        numQuestionsCombo.setSelectedIndex(0);
        numQuestionsCombo.addActionListener(e -> {
            selectedNumQuestions = Integer.parseInt((String) numQuestionsCombo.getSelectedItem());
        });

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
            "General Knowledge", "Science", "Entertainment", "History", "Any Category"
        });
        categoryCombo.setSelectedIndex(0);
        categoryCombo.addActionListener(e -> {
            selectedCategory = categoryCombo.getSelectedIndex();
        });

        JLabel difficultyLabel = new JLabel("Difficulty:");
        difficultyLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JComboBox<String> difficultyCombo = new JComboBox<>(new String[]{
            "Easy", "Medium", "Hard", "Any Difficulty"
        });
        difficultyCombo.setSelectedIndex(0);
        difficultyCombo.addActionListener(e -> {
            selectedDifficulty = difficultyCombo.getSelectedIndex();
        });

        optionsPanel.add(numQuestionsLabel);
        optionsPanel.add(numQuestionsCombo);
        optionsPanel.add(categoryLabel);
        optionsPanel.add(categoryCombo);
        optionsPanel.add(difficultyLabel);
        optionsPanel.add(difficultyCombo);

        JButton startButton = new JButton("Start Quiz");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setForeground(Color.BLACK);
        startButton.setBackground(PRIMARY_COLOR);
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        startButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> startQuiz());

        contentPanel.add(welcomeLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(descriptionLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        contentPanel.add(optionsPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        contentPanel.add(startButton);

        welcomePanel.add(headerPanel, BorderLayout.NORTH);
        welcomePanel.add(contentPanel, BorderLayout.CENTER);
    }

    private void createLoadingScreen() {
        loadingPanel = new JPanel();
        loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
        loadingPanel.setBackground(SECONDARY_COLOR);
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        loadingLabel = new JLabel("Fetching Questions...");
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 20));
        loadingLabel.setForeground(TEXT_COLOR);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loadingProgressBar = new JProgressBar();
        loadingProgressBar.setIndeterminate(true);
        loadingProgressBar.setPreferredSize(new Dimension(400, 20));
        loadingProgressBar.setMaximumSize(new Dimension(400, 20));
        loadingProgressBar.setForeground(PRIMARY_COLOR);
        loadingProgressBar.setBackground(Color.WHITE);
        loadingProgressBar.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        loadingProgressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        loadingPanel.add(Box.createVerticalGlue());
        loadingPanel.add(loadingLabel);
        loadingPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        loadingPanel.add(loadingProgressBar);
        loadingPanel.add(Box.createVerticalGlue());
    }

    private void createQuestionScreen() {
        questionPanel = new JPanel(new BorderLayout(0, 10));
        questionPanel.setBackground(SECONDARY_COLOR);
        questionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        questionNumberLabel = new JLabel("Question 1 of 5");
        questionNumberLabel.setFont(new Font("Arial", Font.BOLD, 16));
        questionNumberLabel.setForeground(Color.WHITE);

        JPanel timerPanel = new JPanel(new BorderLayout(5, 0));
        timerPanel.setOpaque(false);

        timerLabel = new JLabel("15s");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setForeground(Color.WHITE);

        timerProgressBar = new JProgressBar(0, 100);
        timerProgressBar.setValue(100);
        timerProgressBar.setPreferredSize(new Dimension(150, 15));
        timerProgressBar.setForeground(ACCENT_COLOR);
        timerProgressBar.setBackground(new Color(255, 255, 255, 100));
        timerProgressBar.setBorder(null);
        timerProgressBar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionBackground() {
                return Color.WHITE;
            }

            @Override
            protected Color getSelectionForeground() {
                return Color.WHITE;
            }
        });

        timerPanel.add(timerProgressBar, BorderLayout.CENTER);
        timerPanel.add(timerLabel, BorderLayout.EAST);

        headerPanel.add(questionNumberLabel, BorderLayout.WEST);
        headerPanel.add(timerPanel, BorderLayout.EAST);

        JPanel questionContentPanel = new JPanel(new BorderLayout());
        questionContentPanel.setBackground(SECONDARY_COLOR);
        questionContentPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        questionTextArea = new JTextArea();
        questionTextArea.setFont(new Font("Arial", Font.BOLD, 18));
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setEditable(false);
        questionTextArea.setFocusable(false);
        questionTextArea.setBackground(SECONDARY_COLOR);
        questionTextArea.setForeground(TEXT_COLOR);
        questionTextArea.setBorder(BorderFactory.createEmptyBorder(10, 5, 20, 5));

        optionsPanel = new JPanel(new GridLayout(4, 1, 0, 10));
        optionsPanel.setBackground(SECONDARY_COLOR);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 20, 5));

        optionsGroup = new ButtonGroup();

        for (char option = 'A'; option <= 'D'; option++) {
            String optionKey = String.valueOf(option);
            JRadioButton optionButton = createOptionButton(optionKey);
            optionButtons.put(optionKey, optionButton);
            optionsPanel.add(optionButton);
        }

        questionContentPanel.add(questionTextArea, BorderLayout.NORTH);
        questionContentPanel.add(optionsPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(SECONDARY_COLOR);

        nextButton = new JButton("Next");
        nextButton.setFont(new Font("Arial", Font.BOLD, 16));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(PRIMARY_COLOR);
        nextButton.setFocusPainted(false);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 25, 8, 25));
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> {
            if (questionAnswered.get()) {
                processAnswer();
            } else {

                questionAnswered.set(true);
                processAnswer();
            }
        });

        footerPanel.add(nextButton);

        questionPanel.add(headerPanel, BorderLayout.NORTH);
        questionPanel.add(questionContentPanel, BorderLayout.CENTER);
        questionPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private JRadioButton createOptionButton(String optionKey) {
        JRadioButton optionButton = new JRadioButton();
        optionButton.setFont(new Font("Arial", Font.PLAIN, 16));
        optionButton.setBackground(SECONDARY_COLOR);
        optionButton.setForeground(TEXT_COLOR);
        optionButton.setFocusPainted(false);
        optionButton.setActionCommand(optionKey);

        optionButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        optionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                optionButton.setFont(new Font("Arial", Font.BOLD, 16));
                optionButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    BorderFactory.createEmptyBorder(4, 9, 4, 9)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                optionButton.setFont(new Font("Arial", Font.PLAIN, 16));
                optionButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
        });

        optionButton.addActionListener(e -> {

            if (!questionAnswered.get()) {
                nextButton.setEnabled(true);
            }
        });

        optionsGroup.add(optionButton);
        return optionButton;
    }

    private void createResultsScreen() {
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BorderLayout());
        resultsPanel.setBackground(SECONDARY_COLOR);

    }

    private void updateResultsScreen() {
        resultsPanel.removeAll();

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Quiz Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SECONDARY_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JPanel scorePanel = new JPanel();
        scorePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        scorePanel.setBackground(SECONDARY_COLOR);
        scorePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        int score = result.getScore();
        JLabel scoreLabel = new JLabel(score + "%");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 60));
        scoreLabel.setForeground(score >= 70 ? CORRECT_COLOR : (score >= 40 ? PRIMARY_COLOR : INCORRECT_COLOR));

        scorePanel.add(scoreLabel);

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(2, 2, 20, 10));
        statsPanel.setBackground(SECONDARY_COLOR);
        statsPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        statsPanel.setMaximumSize(new Dimension(400, 100));
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel totalQuestionsLabel = new JLabel("Total Questions:");
        totalQuestionsLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel totalQuestionsValue = new JLabel(String.valueOf(result.getTotalQuestions()));
        totalQuestionsValue.setFont(new Font("Arial", Font.PLAIN, 16));

        JLabel correctAnswersLabel = new JLabel("Correct Answers:");
        correctAnswersLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel correctAnswersValue = new JLabel(String.valueOf(result.getCorrectAnswers()));
        correctAnswersValue.setFont(new Font("Arial", Font.PLAIN, 16));

        statsPanel.add(totalQuestionsLabel);
        statsPanel.add(totalQuestionsValue);
        statsPanel.add(correctAnswersLabel);
        statsPanel.add(correctAnswersValue);

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(SECONDARY_COLOR);
        summaryPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR), 
            "Question Summary", 
            TitledBorder.LEFT, 
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            PRIMARY_COLOR
        ));
        summaryPanel.setMaximumSize(new Dimension(600, 300));
        summaryPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<Boolean> answerResults = result.getAnswerResults();
        for (int i = 0; i < answerResults.size(); i++) {
            JPanel questionResultPanel = new JPanel();
            questionResultPanel.setLayout(new BorderLayout());
            questionResultPanel.setBackground(answerResults.get(i) ? new Color(240, 255, 240) : new Color(255, 240, 240));
            questionResultPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel questionLabel = new JLabel("Question " + (i + 1));
            questionLabel.setFont(new Font("Arial", Font.BOLD, 14));
            questionLabel.setForeground(answerResults.get(i) ? CORRECT_COLOR : INCORRECT_COLOR);

            JLabel resultLabel = new JLabel(answerResults.get(i) ? "Correct" : "Incorrect");
            resultLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            resultLabel.setForeground(answerResults.get(i) ? CORRECT_COLOR : INCORRECT_COLOR);

            questionResultPanel.add(questionLabel, BorderLayout.WEST);
            questionResultPanel.add(resultLabel, BorderLayout.EAST);

            summaryPanel.add(questionResultPanel);
            if (i < answerResults.size() - 1) {
                summaryPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        List<String> timedOutQuestions = result.getTimedOutQuestions();
        if (!timedOutQuestions.isEmpty()) {
            JPanel timedOutPanel = new JPanel();
            timedOutPanel.setLayout(new BoxLayout(timedOutPanel, BoxLayout.Y_AXIS));
            timedOutPanel.setBackground(SECONDARY_COLOR);
            timedOutPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR), 
                "Timed Out Questions", 
                TitledBorder.LEFT, 
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                ACCENT_COLOR
            ));
            timedOutPanel.setMaximumSize(new Dimension(600, 100));
            timedOutPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel timedOutLabel = new JLabel("You ran out of time on: " + String.join(", ", timedOutQuestions));
            timedOutLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            timedOutLabel.setForeground(ACCENT_COLOR);
            timedOutLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            timedOutLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            timedOutPanel.add(timedOutLabel);
            summaryPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            summaryPanel.add(timedOutPanel);
        }

        JScrollPane scrollPane = new JScrollPane(summaryPanel);
        scrollPane.setBorder(null);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        scrollPane.getVerticalScrollBar().setUnitIncrement(56);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(SECONDARY_COLOR);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton restartButton = new JButton("Take Another Quiz");
        restartButton.setFont(new Font("Arial", Font.BOLD, 16));
        restartButton.setForeground(Color.WHITE);
        restartButton.setBackground(PRIMARY_COLOR);
        restartButton.setFocusPainted(false);
        restartButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        restartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        restartButton.addActionListener(e -> resetQuiz());

        buttonPanel.add(restartButton);

        contentPanel.add(scorePanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(statsPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(scrollPane);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(buttonPanel);

        resultsPanel.add(headerPanel, BorderLayout.NORTH);
        resultsPanel.add(new JScrollPane(contentPanel), BorderLayout.CENTER);

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void startQuiz() {

        cardLayout.show(mainPanel, "loading");

        result = new QuizResult();
        currentQuestionIndex = 0;

        executorService.submit(() -> {
            try {

                String difficulty = null;
                if (selectedDifficulty < 3) {
                    difficulty = new String[]{"easy", "medium", "hard"}[selectedDifficulty];
                }

                Integer category = null;
                if (selectedCategory < 4) {

                    category = new Integer[]{9, 17, 11, 23}[selectedCategory];
                }

                questions = ApiClient.fetchQuestions(selectedNumQuestions, category, difficulty, "multiple");

                SwingUtilities.invokeLater(() -> {
                    if (questions.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to load questions. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        cardLayout.show(mainPanel, "welcome");
                    } else {
                        showQuestion(0);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    cardLayout.show(mainPanel, "welcome");
                });
            }
        });
    }

    private void showQuestion(int index) {

        cancelTimer();

        questionAnswered.set(false);
        optionsGroup.clearSelection();
        nextButton.setEnabled(false);

        if (index >= questions.size()) {

            updateResultsScreen();
            cardLayout.show(mainPanel, "results");
            return;
        }

        Question question = questions.get(index);

        questionNumberLabel.setText("Question " + (index + 1) + " of " + questions.size());
        questionTextArea.setText(question.getQuestionText());

        String[] options = question.getOptions();
        int optionIndex = 0;
        for (char option = 'A'; option <= 'D' && optionIndex < options.length; option++, optionIndex++) {
            String optionKey = String.valueOf(option);
            JRadioButton optionButton = optionButtons.get(optionKey);

            if (optionButton != null) {
                if (optionIndex < options.length) {

                    String optionText = options[optionIndex];
                    optionButton.setText(optionText);
                    optionButton.setVisible(true);

                    optionButton.setFont(new Font("Arial", Font.PLAIN, 16));
                    optionButton.setForeground(TEXT_COLOR);
                } else {
                    optionButton.setVisible(false);
                }
            }
        }

        startTimer(question.getTimeLimit());

        cardLayout.show(mainPanel, "question");
    }

    private void startTimer(int timeLimit) {

        timerLabel.setText(timeLimit + "s");
        timerProgressBar.setValue(100);

        final int[] timeRemaining = {timeLimit};
        final long startTime = System.currentTimeMillis();
        final long duration = timeLimit * 1000L;

        timerFuture = timerExecutor.scheduleAtFixedRate(() -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int remainingTime = Math.max(0, (int)((duration - elapsedTime) / 1000));
            int progressValue = Math.max(0, (int)((duration - elapsedTime) * 100 / duration));

            if (remainingTime != timeRemaining[0]) {
                timeRemaining[0] = remainingTime;
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText(remainingTime + "s");

                    if (remainingTime <= 5) {
                        timerLabel.setForeground(INCORRECT_COLOR);
                        timerProgressBar.setForeground(INCORRECT_COLOR);
                    }
                });
            }

            SwingUtilities.invokeLater(() -> {
                timerProgressBar.setValue(progressValue);
            });

            if (elapsedTime >= duration && !questionAnswered.get()) {

                questionAnswered.set(true);
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText("Time's up!");
                    nextButton.setEnabled(true);

                    highlightCorrectAnswer();

                    result.addResult(
                        currentQuestionIndex,
                        -1, 
                        false, 
                        true 
                    );
                });

                cancelTimer();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void cancelTimer() {
        if (timerFuture != null && !timerFuture.isDone()) {
            timerFuture.cancel(true);
        }
    }

    private void processAnswer() {

        cancelTimer();

        int selectedAnswerIndex = -1;
        Enumeration<AbstractButton> buttons = optionsGroup.getElements();

        char option = 'A';
        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                selectedAnswerIndex = option - 'A';
                break;
            }
            option++;
        }

        Question question = questions.get(currentQuestionIndex);
        int correctAnswerIndex = question.getCorrectAnswerIndex();

        boolean isCorrect = (selectedAnswerIndex == correctAnswerIndex);

        result.addResult(currentQuestionIndex, selectedAnswerIndex, isCorrect, false);

        highlightCorrectAnswer();

        Timer delayTimer = new Timer(1500, e -> {
            currentQuestionIndex++;
            showQuestion(currentQuestionIndex);
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void highlightCorrectAnswer() {
        Question question = questions.get(currentQuestionIndex);
        int correctIndex = question.getCorrectAnswerIndex();

        char option = 'A';
        for (int i = 0; i < 4; i++, option++) {
            JRadioButton button = optionButtons.get(String.valueOf(option));
            if (i == correctIndex) {
                button.setFont(new Font("Arial", Font.BOLD, 16));
            }
        }
    }

    private void resetQuiz() {

        cardLayout.show(mainPanel, "welcome");

        result = new QuizResult();
        currentQuestionIndex = 0;

        optionsGroup.clearSelection();
        for (JRadioButton button : optionButtons.values()) {
            button.setForeground(TEXT_COLOR);
            button.setFont(new Font("Arial", Font.PLAIN, 16));
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
    }

    private void cleanup() {

        if (executorService != null) {
            executorService.shutdownNow();
        }

        if (timerExecutor != null) {
            timerExecutor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        try {

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            QuizApp quizApp = new QuizApp();
            quizApp.setVisible(true);
        });
    }
}