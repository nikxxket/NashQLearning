import javax.swing.*;
import java.awt.*;

public class GridRenderer extends JPanel {
    private static final int CELL_SIZE = 100;
    private static final Color AGENT1_COLOR = new Color(255, 100, 100);
    private static final Color AGENT2_COLOR = new Color(100, 100, 255);
    private static final Color GOAL_COLOR = new Color(100, 255, 100);
    private static final Color SHARED_GOAL_COLOR = new Color(255, 255, 100);
    private static final Color GRID_COLOR = Color.BLACK;
    private static final Color TEXT_COLOR = Color.WHITE;

    private int[] agentPositions;
    private int[] goals;
    private boolean isGame1;
    private boolean agent1ReachedGoal;
    private boolean agent2ReachedGoal;

    public GridRenderer() {
        setPreferredSize(new Dimension(CELL_SIZE * 3, CELL_SIZE * 3));
        setBackground(Color.WHITE);
    }

    public void updateState(int[] positions, int[] goals, boolean isGame1,
                            boolean a1Goal, boolean a2Goal) {
        this.agentPositions = positions.clone();
        this.goals = goals.clone();
        this.isGame1 = isGame1;
        this.agent1ReachedGoal = a1Goal;
        this.agent2ReachedGoal = a2Goal;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем сетку
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(2));

        for (int i = 0; i <= 3; i++) {
            g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, CELL_SIZE * 3);
            g2d.drawLine(0, i * CELL_SIZE, CELL_SIZE * 3, i * CELL_SIZE);
        }

        // Рисуем цели
        if (goals != null) {
            for (int i = 0; i < goals.length; i++) {
                int[] coords = GridWorld.getCellCoords()[goals[i]];
                int x = coords[0] * CELL_SIZE;
                int y = (2 - coords[1]) * CELL_SIZE;

                if (!isGame1 && i == 1) continue; // В игре 2 цель одна

                if (isGame1) {
                    g2d.setColor(GOAL_COLOR);
                } else {
                    g2d.setColor(SHARED_GOAL_COLOR);
                }
                g2d.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);

                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                String text = isGame1 ? "Цель " + (i + 1) : "Цель";
                FontMetrics fm = g2d.getFontMetrics();
                int textX = x + (CELL_SIZE - fm.stringWidth(text)) / 2;
                int textY = y + (CELL_SIZE + fm.getAscent()) / 2;
                g2d.drawString(text, textX, textY);
            }
        }

        // Рисуем агентов
        if (agentPositions != null) {
            // Агент 1
            int[] coords1 = GridWorld.getCellCoords()[agentPositions[0]];
            int x1 = coords1[0] * CELL_SIZE;
            int y1 = (2 - coords1[1]) * CELL_SIZE;

            g2d.setColor(AGENT1_COLOR);
            g2d.fillOval(x1 + 15, y1 + 15, CELL_SIZE - 30, CELL_SIZE - 30);
            g2d.setColor(TEXT_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g2d.getFontMetrics();
            String text1 = "1";
            g2d.drawString(text1, x1 + (CELL_SIZE - fm.stringWidth(text1)) / 2,
                    y1 + (CELL_SIZE + fm.getAscent()) / 2);

            // Агент 2
            int[] coords2 = GridWorld.getCellCoords()[agentPositions[1]];
            int x2 = coords2[0] * CELL_SIZE;
            int y2 = (2 - coords2[1]) * CELL_SIZE;

            g2d.setColor(AGENT2_COLOR);
            g2d.fillOval(x2 + 15, y2 + 15, CELL_SIZE - 30, CELL_SIZE - 30);
            g2d.setColor(TEXT_COLOR);
            String text2 = "2";
            g2d.drawString(text2, x2 + (CELL_SIZE - fm.stringWidth(text2)) / 2,
                    y2 + (CELL_SIZE + fm.getAscent()) / 2);
        }

        // Нумерация ячеек
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i < 9; i++) {
            int[] coords = GridWorld.getCellCoords()[i];
            int x = coords[0] * CELL_SIZE + 5;
            int y = (2 - coords[1]) * CELL_SIZE + 15;
            g2d.drawString(String.valueOf(i), x, y);
        }
    }
}