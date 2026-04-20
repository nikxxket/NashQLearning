import java.util.ArrayList;
import java.util.List;

public class NashSolver {

    public static class NashEquilibrium {
        public final double value1;
        public final double value2;
        public final int action1;
        public final int action2;
        public final double[] strategy1;
        public final double[] strategy2;
        public final boolean isPure;

        public NashEquilibrium(double v1, double v2, int a1, int a2) {
            this.value1 = v1;
            this.value2 = v2;
            this.action1 = a1;
            this.action2 = a2;
            this.strategy1 = new double[4];
            this.strategy2 = new double[4];
            this.strategy1[a1] = 1.0;
            this.strategy2[a2] = 1.0;
            this.isPure = true;
        }

        public NashEquilibrium(double v1, double v2, double[] s1, double[] s2) {
            this.value1 = v1;
            this.value2 = v2;
            this.action1 = -1;
            this.action2 = -1;
            this.strategy1 = s1.clone();
            this.strategy2 = s2.clone();
            this.isPure = false;
        }
    }

    public static NashEquilibrium solve(double[][] q1, double[][] q2) {
        // Проверяем, все ли Q равны нулю
        boolean allZero = true;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (Math.abs(q1[i][j]) > 1e-6 || Math.abs(q2[i][j]) > 1e-6) {
                    allZero = false;
                    break;
                }
            }
        }

        if (allZero) {
            double[] uniform = {0.25, 0.25, 0.25, 0.25};
            return new NashEquilibrium(0, 0, uniform, uniform);
        }

        // Ищем чистые равновесия
        List<NashEquilibrium> pureEquilibria = new ArrayList<>();
        double bestSum = Double.NEGATIVE_INFINITY;
        NashEquilibrium bestEq = null;

        for (int a1 = 0; a1 < 4; a1++) {
            for (int a2 = 0; a2 < 4; a2++) {
                if (isPureNash(q1, q2, a1, a2)) {
                    NashEquilibrium eq = new NashEquilibrium(q1[a1][a2], q2[a1][a2], a1, a2);
                    pureEquilibria.add(eq);
                    double sum = q1[a1][a2] + q2[a1][a2];
                    if (sum > bestSum) {
                        bestSum = sum;
                        bestEq = eq;
                    }
                }
            }
        }

        if (bestEq != null) {
            return bestEq;
        }

        // Если чистых нет - ищем смешанное (maxmin)
        double[] maxmin1 = maxminStrategy(q1, true);
        double[] maxmin2 = maxminStrategy(transpose(q2), false);

        double v1 = expectedValue(q1, maxmin1, maxmin2);
        double v2 = expectedValue(q2, maxmin1, maxmin2);

        return new NashEquilibrium(v1, v2, maxmin1, maxmin2);
    }

    private static boolean isPureNash(double[][] q1, double[][] q2, int a1, int a2) {
        for (int a1p = 0; a1p < 4; a1p++) {
            if (q1[a1p][a2] > q1[a1][a2] + 1e-6) {
                return false;
            }
        }
        for (int a2p = 0; a2p < 4; a2p++) {
            if (q2[a1][a2p] > q2[a1][a2] + 1e-6) {
                return false;
            }
        }
        return true;
    }

    private static double[] maxminStrategy(double[][] matrix, boolean isRowPlayer) {
        int n = matrix.length;
        int m = matrix[0].length;
        double[] strategy = new double[isRowPlayer ? n : m];

        if (isRowPlayer) {
            double maxMin = Double.NEGATIVE_INFINITY;
            List<Integer> bestActions = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                double min = Double.POSITIVE_INFINITY;
                for (int j = 0; j < m; j++) {
                    min = Math.min(min, matrix[i][j]);
                }
                if (min > maxMin + 1e-6) {
                    maxMin = min;
                    bestActions.clear();
                    bestActions.add(i);
                } else if (Math.abs(min - maxMin) < 1e-6) {
                    bestActions.add(i);
                }
            }
            double prob = 1.0 / bestActions.size();
            for (int action : bestActions) {
                strategy[action] = prob;
            }
        } else {
            double maxMin = Double.NEGATIVE_INFINITY;
            List<Integer> bestActions = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                double min = Double.POSITIVE_INFINITY;
                for (int i = 0; i < n; i++) {
                    min = Math.min(min, matrix[i][j]);
                }
                if (min > maxMin + 1e-6) {
                    maxMin = min;
                    bestActions.clear();
                    bestActions.add(j);
                } else if (Math.abs(min - maxMin) < 1e-6) {
                    bestActions.add(j);
                }
            }
            double prob = 1.0 / bestActions.size();
            for (int action : bestActions) {
                strategy[action] = prob;
            }
        }
        return strategy;
    }

    private static double[][] transpose(double[][] matrix) {
        int n = matrix.length;
        int m = matrix[0].length;
        double[][] transposed = new double[m][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }

    private static double expectedValue(double[][] matrix, double[] s1, double[] s2) {
        double value = 0;
        for (int i = 0; i < s1.length; i++) {
            for (int j = 0; j < s2.length; j++) {
                value += s1[i] * s2[j] * matrix[i][j];
            }
        }
        return value;
    }
}