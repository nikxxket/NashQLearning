import java.util.Random;

public class GridWorld {
    public enum Action {
        UP(0), DOWN(1), LEFT(2), RIGHT(3);

        private final int value;
        Action(int value) { this.value = value; }
        public int getValue() { return value; }
        public static Action fromValue(int value) {
            for (Action a : values()) {
                if (a.value == value) return a;
            }
            return UP;
        }
    }

    private int[] pos;
    private int[] prevPos;
    private int[] goals;
    private boolean gameOver;
    private boolean stochastic;
    private Random random;
    private boolean isGame1;

    private boolean agent1ReachedGoal;
    private boolean agent2ReachedGoal;

    public static final double STEP_PENALTY = -0.05;      // штраф за каждый шаг (только активным)
    public static final double IDLE_PENALTY = -0.1;       // штраф за бездействие (если агент не сдвинулся)

    private static final int[][] CELL_COORDS = {
            {0, 0}, {1, 0}, {2, 0},
            {0, 1}, {1, 1}, {2, 1},
            {0, 2}, {1, 2}, {2, 2}
    };

    public GridWorld(boolean stochastic, boolean isGame1) {
        this.stochastic = stochastic;
        this.isGame1 = isGame1;
        this.random = new Random();

        if (isGame1) {
            goals = new int[]{8, 6};
        } else {
            goals = new int[]{7, 7};
        }

        reset();
    }

    public void reset() {
        pos = new int[]{0, 2};
        prevPos = new int[]{0, 2};
        gameOver = false;
        agent1ReachedGoal = false;
        agent2ReachedGoal = false;
    }

    public double[] step(Action a1, Action a2) {
        if (gameOver) return new double[]{0, 0};

        int oldPos1 = pos[0];
        int oldPos2 = pos[1];

        boolean active1 = !agent1ReachedGoal;
        boolean active2 = !agent2ReachedGoal;

        int[] intended = new int[2];
        intended[0] = active1 ? move(pos[0], a1, 0) : pos[0];
        intended[1] = active2 ? move(pos[1], a2, 1) : pos[1];

        // Стохастичность
        if (stochastic) {
            if (active1 && (pos[0] == 0 || pos[0] == 2) && a1 == Action.UP) {
                if (random.nextDouble() < 0.5) intended[0] = pos[0];
            }
            if (active2 && (pos[1] == 0 || pos[1] == 2) && a2 == Action.UP) {
                if (random.nextDouble() < 0.5) intended[1] = pos[1];
            }
        }

        double[] rewards = new double[2];
        boolean collision = false;

        if (active1 && active2 && intended[0] == intended[1]) {
            boolean isGoalForAny = (intended[0] == goals[0] || intended[0] == goals[1]);
            if (!isGame1 && intended[0] == 7) isGoalForAny = true;
            if (!isGoalForAny) collision = true;
        }

        if (collision) {
            rewards[0] = -1;
            rewards[1] = -1;
        } else {
            if (active1) pos[0] = intended[0];
            if (active2) pos[1] = intended[1];

            if (isGame1) {
                // Игра 1: индивидуальные цели
                if (active1 && pos[0] == goals[0]) {
                    rewards[0] = 100;
                    agent1ReachedGoal = true;
                    System.out.println("Агент 1 достиг цели! Позиция: " + pos[0]);
                }
                if (active2 && pos[1] == goals[1]) {
                    rewards[1] = 100;
                    agent2ReachedGoal = true;
                    System.out.println("Агент 2 достиг цели! Позиция: " + pos[1]);
                }
                if (agent1ReachedGoal && agent2ReachedGoal) gameOver = true;
            } else {
                // Игра 2: общая цель — награда только когда оба в клетке 7
                if (active1 && pos[0] == 7) agent1ReachedGoal = true;
                if (active2 && pos[1] == 7) agent2ReachedGoal = true;

                if (agent1ReachedGoal && agent2ReachedGoal) {
                    rewards[0] = 100;
                    rewards[1] = 100;
                    gameOver = true;
                    System.out.println("Оба агента достигли общей цели!");
                }
            }

            // Штраф за бездействие (только активным и если не столкнулись)
            if (active1 && pos[0] == oldPos1 && !collision) rewards[0] += IDLE_PENALTY;
            if (active2 && pos[1] == oldPos2 && !collision) rewards[1] += IDLE_PENALTY;

            // Шаговый штраф — только активным и если игра не завершена
            if (!gameOver) {
                if (active1) rewards[0] += STEP_PENALTY;
                if (active2) rewards[1] += STEP_PENALTY;
            }
        }

        return rewards;
    }

    private int move(int cell, Action a, int agentIndex) {
        int[] coords = CELL_COORDS[cell];
        int x = coords[0];
        int y = coords[1];
        int goal = goals[agentIndex];

        // Нельзя вверх из верхнего ряда, если это не вход в цель
        if (a == Action.UP && y == 2) {
            boolean allowed = false;
            if (isGame1) {
                if ((goal == 8 && cell == 5) || (goal == 6 && cell == 3)) allowed = true;
            } else {
                if (goal == 7 && cell == 4) allowed = true;
            }
            if (!allowed) return cell;
        }

        switch (a) {
            case UP:    y = Math.min(y + 1, 2); break;
            case DOWN:  y = Math.max(y - 1, 0); break;
            case LEFT:  x = Math.max(x - 1, 0); break;
            case RIGHT: x = Math.min(x + 1, 2); break;
        }

        int next = y * 3 + x;

        // Вход в цель только с разрешённой клетки
        if (next == goal) {
            boolean allowed = false;
            if (isGame1) {
                if (goal == 8 && cell == 5) allowed = true;
                if (goal == 6 && cell == 3) allowed = true;
            } else {
                if (goal == 7 && cell == 4) allowed = true;
            }
            if (!allowed) return cell;
        }

        // Запрет входа в чужую цель в Игре 1
        if (isGame1) {
            if (agentIndex == 0 && next == 6) return cell;
            if (agentIndex == 1 && next == 8) return cell;
        }

        return next;
    }

    // Геттеры
    public int getState() { return pos[0] * 9 + pos[1]; }
    public int[] getPositions() { return pos.clone(); }
    public int[] getGoals() { return goals.clone(); }
    public boolean isGameOver() { return gameOver; }
    public boolean isStochastic() { return stochastic; }
    public boolean isGame1() { return isGame1; }
    public boolean isAgent1ReachedGoal() { return agent1ReachedGoal; }
    public boolean isAgent2ReachedGoal() { return agent2ReachedGoal; }
    public static int[][] getCellCoords() { return CELL_COORDS; }
}