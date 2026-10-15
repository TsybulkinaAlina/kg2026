import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Лабораторная работа: Цветовые модели.
 * Четный вариант: CMYK - RGB - HSV.
 *
 * Реализовано:
 * 1. Три способа задания цвета в каждой модели: слайдер, текстовое поле, палитра.
 * 2. Автоматический пересчет во все модели при изменении любой компоненты.
 * 3. Корректные математические преобразования.
 */
public class ColorLab extends JFrame {

    // Флаг для предотвращения бесконечного цикла обновлений
    private boolean isUpdating = false;

    // Компоненты для CMYK
    private JSlider cSlider, mSlider, ySlider, kSlider;
    private JTextField cField, mField, yField, kField;

    // Компоненты для RGB
    private JSlider rSlider, gSlider, bSlider;
    private JTextField rField, gField, bField;

    // Компоненты для HSV
    private JSlider hSlider, sSlider, vSlider;
    private JTextField hField, sField, vField;

    // Панель предпросмотра цвета
    private JPanel colorPreview;

    public ColorLab() {
        setTitle("Лабораторная работа: Цветовые модели (CMYK - RGB - HSV)");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 1. Панель предпросмотра цвета
        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(950, 100));
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        add(colorPreview, BorderLayout.NORTH);

        // 2. Панель с тремя цветовыми моделями
        JPanel modelsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        modelsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        modelsPanel.add(createCMYKPanel());
        modelsPanel.add(createRGBPanel());
        modelsPanel.add(createHSVPanel());
        add(modelsPanel, BorderLayout.CENTER);

        // 3. Инициализация начальным цветом
        updateAllFromRGB(0, 0, 255);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // V (Value / Яркость)
        float v = max;

        // S (Saturation / Насыщенность)
        float s = 0;
        if (max != 0) {
            s = delta / max;
        }

        // H (Hue / Цветовой тон)
        float h = 0;
        if (delta != 0) {
            if (max == rf) {
                h = (gf - bf) / delta + (gf < bf ? 6.0f : 0.0f);
            } else if (max == gf) {
                h = (bf - rf) / delta + 2.0f;
            } else {
                h = (rf - gf) / delta + 4.0f;
            }
            h /= 6.0f; // Переводим в [0, 1]
        }

        // Возвращаем в удобных единицах: H (0-360), S (0-100), V (0-100)
        return new float[]{h * 360.0f, s * 100.0f, v * 100.0f};
    }

    public static int[] hsvToRgb(float h, float s, float v) {
        float hf = h / 360.0f;
        float sf = s / 100.0f;
        float vf = v / 100.0f;

        int i = (int) Math.floor(hf * 6);
        float f = hf * 6 - i;
        float p = vf * (1 - sf);
        float q = vf * (1 - f * sf);
        float t = vf * (1 - (1 - f) * sf);

        float r = 0, g = 0, b = 0;
        switch (i % 6) {
            case 0: r = vf; g = t; b = p; break;
            case 1: r = q; g = vf; b = p; break;
            case 2: r = p; g = vf; b = t; break;
            case 3: r = p; g = q; b = vf; break;
            case 4: r = t; g = p; b = vf; break;
            case 5: r = vf; g = p; b = q; break;
        }
        return new int[]{Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)};
    }


    public static float[] rgbToCmyk(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float k = 1.0f - Math.max(rf, Math.max(gf, bf));
        float c, m, y;

        if (k == 1.0f) {
            c = 0; m = 0; y = 0; // Защита от деления на ноль
        } else {
            c = (1.0f - rf - k) / (1.0f - k);
            m = (1.0f - gf - k) / (1.0f - k);
            y = (1.0f - bf - k) / (1.0f - k);
        }
        return new float[]{c * 100, m * 100, y * 100, k * 100};
    }


    public static int[] cmykToRgb(float c, float m, float y, float k) {
        float cf = c / 100.0f;
        float mf = m / 100.0f;
        float yf = y / 100.0f;
        float kf = k / 100.0f;

        int r = Math.round(255 * (1 - cf) * (1 - kf));
        int g = Math.round(255 * (1 - mf) * (1 - kf));
        int b = Math.round(255 * (1 - yf) * (1 - kf));

        return new int[]{r, g, b};
    }

    // ЛОГИКА ОБНОВЛЕНИЯ ИНТЕРФЕЙСА

    private void updateAllFromCMYK() {
        if (isUpdating) return;
        isUpdating = true;

        float c = cSlider.getValue();
        float m = mSlider.getValue();
        float y = ySlider.getValue();
        float k = kSlider.getValue();

        // 1. CMYK -> RGB
        int[] rgb = cmykToRgb(c, m, y, k);
        // 2. RGB -> HSV
        float[] hsv = rgbToHsv(rgb[0], rgb[1], rgb[2]);

        updateRGBFields(rgb[0], rgb[1], rgb[2]);
        updateHSVFields(Math.round(hsv[0]), Math.round(hsv[1]), Math.round(hsv[2]));
        updatePreview(rgb[0], rgb[1], rgb[2]);

        isUpdating = false;
    }

    private void updateAllFromRGB(int r, int g, int b) {
        if (isUpdating) return;
        isUpdating = true;

        r = clamp(r, 0, 255); g = clamp(g, 0, 255); b = clamp(b, 0, 255);

        // 1. RGB -> CMYK
        float[] cmyk = rgbToCmyk(r, g, b);
        // 2. RGB -> HSV
        float[] hsv = rgbToHsv(r, g, b);

        updateCMYKFields(Math.round(cmyk[0]), Math.round(cmyk[1]), Math.round(cmyk[2]), Math.round(cmyk[3]));
        updateHSVFields(Math.round(hsv[0]), Math.round(hsv[1]), Math.round(hsv[2]));
        updatePreview(r, g, b);

        isUpdating = false;
    }

    private void updateAllFromHSV(int h, int s, int v) {
        if (isUpdating) return;
        isUpdating = true;

        h = clamp(h, 0, 360); s = clamp(s, 0, 100); v = clamp(v, 0, 100);

        // 1. HSV -> RGB
        int[] rgb = hsvToRgb(h, s, v);
        // 2. RGB -> CMYK
        float[] cmyk = rgbToCmyk(rgb[0], rgb[1], rgb[2]);

        updateRGBFields(rgb[0], rgb[1], rgb[2]);
        updateCMYKFields(Math.round(cmyk[0]), Math.round(cmyk[1]), Math.round(cmyk[2]), Math.round(cmyk[3]));
        updatePreview(rgb[0], rgb[1], rgb[2]);

        isUpdating = false;
    }

    // Вспомогательные методы для обновления полей
    private void updateCMYKFields(int c, int m, int y, int k) {
        cSlider.setValue(c); cField.setText(String.valueOf(c));
        mSlider.setValue(m); mField.setText(String.valueOf(m));
        ySlider.setValue(y); yField.setText(String.valueOf(y));
        kSlider.setValue(k); kField.setText(String.valueOf(k));
    }

    private void updateRGBFields(int r, int g, int b) {
        rSlider.setValue(r); rField.setText(String.valueOf(r));
        gSlider.setValue(g); gField.setText(String.valueOf(g));
        bSlider.setValue(b); bField.setText(String.valueOf(b));
    }

    private void updateHSVFields(int h, int s, int v) {
        hSlider.setValue(h); hField.setText(String.valueOf(h));
        sSlider.setValue(s); sField.setText(String.valueOf(s));
        vSlider.setValue(v); vField.setText(String.valueOf(v));
    }

    private void updatePreview(int r, int g, int b) {
        colorPreview.setBackground(new Color(r, g, b));
    }

    private void openPalette() {
        Color initial = colorPreview.getBackground();
        Color chosen = JColorChooser.showDialog(this, "Выберите цвет", initial);
        if (chosen != null) {
            updateAllFromRGB(chosen.getRed(), chosen.getGreen(), chosen.getBlue());
        }
    }

    // СОЗДАНИЕ ИНТЕРФЕЙСА (ПАНЕЛИ)

    private JPanel createCMYKPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 3, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("CMYK (0-100)"));

        cSlider = createSlider(0, 100); mSlider = createSlider(0, 100);
        ySlider = createSlider(0, 100); kSlider = createSlider(0, 100);
        cField = createTextField(); mField = createTextField();
        yField = createTextField(); kField = createTextField();

        JButton paletteBtn = new JButton("Палитра");
        paletteBtn.addActionListener(e -> openPalette());

        ChangeListener sliderListener = e -> updateAllFromCMYK();
        cSlider.addChangeListener(sliderListener);
        mSlider.addChangeListener(sliderListener);
        ySlider.addChangeListener(sliderListener);
        kSlider.addChangeListener(sliderListener);

        ActionListener fieldListener = e -> updateAllFromCMYK();
        cField.addActionListener(fieldListener);
        mField.addActionListener(fieldListener);
        yField.addActionListener(fieldListener);
        kField.addActionListener(fieldListener);

        panel.add(new JLabel("C:")); panel.add(cSlider); panel.add(cField);
        panel.add(new JLabel("M:")); panel.add(mSlider); panel.add(mField);
        panel.add(new JLabel("Y:")); panel.add(ySlider); panel.add(yField);
        panel.add(new JLabel("K:")); panel.add(kSlider); panel.add(kField);
        panel.add(new JLabel("")); panel.add(paletteBtn); panel.add(new JLabel(""));
        return panel;
    }

    private JPanel createRGBPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 3, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("RGB (0-255)"));

        rSlider = createSlider(0, 255); gSlider = createSlider(0, 255); bSlider = createSlider(0, 255);
        rField = createTextField(); gField = createTextField(); bField = createTextField();

        JButton paletteBtn = new JButton("Палитра");
        paletteBtn.addActionListener(e -> openPalette());

        ChangeListener sliderListener = e -> updateAllFromRGB(rSlider.getValue(), gSlider.getValue(), bSlider.getValue());
        rSlider.addChangeListener(sliderListener);
        gSlider.addChangeListener(sliderListener);
        bSlider.addChangeListener(sliderListener);

        ActionListener fieldListener = e -> updateAllFromRGB(getFieldVal(rField), getFieldVal(gField), getFieldVal(bField));
        rField.addActionListener(fieldListener);
        gField.addActionListener(fieldListener);
        bField.addActionListener(fieldListener);

        panel.add(new JLabel("R:")); panel.add(rSlider); panel.add(rField);
        panel.add(new JLabel("G:")); panel.add(gSlider); panel.add(gField);
        panel.add(new JLabel("B:")); panel.add(bSlider); panel.add(bField);
        panel.add(new JLabel("")); panel.add(paletteBtn); panel.add(new JLabel(""));
        return panel;
    }

    private JPanel createHSVPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 3, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("HSV (H:0-360, S,V:0-100)"));

        hSlider = createSlider(0, 360); sSlider = createSlider(0, 100); vSlider = createSlider(0, 100);
        hField = createTextField(); sField = createTextField(); vField = createTextField();

        JButton paletteBtn = new JButton("Палитра");
        paletteBtn.addActionListener(e -> openPalette());

        ChangeListener sliderListener = e -> updateAllFromHSV(hSlider.getValue(), sSlider.getValue(), vSlider.getValue());
        hSlider.addChangeListener(sliderListener);
        sSlider.addChangeListener(sliderListener);
        vSlider.addChangeListener(sliderListener);

        ActionListener fieldListener = e -> updateAllFromHSV(getFieldVal(hField), getFieldVal(sField), getFieldVal(vField));
        hField.addActionListener(fieldListener);
        sField.addActionListener(fieldListener);
        vField.addActionListener(fieldListener);

        panel.add(new JLabel("H:")); panel.add(hSlider); panel.add(hField);
        panel.add(new JLabel("S:")); panel.add(sSlider); panel.add(sField);
        panel.add(new JLabel("V:")); panel.add(vSlider); panel.add(vField);
        panel.add(new JLabel("")); panel.add(paletteBtn); panel.add(new JLabel(""));
        return panel;
    }

    // УТИЛИТЫ

    private JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(45, 22));
        return field;
    }

    private int getFieldVal(JTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ColorLab::new);
    }
}