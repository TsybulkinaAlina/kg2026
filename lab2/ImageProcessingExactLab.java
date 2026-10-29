import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/**
 * Лабораторная работа — Фильтры, Пороговая обработка и Сжатие (RLE)
 */
public class ImageProcessingExactLab extends JFrame {

    private BufferedImage originalImage;
    private BufferedImage processedImage;

    private JLabel originalLabel;
    private JLabel processedLabel;
    private JLabel statusLabel;

    private JComboBox<String> algorithmBox;
    private JSlider paramSlider;
    private JLabel sliderLabel;
    private JButton applyButton;
    private JButton resetButton;
    private JButton compressButton;

    public ImageProcessingExactLab() {
        setTitle("Лабораторная работа — Пороговая обработка, Порядковые фильтры и Сжатие");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- ДВУХСТРОЧНАЯ ПАНЕЛЬ УПРАВЛЕНИЯ (чтобы все кнопки 100% помещались) ---
        JPanel mainControlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        mainControlPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Строка 1: Загрузка и Выбор метода
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton loadBtn = new JButton("Загрузить изображение");
        loadBtn.addActionListener(e -> loadImage());

        algorithmBox = new JComboBox<>(new String[]{
                "1. Глобальный порог: Ручной (Метод 1)",
                "2. Глобальный порог: Метод Оцу (Метод 2)",
                "3. Порядковая статистика: Медианный фильтр",
                "4. Порядковая статистика: Фильтр минимума",
                "5. Порядковая статистика: Фильтр максимума"
        });
        algorithmBox.addActionListener(e -> updateControlsState());

        row1.add(loadBtn);
        row1.add(new JLabel("Выбор метода:"));
        row1.add(algorithmBox);

        // Строка 2: Настройка параметров, Запуск обработки и СЖАТИЕ RLE
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sliderLabel = new JLabel("Порог (0-255):");
        paramSlider = new JSlider(0, 255, 128);
        paramSlider.setPreferredSize(new Dimension(180, 40));

        applyButton = new JButton("Применить обработку");
        applyButton.addActionListener(e -> processImage());

        resetButton = new JButton("Сбросить");
        resetButton.addActionListener(e -> resetImage());

        // ЯРКАЯ КНОПКА СЖАТИЯ RLE
        compressButton = new JButton("⚡ СЖАТИЕ ИЗ ЛЕКЦИЙ (RLE)");
        compressButton.setFont(new Font("Arial", Font.BOLD, 12));
        compressButton.setBackground(new Color(180, 230, 180));
        compressButton.setForeground(Color.BLACK);
        compressButton.addActionListener(e -> testRLECompression());

        row2.add(sliderLabel);
        row2.add(paramSlider);
        row2.add(applyButton);
        row2.add(resetButton);
        row2.add(compressButton); // Теперь кнопка гарантированно на виду!

        mainControlPanel.add(row1);
        mainControlPanel.add(row2);

        add(mainControlPanel, BorderLayout.NORTH);

        // --- Панель предпросмотра изображений ---
        JPanel imagePanel = new JPanel(new GridLayout(1, 2, 10, 10));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        originalLabel = new JLabel("Оригинал не загружен", SwingConstants.CENTER);
        processedLabel = new JLabel("Результат обработки / Сжатия", SwingConstants.CENTER);

        originalLabel.setBorder(BorderFactory.createTitledBorder("Исходное изображение"));
        processedLabel.setBorder(BorderFactory.createTitledBorder("Обработанное изображение"));

        imagePanel.add(new JScrollPane(originalLabel));
        imagePanel.add(new JScrollPane(processedLabel));

        add(imagePanel, BorderLayout.CENTER);

        // --- Строка состояния ---
        statusLabel = new JLabel(" Готово к работе. Загрузите изображение.");
        statusLabel.setPreferredSize(new Dimension(getWidth(), 25));
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        add(statusLabel, BorderLayout.SOUTH);

        updateControlsState();
        setLocationRelativeTo(null);
    }

    private void updateControlsState() {
        int index = algorithmBox.getSelectedIndex();
        if (index == 0) { // Ручной порог
            sliderLabel.setText("Порог (0-255):");
            paramSlider.setMinimum(0);
            paramSlider.setMaximum(255);
            paramSlider.setValue(128);
            sliderLabel.setEnabled(true);
            paramSlider.setEnabled(true);
        } else if (index == 1) { // Метод Оцу
            sliderLabel.setText("Автопорог (Оцу)");
            sliderLabel.setEnabled(false);
            paramSlider.setEnabled(false);
        } else { // Фильтры порядковых статистик
            sliderLabel.setText("Окно (3-15):");
            paramSlider.setMinimum(3);
            paramSlider.setMaximum(15);
            paramSlider.setValue(3);
            sliderLabel.setEnabled(true);
            paramSlider.setEnabled(true);
        }
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                originalImage = ImageIO.read(file);
                if (originalImage != null) {
                    processedImage = copyImage(originalImage);
                    displayImage(originalImage, originalLabel);
                    displayImage(processedImage, processedLabel);
                    statusLabel.setText(" Загружено: " + file.getName() + " [" + originalImage.getWidth() + "x" + originalImage.getHeight() + "]");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resetImage() {
        if (originalImage != null) {
            processedImage = copyImage(originalImage);
            displayImage(processedImage, processedLabel);
            statusLabel.setText(" Изображение сброшено к оригиналу.");
        }
    }

    private void displayImage(BufferedImage img, JLabel label) {
        if (img == null) return;
        label.setIcon(new ImageIcon(img));
        label.setText("");
    }

    private void processImage() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Загрузите изображение!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int index = algorithmBox.getSelectedIndex();
        long startTime = System.currentTimeMillis();

        switch (index) {
            case 0: // Ручной порог
                int thresh = paramSlider.getValue();
                processedImage = applyFixedThreshold(originalImage, thresh);
                statusLabel.setText(String.format(" Пороговая обработка (Ручная T=%d) (%d мс)", thresh, (System.currentTimeMillis() - startTime)));
                break;

            case 1: // Метод Оцу
                int otsuThresh = computeOtsuThreshold(originalImage);
                processedImage = applyFixedThreshold(originalImage, otsuThresh);
                statusLabel.setText(String.format(" Пороговая обработка (Метод Оцу T=%d) (%d мс)", otsuThresh, (System.currentTimeMillis() - startTime)));
                break;

            case 2: // Медианный
            case 3: // Минимум
            case 4: // Максимум
                int windowSize = paramSlider.getValue();
                if (windowSize % 2 == 0) windowSize++;
                FilterType type = (index == 2) ? FilterType.MEDIAN : (index == 3 ? FilterType.MIN : FilterType.MAX);
                processedImage = applyOrderStatisticFilter(originalImage, windowSize, type);
                statusLabel.setText(String.format(" Фильтр %s (окно %dx%d) (%d мс)", type, windowSize, windowSize, (System.currentTimeMillis() - startTime)));
                break;
        }

        displayImage(processedImage, processedLabel);
    }

    // =========================================================================
    // АЛГОРИТМ СЖАТИЯ ИЗ ЛЕКЦИЙ: RLE (Run-Length Encoding)
    // =========================================================================
    private void testRLECompression() {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Сначала загрузите и обработайте изображение!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int width = processedImage.getWidth();
        int height = processedImage.getHeight();
        byte[] rawData = new byte[width * height];
        int idx = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = processedImage.getRGB(x, y);
                rawData[idx++] = (byte) ((rgb >> 16) & 0xFF);
            }
        }

        java.io.ByteArrayOutputStream compressedStream = new java.io.ByteArrayOutputStream();
        try {
            if (rawData.length > 0) {
                byte currentVal = rawData[0];
                int runLength = 1;
                for (int i = 1; i < rawData.length; i++) {
                    if (rawData[i] == currentVal && runLength < 255) {
                        runLength++;
                    } else {
                        compressedStream.write(runLength);
                        compressedStream.write(currentVal);
                        currentVal = rawData[i];
                        runLength = 1;
                    }
                }
                compressedStream.write(runLength);
                compressedStream.write(currentVal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] compressedData = compressedStream.toByteArray();
        double origSize = rawData.length;
        double compSize = compressedData.length;
        double compressionRatio = origSize / compSize;
        double spaceSaving = (1.0 - (compSize / origSize)) * 100.0;

        String report = String.format(
                "<html><body style='width: 300px; padding: 5px;'>" +
                        "<h2>Результаты сжатия (RLE):</h2>" +
                        "<b>• Исходный размер:</b> %.0f байт<br>" +
                        "<b>• Размер после сжатия:</b> %.0f байт<br><br>" +
                        "<h3 style='color: green;'>Коэффициент сжатия (К): %.2f</h3>" +
                        "<b>• Экономия памяти:</b> %.2f%%" +
                        "</body></html>",
                origSize, compSize, compressionRatio, spaceSaving
        );

        JOptionPane.showMessageDialog(this, report, "Сжатие изображений (RLE)", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText(String.format(" Сжатие RLE выполнено. Коэффициент сжатия К = %.2f", compressionRatio));
    }

    private int toGrayscale(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    private BufferedImage applyFixedThreshold(BufferedImage src, int threshold) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = toGrayscale(src.getRGB(x, y));
                res.setRGB(x, y, (gray >= threshold) ? 0xFFFFFF : 0x000000);
            }
        }
        return res;
    }

    private int computeOtsuThreshold(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight(), total = w * h;
        int[] hist = new int[256];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                hist[toGrayscale(src.getRGB(x, y))]++;

        float sum = 0;
        for (int t = 0; t < 256; t++) sum += t * hist[t];
        float sumB = 0, varMax = 0;
        int wB = 0, bestThresh = 0;

        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            int wF = total - wB;
            if (wF == 0) break;
            sumB += (float)(t * hist[t]);
            float mB = sumB / wB, mF = (sum - sumB) / wF;
            float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);
            if (varBetween > varMax) { varMax = varBetween; bestThresh = t; }
        }
        return bestThresh;
    }

    enum FilterType { MEDIAN, MIN, MAX }

    private BufferedImage applyOrderStatisticFilter(BufferedImage src, int windowSize, FilterType type) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int radius = windowSize / 2, area = windowSize * windowSize;
        int[] rWin = new int[area];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int count = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int px = clamp(x + dx, 0, w - 1);
                        int py = clamp(y + dy, 0, h - 1);
                        rWin[count++] = (src.getRGB(px, py) >> 16) & 0xFF;
                    }
                }
                Arrays.sort(rWin, 0, count);
                int val = (type == FilterType.MEDIAN) ? rWin[count / 2] : (type == FilterType.MIN ? rWin[0] : rWin[count - 1]);
                res.setRGB(x, y, (val << 16) | (val << 8) | val);
            }
        }
        return res;
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private BufferedImage copyImage(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return copy;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageProcessingExactLab().setVisible(true));
    }
}
