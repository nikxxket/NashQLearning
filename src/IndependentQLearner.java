import java.util.*;

public class IndependentQLearner {
    private double[][] qTable;
    private double alpha;
    private double gamma;
    private double epsilon;
    private double epsilonDecay;
    private double minEpsilon;
    private Random random;

    private static final int MAX_STATES = 81;

    public IndependentQLearner(double alpha, double gamma, double epsilon,
                               double epsilonDecay, double minEpsilon) {
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.epsilonDecay = epsilonDecay;
        this.minEpsilon = minEpsilon;
        this.qTable = new double[MAX_STATES][4];
        this.random = new Random();

        // Оптимистичная инициализация
        for (int s = 0; s < MAX_STATES; s++) {
            for (int a = 0; a < 4; a++) {
                qTable[s][a] = 1.0;
            }
        }
    }

    public GridWorld.Action selectAction(int state, boolean training) {
        int safeState = Math.min(state, MAX_STATES - 1);

        if (training && random.nextDouble() < epsilon) {
            return GridWorld.Action.values()[random.nextInt(4)];
        } else {
            double maxQ = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                if (qTable[safeState][i] > maxQ) {
                    maxQ = qTable[safeState][i];
                }
            }

            List<Integer> bestActions = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                if (Math.abs(qTable[safeState][i] - maxQ) < 1e-6) {
                    bestActions.add(i);
                }
            }

            if (bestActions.size() == 4 && maxQ == 0) {
                return GridWorld.Action.values()[random.nextInt(4)];
            }

            int randomIndex = random.nextInt(bestActions.size());
            return GridWorld.Action.fromValue(bestActions.get(randomIndex));
        }
    }

    public void learn(int state, GridWorld.Action action, double reward, int nextState, boolean done) {
        int safeState = Math.min(state, MAX_STATES - 1);
        int safeNextState = Math.min(nextState, MAX_STATES - 1);
        int actionIdx = action.getValue();

        double maxNextQ = 0.0;
        if (!done) {
            maxNextQ = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                maxNextQ = Math.max(maxNextQ, qTable[safeNextState][i]);
            }
        }

        double target = reward + gamma * maxNextQ;
        qTable[safeState][actionIdx] += alpha * (target - qTable[safeState][actionIdx]);
    }

    public void decayEpsilon() {
        epsilon = Math.max(minEpsilon, epsilon * epsilonDecay);
    }

    public double getEpsilon() {
        return epsilon;
    }
}