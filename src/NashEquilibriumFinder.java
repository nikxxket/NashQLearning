import java.util.*;

public class NashEquilibriumFinder {

    static class Path {
        List<Integer> positions;
        List<GridWorld.Action> actions;

        Path() {
            positions = new ArrayList<>();
            actions = new ArrayList<>();
        }

        Path(Path other) {
            this.positions = new ArrayList<>(other.positions);
            this.actions = new ArrayList<>(other.actions);
        }

        void add(int pos, GridWorld.Action action) {
            positions.add(pos);
            actions.add(action);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (GridWorld.Action a : actions) {
                if (a != null) {
                    sb.append(a.toString().charAt(0));
                }
            }
            return sb.toString();
        }

        public String toDetailedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Позиции: ");
            for (int i = 0; i < positions.size(); i++) {
                sb.append(positions.get(i));
                if (i < positions.size() - 1) sb.append(" -> ");
            }
            sb.append(" | Действия: ");
            sb.append(toString());
            return sb.toString();
        }
    }

    static class PathPair {
        Path path1;
        Path path2;

        PathPair(Path p1, Path p2) {
            this.path1 = p1;
            this.path2 = p2;
        }
    }

    public static void findAllNashEquilibria() {
        System.out.println("=== Поиск равновесий Нэша в чистых стратегиях для Игры 1 ===\n");
        System.out.println("Правила:");
        System.out.println("- Агент 1 стартует в ячейке 0, цель - ячейка 8");
        System.out.println("- Агент 2 стартует в ячейке 2, цель - ячейка 6");
        System.out.println("- Вход в цель разрешён только снизу (8 из 5, 6 из 3)");
        System.out.println("- Столкновения запрещены\n");

        // Генерируем все кратчайшие пути для обоих агентов
        List<Path> paths1 = generateShortestPaths(0, 8, 0);
        List<Path> paths2 = generateShortestPaths(2, 6, 1);

        System.out.println("Кратчайшие пути для Агента 1 (из 0 в 8 через 5):");
        for (int i = 0; i < paths1.size(); i++) {
            System.out.printf("%2d. %-6s  %s\n", i+1, paths1.get(i),
                    paths1.get(i).toDetailedString());
        }

        System.out.println("\nКратчайшие пути для Агента 2 (из 2 в 6 через 3):");
        for (int i = 0; i < paths2.size(); i++) {
            System.out.printf("%2d. %-6s  %s\n", i+1, paths2.get(i),
                    paths2.get(i).toDetailedString());
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Пары путей, образующие равновесие Нэша:");
        System.out.println("(отсутствие столкновений и одновременное достижение целей)\n");

        List<PathPair> nashPairs = new ArrayList<>();

        for (Path p1 : paths1) {
            for (Path p2 : paths2) {
                if (isNashEquilibrium(p1, p2)) {
                    nashPairs.add(new PathPair(p1, p2));
                }
            }
        }

        // Выводим результаты
        for (int i = 0; i < nashPairs.size(); i++) {
            PathPair pair = nashPairs.get(i);
            System.out.printf("%2d. Агент1: %-6s  Агент2: %-6s\n",
                    i+1, pair.path1, pair.path2);
        }

        System.out.println("\nСимметричные варианты (обмен агентов местами):");
        for (int i = 0; i < nashPairs.size(); i++) {
            PathPair pair = nashPairs.get(i);
            System.out.printf("%2d. Агент1: %-6s  Агент2: %-6s\n",
                    i+1+nashPairs.size(), pair.path2, pair.path1);
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Всего уникальных пар равновесий: " + nashPairs.size());
        System.out.println("Всего с учётом симметрии: " + (nashPairs.size() * 2));

        // Вывод траекторий в виде координат
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Детальное описание равновесных траекторий:\n");

        for (int i = 0; i < nashPairs.size(); i++) {
            PathPair pair = nashPairs.get(i);
            System.out.printf("Пара %d:\n", i+1);
            System.out.printf("  Агент 1: %s\n", getPathDescription(pair.path1));
            System.out.printf("  Агент 2: %s\n\n", getPathDescription(pair.path2));
        }
    }

    private static String getPathDescription(Path path) {
        StringBuilder sb = new StringBuilder();
        List<Integer> pos = path.positions;
        for (int i = 0; i < pos.size(); i++) {
            int cell = pos.get(i);
            int[] coords = GridWorld.getCellCoords()[cell];
            sb.append(String.format("(%d,%d)", coords[0], coords[1]));
            if (i < pos.size() - 1) sb.append(" → ");
        }
        return sb.toString();
    }

    private static List<Path> generateShortestPaths(int start, int goal, int agentIndex) {
        List<Path> allPaths = new ArrayList<>();
        Path initial = new Path();
        initial.add(start, null);

        Set<Integer> visited = new HashSet<>();
        visited.add(start);

        dfs(start, goal, visited, initial, allPaths, 0, agentIndex);

        // Находим минимальную длину пути
        int minLength = Integer.MAX_VALUE;
        for (Path p : allPaths) {
            int length = 0;
            for (GridWorld.Action a : p.actions) {
                if (a != null) length++;
            }
            minLength = Math.min(minLength, length);
        }

        // Оставляем только пути минимальной длины
        List<Path> shortestPaths = new ArrayList<>();
        for (Path p : allPaths) {
            int length = 0;
            for (GridWorld.Action a : p.actions) {
                if (a != null) length++;
            }
            if (length == minLength) {
                shortestPaths.add(p);
            }
        }

        return shortestPaths;
    }

    private static void dfs(int current, int goal, Set<Integer> visited,
                            Path currentPath, List<Path> paths, int depth,
                            int agentIndex) {
        if (depth > 8) return; // Ограничение глубины

        if (current == goal) {
            paths.add(new Path(currentPath));
            return;
        }

        for (GridWorld.Action action : GridWorld.Action.values()) {
            int next = move(current, action, goal, agentIndex);

            // Проверяем, что движение действительно происходит
            if (next != current && !visited.contains(next)) {
                visited.add(next);
                currentPath.add(next, action);

                dfs(next, goal, visited, currentPath, paths, depth + 1, agentIndex);

                // Backtrack
                currentPath.positions.remove(currentPath.positions.size() - 1);
                currentPath.actions.remove(currentPath.actions.size() - 1);
                visited.remove(next);
            }
        }
    }

    private static int move(int cell, GridWorld.Action a, int goal, int agentIndex) {
        int[] coords = GridWorld.getCellCoords()[cell];
        int x = coords[0];
        int y = coords[1];

        switch (a) {
            case UP:    y = Math.min(y + 1, 2); break;
            case DOWN:  y = Math.max(y - 1, 0); break;
            case LEFT:  x = Math.max(x - 1, 0); break;
            case RIGHT: x = Math.min(x + 1, 2); break;
        }

        int next = y * 3 + x;

        // Если следующая позиция - цель, проверяем вход только снизу
        if (next == goal) {
            boolean allowed = false;
            if (goal == 8) {
                allowed = (cell == 5);
            } else if (goal == 6) {
                allowed = (cell == 3);
            }
            if (!allowed) {
                return cell;
            }
        }

        return next;
    }

    private static boolean isNashEquilibrium(Path p1, Path p2) {
        int steps = Math.min(p1.positions.size(), p2.positions.size());

        // Проверяем каждый шаг (кроме последнего - там цели)
        for (int i = 1; i < steps - 1; i++) {
            int pos1 = p1.positions.get(i);
            int pos2 = p2.positions.get(i);

            // Проверка на столкновение (одна и та же ячейка)
            if (pos1 == pos2) {
                return false;
            }

            // Проверка на пересечение (обмен позициями)
            if (i > 0) {
                int prevPos1 = p1.positions.get(i-1);
                int prevPos2 = p2.positions.get(i-1);
                if (pos1 == prevPos2 && pos2 == prevPos1) {
                    return false;
                }
            }
        }

        // Проверяем, что оба достигают целей на последнем шаге
        int lastStep = steps - 1;
        return p1.positions.get(lastStep) == 8 && p2.positions.get(lastStep) == 6;
    }

    public static void main(String[] args) {
        findAllNashEquilibria();
    }
}