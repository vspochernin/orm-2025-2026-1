package ru.vspochernin.gigalearn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vspochernin.gigalearn.entity.*;
import ru.vspochernin.gigalearn.repository.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public long createQuiz(long moduleId, String title, Integer timeLimitSeconds) {
        // Проверяем существование модуля
        ru.vspochernin.gigalearn.entity.Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        // Создаем квиз
        Quiz quiz = Quiz.builder()
                .title(title)
                .timeLimit(timeLimitSeconds)
                .module(module)
                .build();

        Quiz saved = quizRepository.save(quiz);
        return saved.getId();
    }

    @Transactional
    public long addQuestion(long quizId, String text) {
        // Проверяем существование квиза
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        // Создаем вопрос
        Question question = Question.builder()
                .text(text)
                .quiz(quiz)
                .build();

        Question saved = questionRepository.save(question);
        return saved.getId();
    }

    @Transactional
    public long addAnswerOption(long questionId, String text, boolean isCorrect) {
        // Проверяем существование вопроса
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        // Создаем вариант ответа
        AnswerOption option = AnswerOption.builder()
                .text(text)
                .isCorrect(isCorrect)
                .question(question)
                .build();

        AnswerOption saved = answerOptionRepository.save(option);
        return saved.getId();
    }

    @Transactional
    public QuizSubmission takeQuiz(long studentId, long quizId, Map<Long, List<Long>> answersByQuestion) {
        // Проверяем существование студента
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + studentId));

        // Проверяем существование квиза и загружаем его вопросы
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        // Загружаем все вопросы квиза
        List<Question> questions = questionRepository.findByQuizId(quizId);
        Set<Long> validQuestionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toSet());

        // Валидация: все questionId из ответов должны принадлежать этому квизу
        for (Long questionId : answersByQuestion.keySet()) {
            if (!validQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException(
                        String.format("Question %d does not belong to quiz %d", questionId, quizId)
                );
            }
        }

        // Загружаем все варианты ответов для валидации и подсчета
        Map<Long, List<AnswerOption>> optionsByQuestion = new HashMap<>();
        for (Question question : questions) {
            List<AnswerOption> options = answerOptionRepository.findByQuestionId(question.getId());
            optionsByQuestion.put(question.getId(), options);
        }

        // Валидация: все optionId должны принадлежать соответствующим вопросам
        for (Map.Entry<Long, List<Long>> entry : answersByQuestion.entrySet()) {
            Long questionId = entry.getKey();
            List<Long> selectedOptionIds = entry.getValue();

            List<AnswerOption> validOptions = optionsByQuestion.get(questionId);
            Set<Long> validOptionIds = validOptions.stream()
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());

            for (Long optionId : selectedOptionIds) {
                if (!validOptionIds.contains(optionId)) {
                    throw new IllegalArgumentException(
                            String.format("Option %d does not belong to question %d", optionId, questionId)
                    );
                }
            }
        }

        // Подсчет балла: вопрос считается верно отвеченным, если множество выбранных
        // вариантов ТОЧНО совпадает с множеством правильных вариантов
        int correctAnswers = 0;
        for (Question question : questions) {
            Long questionId = question.getId();
            List<AnswerOption> options = optionsByQuestion.get(questionId);

            // Множество правильных вариантов для этого вопроса
            Set<Long> correctOptionIds = options.stream()
                    .filter(AnswerOption::getIsCorrect)
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());

            // Множество выбранных студентом вариантов
            Set<Long> selectedOptionIds = answersByQuestion.getOrDefault(questionId, List.of())
                    .stream()
                    .collect(Collectors.toSet());

            // Проверяем точное совпадение множеств
            if (correctOptionIds.equals(selectedOptionIds)) {
                correctAnswers++;
            }
        }

        // Создаем и сохраняем результат прохождения теста
        QuizSubmission submission = QuizSubmission.builder()
                .quiz(quiz)
                .student(student)
                .score(correctAnswers)
                .takenAt(OffsetDateTime.now())
                .build();

        return quizSubmissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public Quiz getQuizById(long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<QuizSubmission> getSubmissionsByQuiz(long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new IllegalArgumentException("Quiz not found: " + quizId);
        }
        return quizSubmissionRepository.findByQuizId(quizId);
    }

    @Transactional(readOnly = true)
    public List<QuizSubmission> getSubmissionsByStudent(long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new IllegalArgumentException("User not found: " + studentId);
        }
        return quizSubmissionRepository.findByStudentId(studentId);
    }
}

