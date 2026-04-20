import java.util.*;

public class NashQLearner {
    private double[][][] qTable;
    private double alpha;
    private double gamma;
    private double epsilon;
    private double epsilonDecay;
    private double minEpsilon;
    private int agentId;
    private Random random;

    private NashQLearner otherAgent;

    private static final int MAX_STATES = 81;

    public NashQLearner(int agentId, double alpha, double gamma,
                        double epsilon, double epsilonDecay, double minEpsilon) {
        this.agentId = agentId;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.epsilonDecay = epsilonDecay;
        this.minEpsilon = minEpsilon;
        this.qTable = new double[MAX_STATES][4][4];
        this.random = new Random();

        // Оптимистичная инициализация
        for (int s = 0; s < MAX_STATES; s++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    qTable[s][i][j] = 1.0;
                }
            }
        }
    }

    public void setOtherAgent(NashQLearner other) {
        this.otherAgent = other;
    }

    private boolean isAtGoal(int state) {
        int pos1 = state / 9;
        int pos2 = state % 9;
        int myPos = (agentId == 0) ? pos1 : pos2;
        // В Игре 1 цели: 8 для агента 0, 6 для агента 1
        // В Игре 2 цель 7 для обоих, но там эпизод завершается раньше
        return (agentId == 0 && myPos == 8) || (agentId == 1 && myPos == 6);
    }

    public GridWorld.Action selectAction(int state, boolean training) {
        int safeState = Math.min(state, MAX_STATES - 1);

        // Если агент уже у цели, действие не важно
        if (isAtGoal(safeState)) {
            return GridWorld.Action.values()[random.nextInt(4)];
        }

        if (training && random.nextDouble() < epsilon) {
            return GridWorld.Action.values()[random.nextInt(4)];
        } else {
            NashSolver.NashEquilibrium eq = getNashEquilibrium(safeState);

            if (eq.isPure) {
                int action = (agentId == 0) ? eq.action1 : eq.action2;
                return GridWorld.Action.fromValue(action);
            } else {
                double[] strategy = (agentId == 0) ? eq.strategy1 : eq.strategy2;
                double r = random.nextDouble();
                double sum = 0;
                for (int i = 0; i < 4; i++) {
                    sum += strategy[i];
                    if (r < sum) {
                        return GridWorld.Action.fromValue(i);
                    }
                }
                return GridWorld.Action.values()[random.nextInt(4)];
            }
        }
    }

    public void learn(int state, GridWorld.Action a1, GridWorld.Action a2,
                      double reward, int nextState, boolean done) {
        int safeState = Math.min(state, MAX_STATES - 1);
        int safeNextState = Math.min(nextState, MAX_STATES - 1);
        int a1Idx = a1.getValue();
        int a2Idx = a2.getValue();

        // Если агент уже достиг своей цели, не обновляем Q-значения
        if (isAtGoal(safeState)) {
            return;
        }

        NashSolver.NashEquilibrium eq = done ? null : getNashEquilibrium(safeNextState);
        double targetValue = done ? 0.0 : (agentId == 0 ? eq.value1 : eq.value2);

        double target = reward + gamma * targetValue;
        double oldValue = qTable[safeState][a1Idx][a2Idx];
        qTable[safeState][a1Idx][a2Idx] += alpha * (target - oldValue);

        // Симметричное обновление
        int symState = (state % 9) * 9 + (state / 9);
        if (symState != state && symState < MAX_STATES) {
            qTable[symState][a2Idx][a1Idx] += alpha * (target - qTable[symState][a2Idx][a1Idx]);
        }
    }

    public void decayEpsilon() {
        epsilon = Math.max(minEpsilon, epsilon * epsilonDecay);
    }

    private NashSolver.NashEquilibrium getNashEquilibrium(int state) {
        double[][] q1 = new double[4][4];
        double[][] q2 = new double[4][4];

        int pos1 = state / 9;
        int pos2 = state % 9;

        // Если агент 1 уже в цели, его Q-матрица обнуляется (он безразличен)
        if (pos1 == 8) {
            for (int i = 0; i < 4; i++) {
                Arrays.fill(q1[i], 0.0);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    q1[i][j] = qTable[state][i][j];
                }
            }
        }

        // Если агент 2 уже в цели (6)
        if (pos2 == 6) {
            for (int i = 0; i < 4; i++) {
                Arrays.fill(q2[i], 0.0);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    q2[i][j] = otherAgent.qTable[state][i][j];
                }
            }
        }

        return NashSolver.solve(q1, q2);
    }

    public double getEpsilon() {
        return epsilon;
    }
}