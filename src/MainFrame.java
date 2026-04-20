import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainFrame extends JFrame {
    private GridRenderer gridRenderer;
    private GridWorld game;
    private NashQLearner agent1, agent2;
    private IndependentQLearner indAgent1, indAgent2;

    private JTextArea logArea;
    private JLabel statusLabel, episodeLabel, rewardLabel, epsilonLabel, stepsLabel;

    private Timer timer;
    private int episodeCount = 0;
    private int maxEpisodes = 10000;
    private boolean isTraining = false;
    private boolean useNash = true;
    private boolean isGame1 = true;
    private boolean isStochastic = false;

    private List<Double> episodeRewards = new ArrayList<>();
    private int currentEpisodeSteps = 0;
    private double totalEpisodeReward = 0;
    private int successfulEpisodes = 0;

    private static final double ALPHA = 0.2;             // увеличено для более быстрого обучения
    private static final double GAMMA = 0.95;
    private static final double EPSILON = 1.0;
    private static final double EPSILON_DECAY = 0.997;   // медленнее убывание
    private static final double MIN_EPSILON = 0.02;

    public MainFrame() {
        setTitle("Nash Q-Learning - Grid World");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        initComponents();
        initGame();
        setSize(850, 650);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel gridPanel = new JPanel(new BorderLayout());
        gridPanel.setBorder(BorderFactory.createTitledBorder("Игровое поле"));
        gridRenderer = new GridRenderer();
        gridPanel.add(gridRenderer, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Управление"));

        JPanel gameTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup gameGroup = new ButtonGroup();
        JRadioButton game1Radio = new JRadioButton("Игра 1 (разные цели)", true);
        JRadioButton game2Radio = new JRadioButton("Игра 2 (общая цель)");
        gameGroup.add(game1Radio);
        gameGroup.add(game2Radio);
        gameTypePanel.add(game1Radio);
        gameTypePanel.add(game2Radio);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox stochasticCheck = new JCheckBox("Стохастический режим");
        JCheckBox nashCheck = new JCheckBox("Nash Q-learning", true);
        modePanel.add(stochasticCheck);
        modePanel.add(nashCheck);

        JPanel trainButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton trainButton = new JButton("Обучать");
        JButton train1000Button = new JButton("Обучить 1000");
        JButton train5000Button = new JButton("Обучить 5000");
        JButton stopButton = new JButton("Остановить");
        JButton resetButton = new JButton("Сброс");
        trainButtonPanel.add(trainButton);
        trainButtonPanel.add(train1000Button);
        trainButtonPanel.add(train5000Button);
        trainButtonPanel.add(stopButton);
        trainButtonPanel.add(resetButton);

        JPanel testButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton stepButton = new JButton("Шаг");
        JButton randomStepButton = new JButton("Случ. шаг");
        JButton episodeButton = new JButton("Эпизод");
        testButtonPanel.add(stepButton);
        testButtonPanel.add(randomStepButton);
        testButtonPanel.add(episodeButton);

        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        speedPanel.add(new JLabel("Задержка (мс):"));
        JTextField delayField = new JTextField("0", 5);
        speedPanel.add(delayField);
        JButton applySpeedButton = new JButton("Применить");
        speedPanel.add(applySpeedButton);

        JPanel statusPanel = new JPanel(new GridLayout(5, 1));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Статус"));
        episodeLabel = new JLabel("Эпизод: 0");
        stepsLabel = new JLabel("Шагов: 0");
        rewardLabel = new JLabel("Награда: 0");
        epsilonLabel = new JLabel("Epsilon: 1.0");
        statusLabel = new JLabel("Готов");
        statusPanel.add(episodeLabel);
        statusPanel.add(stepsLabel);
        statusPanel.add(rewardLabel);
        statusPanel.add(epsilonLabel);
        statusPanel.add(statusLabel);

        logArea = new JTextArea(12, 40);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог"));

        controlPanel.add(gameTypePanel);
        controlPanel.add(modePanel);
        controlPanel.add(new JSeparator());
        controlPanel.add(trainButtonPanel);
        controlPanel.add(testButtonPanel);
        controlPanel.add(speedPanel);
        controlPanel.add(statusPanel);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(controlPanel, BorderLayout.NORTH);
        rightPanel.add(logScroll, BorderLayout.CENTER);
        add(gridPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Обработчики
        game1Radio.addActionListener(e -> { stopTraining(); isGame1 = true; useNash = true; nashCheck.setSelected(true); initGame(); log("Переключено на Игру 1 (Nash Q-learning)"); });
        game2Radio.addActionListener(e -> { stopTraining(); isGame1 = false; useNash = true; nashCheck.setSelected(true); initGame(); log("Переключено на Игру 2 (Nash Q-learning)"); });
        stochasticCheck.addActionListener(e -> { stopTraining(); isStochastic = stochasticCheck.isSelected(); initGame(); });
        nashCheck.addActionListener(e -> { useNash = nashCheck.isSelected(); log("Алгоритм: " + (useNash ? "Nash" : "Independent")); });

        trainButton.addActionListener(e -> startTraining());
        train1000Button.addActionListener(e -> { stopTraining(); maxEpisodes = episodeCount + 1000; startTraining(); log("Запуск обучения на 1000 эпизодов (до " + maxEpisodes + ")"); });
        train5000Button.addActionListener(e -> { stopTraining(); maxEpisodes = episodeCount + 5000; startTraining(); log("Запуск обучения на 5000 эпизодов (до " + maxEpisodes + ")"); });
        stopButton.addActionListener(e -> { stopTraining(); game.reset(); currentEpisodeSteps = 0; totalEpisodeReward = 0; updateDisplay(); log("Обучение остановлено."); });
        resetButton.addActionListener(e -> { stopTraining(); resetGame(); });

        stepButton.addActionListener(e -> { if (isTraining) stopTraining(); performStep(); });
        randomStepButton.addActionListener(e -> { if (isTraining) stopTraining(); performRandomStep(); });
        episodeButton.addActionListener(e -> { if (isTraining) stopTraining(); runSingleEpisode(); });
        applySpeedButton.addActionListener(e -> {
            try { timer.setDelay(Integer.parseInt(delayField.getText())); } catch (NumberFormatException ex) { log("Ошибка задержки"); }
        });

        timer = new Timer(0, e -> {
            if (isTraining && episodeCount < maxEpisodes) trainingStep();
            else if (episodeCount >= maxEpisodes) { stopTraining(); log("Достигнуто " + maxEpisodes + " эпизодов."); }
        });
    }

    private void initGame() {
        game = new GridWorld(isStochastic, isGame1);
        agent1 = new NashQLearner(0, ALPHA, GAMMA, EPSILON, EPSILON_DECAY, MIN_EPSILON);
        agent2 = new NashQLearner(1, ALPHA, GAMMA, EPSILON, EPSILON_DECAY, MIN_EPSILON);
        agent1.setOtherAgent(agent2);
        agent2.setOtherAgent(agent1);
        indAgent1 = new IndependentQLearner(ALPHA, GAMMA, EPSILON, EPSILON_DECAY, MIN_EPSILON);
        indAgent2 = new IndependentQLearner(ALPHA, GAMMA, EPSILON, EPSILON_DECAY, MIN_EPSILON);
        episodeCount = 0; successfulEpisodes = 0; episodeRewards.clear();
        currentEpisodeSteps = 0; totalEpisodeReward = 0;
        updateDisplay();
        log("Игра инициализирована: " + (isGame1 ? "Игра 1" : "Игра 2") + ", " + (isStochastic ? "стохастическая" : "детерминированная"));
    }

    private void startTraining() { if (!isTraining) { isTraining = true; timer.start(); statusLabel.setText("Обучение..."); log("=== Начало обучения ==="); } }
    private void stopTraining() { isTraining = false; timer.stop(); statusLabel.setText("Остановлено"); }
    private void resetGame() { stopTraining(); initGame(); log("=== Сброс выполнен ==="); }

    private void trainingStep() {
        if (game.isGameOver()) {
            episodeCount++;
            episodeRewards.add(totalEpisodeReward);
            if (game.isAgent1ReachedGoal() && game.isAgent2ReachedGoal()) successfulEpisodes++;
            if (episodeCount % 100 == 0) {
                double avg = episodeRewards.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                log(String.format("Эпизод %4d: награда = %6.1f, шагов = %2d, ε = %.3f, усп. = %d",
                        episodeCount, totalEpisodeReward, currentEpisodeSteps,
                        useNash ? agent1.getEpsilon() : indAgent1.getEpsilon(), successfulEpisodes));
                episodeRewards.clear();
            }
            agent1.decayEpsilon(); agent2.decayEpsilon();
            indAgent1.decayEpsilon(); indAgent2.decayEpsilon();
            game.reset();
            currentEpisodeSteps = 0; totalEpisodeReward = 0;
        }

        int state = game.getState();
        GridWorld.Action a1, a2;
        if (useNash) {
            a1 = agent1.selectAction(state, true);
            a2 = agent2.selectAction(state, true);
        } else {
            a1 = indAgent1.selectAction(state, true);
            a2 = indAgent2.selectAction(state, true);
        }

        double[] rewards = game.step(a1, a2);
        int nextState = game.getState();
        boolean done = game.isGameOver();

        if (useNash) {
            agent1.learn(state, a1, a2, rewards[0], nextState, done);
            agent2.learn(state, a1, a2, rewards[1], nextState, done);
        } else {
            indAgent1.learn(state, a1, rewards[0], nextState, done);
            indAgent2.learn(state, a2, rewards[1], nextState, done);
        }

        totalEpisodeReward += (rewards[0] + rewards[1]) / 2.0;
        currentEpisodeSteps++;
        updateDisplay();

        if (done) {
            timer.stop();
            try { Thread.sleep(300); } catch (InterruptedException ex) {}
            timer.start();
        }
    }

    private void performStep() {
        if (game.isGameOver()) { game.reset(); currentEpisodeSteps = 0; totalEpisodeReward = 0; updateDisplay(); log("Сброс на начальные позиции"); return; }
        int state = game.getState();
        GridWorld.Action a1, a2;
        if (useNash) {
            a1 = agent1.selectAction(state, false);
            a2 = agent2.selectAction(state, false);
        } else {
            a1 = indAgent1.selectAction(state, false);
            a2 = indAgent2.selectAction(state, false);
        }
        double[] rewards = game.step(a1, a2);
        totalEpisodeReward += (rewards[0] + rewards[1]) / 2.0;
        currentEpisodeSteps++;
        log(String.format("Шаг %d: А1=%s, А2=%s, R1=%.2f, R2=%.2f", currentEpisodeSteps, a1, a2, rewards[0], rewards[1]));
        updateDisplay();
        if (game.isGameOver()) log(String.format("=== Эпизод завершен! Шагов: %d, Награда: %.2f ===", currentEpisodeSteps, totalEpisodeReward));
    }

    private void performRandomStep() {
        if (game.isGameOver()) { game.reset(); currentEpisodeSteps = 0; totalEpisodeReward = 0; updateDisplay(); log("Сброс на начальные позиции"); return; }
        Random rand = new Random();
        GridWorld.Action a1 = GridWorld.Action.values()[rand.nextInt(4)];
        GridWorld.Action a2 = GridWorld.Action.values()[rand.nextInt(4)];
        double[] rewards = game.step(a1, a2);
        totalEpisodeReward += (rewards[0] + rewards[1]) / 2.0;
        currentEpisodeSteps++;
        log(String.format("Шаг %d (случ): А1=%s, А2=%s, R1=%.2f, R2=%.2f", currentEpisodeSteps, a1, a2, rewards[0], rewards[1]));
        updateDisplay();
        if (game.isGameOver()) log(String.format("=== Эпизод завершен! Шагов: %d, Награда: %.2f ===", currentEpisodeSteps, totalEpisodeReward));
    }

    private void runSingleEpisode() {
        game.reset(); currentEpisodeSteps = 0; totalEpisodeReward = 0; log("Запуск одного эпизода...");
        while (!game.isGameOver() && currentEpisodeSteps < 100) {
            int state = game.getState();
            GridWorld.Action a1, a2;
            if (useNash) {
                a1 = agent1.selectAction(state, false);
                a2 = agent2.selectAction(state, false);
            } else {
                a1 = indAgent1.selectAction(state, false);
                a2 = indAgent2.selectAction(state, false);
            }
            double[] rewards = game.step(a1, a2);
            totalEpisodeReward += (rewards[0] + rewards[1]) / 2.0;
            currentEpisodeSteps++;
            updateDisplay();
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
        log(String.format("=== Эпизод завершен! Шагов: %d, Награда: %.2f ===", currentEpisodeSteps, totalEpisodeReward));
    }

    private void updateDisplay() {
        gridRenderer.updateState(game.getPositions(), game.getGoals(), isGame1,
                game.isAgent1ReachedGoal(), game.isAgent2ReachedGoal());
        episodeLabel.setText("Эпизод: " + episodeCount);
        stepsLabel.setText("Шагов: " + currentEpisodeSteps);
        rewardLabel.setText(String.format("Награда: %.2f", totalEpisodeReward));
        epsilonLabel.setText(String.format("Epsilon: %.3f", useNash ? agent1.getEpsilon() : indAgent1.getEpsilon()));
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) {}
            new MainFrame().setVisible(true);
        });
    }
}