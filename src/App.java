import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class App extends Application {
    private List<Question> questions = new ArrayList<>();        // Λίστα για την αποθήκευση των ερωτήσεων
    private int currentQuestionIndex = 0;               // Δείκτης για την παρακολούθηση της τρέχουσας ερώτησης
    private int mistakes = 0;                           // Μετρητής για τα λάθη που κάνει ο χρήστης
    private int score = 0;                              // Μετρητής για το σκορ του χρήστη
    private Label questionLabel = new Label();               // Ετικέτα για την εμφάνιση της ερώτησης
    private Label timerLabel = new Label();                     // Ετικέτα για την εμφάνιση του χρονόμετρου
    private Label scoreLabel = new Label("Score: 0");         // Ετικέτα για την εμφάνιση του σκορ
    private Timeline timeline;                            // Timeline για το χρονόμετρο αντίστροφης μέτρησης
    private int timeLeft = 10;                            // Υπολειπόμενος χρόνος για την τρέχουσα ερώτηση
    private Button[] answerButtons = new Button[3];         // Κουμπιά για τις επιλογές απαντήσεων

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Φόρτωση ερωτήσεων από το αρχείο
        if (!loadQuestions("resources/questions.txt")) {
            return;  // Αν δεν φορτωθούν οι ερωτήσεις, έξοδος από την εφαρμογή
        }

        VBox root = new VBox(10);   // Δημιουργία ενός VBox layout με διάστημα 10
        root.getChildren().addAll(questionLabel, timerLabel, scoreLabel);  // Προσθήκη των ετικετών στο layout

        // Αρχικοποίηση των κουμπιών απαντήσεων και προσθήκη τους στο layout
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i] = new Button();
            int finalI = i;
            answerButtons[i].setOnAction(e -> checkAnswer(answerButtons[finalI].getText()));
            root.getChildren().add(answerButtons[i]);
        }

        Scene scene = new Scene(root, 400, 300);  
        primaryStage.setScene(scene);       
        primaryStage.setTitle("Quiz App");
        primaryStage.show();

        showNextQuestion();
    }

    // Μέθοδος για τη φόρτωση ερωτήσεων από ένα αρχείο
    private boolean loadQuestions(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String questionText = line;
                String correctAnswer = br.readLine();
                String wrongAnswer1 = br.readLine();
                String wrongAnswer2 = br.readLine();
                questions.add(new Question(questionText, correctAnswer, wrongAnswer1, wrongAnswer2));
            }
            if (questions.isEmpty()) {
                showAlert("Error", "No questions loaded. Please check the questions file.");
                return false;
            }
        } catch (IOException e) {
            showAlert("Error", "Failed to load questions: " + e.getMessage());
            return false;
        }
        return true;
    }
    
    // Μέθοδος για την εμφάνιση της επόμενης ερώτησης
    private void showNextQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            showAlert("End of Quiz", "Congratulations, you've completed all the questions!\nYour score: " + score);
            resetQuiz();  // Επαναφορά του quiz μετά την ολοκλήρωση όλων των ερωτήσεων
            return;
        }

        Question question = questions.get(currentQuestionIndex++);
        questionLabel.setText(question.getQuestionText());

        List<String> answers = question.getShuffledAnswers();
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(answers.get(i));
        }

        resetTimer();
    }

    // Μέθοδος για τον έλεγχο της επιλεγμένης απάντησης
    private void checkAnswer(String selectedAnswer) {
        if (questions.get(currentQuestionIndex - 1).isCorrectAnswer(selectedAnswer)) {
            score++;
            scoreLabel.setText("Score: " + score);  // Ενημέρωση της ετικέτας σκορ
            showNextQuestion();
        } else {
            mistakes++;
            if (mistakes >= 3) {
                showAlert("Incorrect", "Three incorrect answers, the game restarts.");  
                resetQuiz();
            } else {
                showNextQuestion();
            }
        }
    }

    // Μέθοδος για την επαναφορά του χρονόμετρου
    private void resetTimer() {
        if (timeline != null) {
            timeline.stop();
        }
        timeLeft = 10;
        timerLabel.setText("Time: " + timeLeft);
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timerLabel.setText("Time: " + timeLeft);       
            if (timeLeft == 0) {
                mistakes++;
                if (mistakes >= 3) {
                    showAlert("Time's up", "Three incorrect answers due to timeout, the game restarts.");
                    resetQuiz();
                } else {
                    showNextQuestion();
                }
            }
            timeLeft = Math.max(0, timeLeft - 1); // Ενημέρωση timeLeft και αποφυγή αρνητικών τιμών
            }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // Μέθοδος για την επαναφορά του quiz
    private void resetQuiz() {
        currentQuestionIndex = 0;
        mistakes = 0;
        score = 0;
        scoreLabel.setText("Score: " + score);
        showNextQuestion();
    }

   

    // Μέθοδος για την εμφάνιση ενός alert
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

// Κλάση που αναπαριστά μια ερώτηση του quiz
class Question {
    private String questionText;
    private String correctAnswer;
    private List<String> answers;

    public Question(String questionText, String correctAnswer, String wrongAnswer1, String wrongAnswer2) {
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        answers = new ArrayList<>();
        answers.add(correctAnswer);
        answers.add(wrongAnswer1);
        answers.add(wrongAnswer2);
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getShuffledAnswers() {
        List<String> shuffledAnswers = new ArrayList<>(answers);
        Collections.shuffle(shuffledAnswers);
        return shuffledAnswers;
    }

    public boolean isCorrectAnswer(String answer) {
        return correctAnswer.equals(answer);
    }
}
