import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Лабораторная работа №3. Базовые растровые алгоритмы. */
public class RasterLab3 extends JFrame {
    private final RasterPanel rasterPanel = new RasterPanel();
    private final JComboBox<String> algorithm = new JComboBox<>(new String[]{
            "Пошаговый алгоритм", "ЦДА", "Брезенхем (отрезок)", "Брезенхем (окружность)"});
    private final JTextField x1Field = new JTextField("-8", 4);
    private final JTextField y1Field = new JTextField("-4", 4);
    private final JTextField x2Field = new JTextField("9", 4);
    private final JTextField y2Field = new JTextField("6", 4);
    private final JTextField cxField = new JTextField("0", 4);
    private final JTextField cyField = new JTextField("0", 4);
    private final JTextField radiusField = new JTextField("8", 4);
    private final JLabel timeLabel = new JLabel("Время: —");
    private final JLabel countLabel = new JLabel("Пикселей: —");
    private final JLabel infoLabel = new JLabel("Выберите алгоритм и нажмите «Построить».");
    private final JPanel lineInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    private final JPanel circleInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

    public RasterLab3() {
        super("Лабораторная работа №3 — Базовые растровые алгоритмы");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setLayout(new BorderLayout(8, 8));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        top.add(new JLabel("Алгоритм:"));
        algorithm.setSelectedIndex(2);
        top.add(algorithm);
        JButton buildButton = new JButton("Построить");
        JButton clearButton = new JButton("Очистить");
        JButton zoomIn = new JButton("Масштаб +");
        JButton zoomOut = new JButton("Масштаб −");
        top.add(buildButton);
        top.add(clearButton);
        top.add(zoomIn);
        top.add(zoomOut);
        controls.add(top);

        lineInputs.add(new JLabel("Отрезок: x1")); lineInputs.add(x1Field);
        lineInputs.add(new JLabel("y1")); lineInputs.add(y1Field);
        lineInputs.add(new JLabel("x2")); lineInputs.add(x2Field);
        lineInputs.add(new JLabel("y2")); lineInputs.add(y2Field);
        circleInputs.add(new JLabel("Окружность: центр X0")); circleInputs.add(cxField);
        circleInputs.add(new JLabel("Y0")); circleInputs.add(cyField);
        circleInputs.add(new JLabel("радиус R")); circleInputs.add(radiusField);
        controls.add(lineInputs);
        controls.add(circleInputs);
        JPanel results = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2));
        results.add(timeLabel); results.add(countLabel);
        controls.add(results);
        controls.add(infoLabel);

        add(controls, BorderLayout.NORTH);
        add(rasterPanel, BorderLayout.CENTER);

        algorithm.addActionListener(e -> updateInputVisibility());
        buildButton.addActionListener(e -> build());
        clearButton.addActionListener(e -> {
            rasterPanel.setPixels(Collections.emptyList());
            timeLabel.setText("Время: —"); countLabel.setText("Пикселей: —");
            infoLabel.setText("Поле очищено.");
        });
        zoomIn.addActionListener(e -> rasterPanel.changeScale(1.25));
        zoomOut.addActionListener(e -> rasterPanel.changeScale(0.8));
        updateInputVisibility();
        setLocationRelativeTo(null);
    }

    private void updateInputVisibility() {
        boolean circle = algorithm.getSelectedIndex() == 3;
        lineInputs.setVisible(!circle);
        circleInputs.setVisible(circle);
        revalidate();
    }

    private void build() {
        try {
            int type = algorithm.getSelectedIndex();
            List<Point> pixels = new ArrayList<>();
            long start = System.nanoTime();
            if (type == 3) {
                int cx = readInt(cxField, "X0");
                int cy = readInt(cyField, "Y0");
                int r = readInt(radiusField, "Радиус");
                if (r < 0 || r > 1000) throw new IllegalArgumentException("Радиус должен быть от 0 до 1000.");
                bresenhamCircle(cx, cy, r, pixels);
            } else {
                int x1 = readInt(x1Field, "x1"), y1 = readInt(y1Field, "y1");
                int x2 = readInt(x2Field, "x2"), y2 = readInt(y2Field, "y2");
                if (Math.abs((long)x2-x1) > 2000 || Math.abs((long)y2-y1) > 2000)
                    throw new IllegalArgumentException("Разность координат не должна превышать 2000.");
                switch (type) {
                    case 0: stepByStep(x1, y1, x2, y2, pixels); break;
                    case 1: dda(x1, y1, x2, y2, pixels); break;
                    case 2: bresenhamLine(x1, y1, x2, y2, pixels); break;
                }
            }
            long elapsed = System.nanoTime() - start;
            rasterPanel.setPixels(pixels);
            timeLabel.setText(String.format(Locale.US, "Время: %,d нс (%.3f мс)", elapsed, elapsed / 1_000_000.0));
            countLabel.setText("Пикселей: " + pixels.size());
            infoLabel.setText("Координаты точек — целые числа; один квадрат сетки соответствует одному пикселю.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введите целые числа во все поля.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int readInt(JTextField field, String name) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { throw new NumberFormatException(name); }
    }

    // 1. Пошаговый алгоритм: вычисляем координату по уравнению прямой и округляем.
    // Для крутых линий меняем роли X и Y, чтобы не пропускать пиксели.
    static void stepByStep(int x1, int y1, int x2, int y2, List<Point> out) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { addUnique(out, x1, y1); return; }
        if (Math.abs(dx) >= Math.abs(dy)) {
            double k = (double) dy / dx;
            int direction = dx > 0 ? 1 : -1;
            for (int x = x1; ; x += direction) {
                double y = y1 + k * (x - x1);
                addUnique(out, x, (int)Math.round(y));
                if (x == x2) break;
            }
        } else {
            double k = (double) dx / dy;
            int direction = dy > 0 ? 1 : -1;
            for (int y = y1; ; y += direction) {
                double x = x1 + k * (y - y1);
                addUnique(out, (int)Math.round(x), y);
                if (y == y2) break;
            }
        }
    }

    // 2. ЦДА: число шагов равно максимуму приращений; на каждом шаге
    // прибавляем постоянные dx/steps и dy/steps, затем округляем координаты.
    static void dda(int x1, int y1, int x2, int y2, List<Point> out) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { addUnique(out, x1, y1); return; }
        double x = x1, y = y1;
        double xStep = (double) dx / steps;
        double yStep = (double) dy / steps;
        for (int i = 0; i <= steps; i++) {
            addUnique(out, (int)Math.round(x), (int)Math.round(y));
            x += xStep;
            y += yStep;
        }
    }

    // 3. Целочисленный алгоритм Брезенхема для всех октантов.
    // err — накопленная ошибка; решение принимается без вещественных чисел.
    static void bresenhamLine(int x1, int y1, int x2, int y2, List<Point> out) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            addUnique(out, x1, y1);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    // 4. Брезенхем для окружности. Строим дугу и сразу добавляем
    // семь симметричных точек — всего восемь октантов.
    static void bresenhamCircle(int cx, int cy, int r, List<Point> out) {
        int x = 0, y = r;
        int e = 3 - 2 * r;
        Set<Point> seen = new HashSet<>();
        while (x <= y) {
            addCircleSymmetry(cx, cy, x, y, out, seen);
            if (e >= 0) {
                e += 4 * (x - y) + 10;
                y--;
            } else {
                e += 4 * x + 6;
            }
            x++;
        }
    }

    private static void addCircleSymmetry(int cx, int cy, int x, int y, List<Point> out, Set<Point> seen) {
        int[][] points = {
                {cx + x, cy + y}, {cx + x, cy - y},
                {cx - x, cy + y}, {cx - x, cy - y},
                {cx + y, cy + x}, {cx + y, cy - x},
                {cx - y, cy + x}, {cx - y, cy - x}
        };
        for (int[] point : points) {
            Point p = new Point(point[0], point[1]);
            if (seen.add(p)) out.add(p);
        }
    }

    private static void addUnique(List<Point> out, int x, int y) {
        Point p = new Point(x, y);
        if (out.isEmpty() || !out.get(out.size() - 1).equals(p)) out.add(p);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RasterLab3().setVisible(true));
    }

    static class RasterPanel extends JPanel {
        private List<Point> pixels = Collections.emptyList();
        private double scale = 28.0;
        private final int margin = 45;
        RasterPanel() { setBackground(Color.WHITE); setPreferredSize(new Dimension(800, 550)); }
        void setPixels(List<Point> p) { pixels = new ArrayList<>(p); repaint(); }
        void changeScale(double factor) { scale = Math.max(8, Math.min(80, scale * factor)); repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int ox = getWidth() / 2, oy = getHeight() / 2;
            double s = scale;
            // Сетка. Координатная плоскость центрирована в окне.
            g2.setColor(new Color(230, 234, 239));
            int firstX = (int)Math.floor((-ox) / s) - 1;
            int lastX = (int)Math.ceil((getWidth() - ox) / s) + 1;
            for (int x = firstX; x <= lastX; x++) {
                int sx = ox + (int)Math.round(x * s);
                g2.drawLine(sx, 0, sx, getHeight());
            }
            int firstY = (int)Math.floor((oy - getHeight()) / s) - 1;
            int lastY = (int)Math.ceil(oy / s) + 1;
            for (int y = firstY; y <= lastY; y++) {
                int sy = oy - (int)Math.round(y * s);
                g2.drawLine(0, sy, getWidth(), sy);
            }
            // Оси координат.
            g2.setColor(new Color(75, 83, 95)); g2.setStroke(new BasicStroke(1.6f));
            g2.drawLine(0, oy, getWidth(), oy);
            g2.drawLine(ox, 0, ox, getHeight());
            g2.drawString("X", getWidth() - 18, oy - 8); g2.drawString("Y", ox + 8, 16);
            g2.setFont(getFont().deriveFont(10f)); g2.setColor(new Color(90, 98, 110));
            for (int x = firstX; x <= lastX; x++) if (x != 0) {
                int sx = ox + (int)Math.round(x * s);
                g2.drawString(Integer.toString(x), sx + 2, oy + 13);
            }
            for (int y = firstY; y <= lastY; y++) if (y != 0) {
                int sy = oy - (int)Math.round(y * s);
                g2.drawString(Integer.toString(y), ox + 5, sy - 2);
            }
            g2.drawString("0", ox + 5, oy + 13);
            // Каждый вычисленный целочисленный координатный узел — отдельный квадрат-пиксель.
            g2.setColor(new Color(45, 105, 190));
            int cell = Math.max(3, (int)Math.round(s) - 2);
            for (Point p : pixels) {
                int sx = ox + (int)Math.round(p.x * s);
                int sy = oy - (int)Math.round(p.y * s);
                g2.fillRect(sx - cell / 2, sy - cell / 2, cell, cell);
            }
            if (!pixels.isEmpty()) {
                g2.setColor(new Color(25, 65, 125));
                Point first = pixels.get(0), last = pixels.get(pixels.size() - 1);
                g2.drawString("Начало " + first.x + "," + first.y,
                        ox + (int)Math.round(first.x * s) + 7, oy - (int)Math.round(first.y * s) - 7);
                if (pixels.size() > 1) g2.drawString("Конец " + last.x + "," + last.y,
                        ox + (int)Math.round(last.x * s) + 7, oy - (int)Math.round(last.y * s) + 16);
            }
            g2.dispose();
        }
    }
}
