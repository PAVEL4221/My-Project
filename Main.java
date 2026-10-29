import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Main extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    private enum GameState { MENU, SETTINGS, PLAYING, EDITOR, VICTORY }
    private GameState currentState = GameState.MENU;

    private int currentLevel = 1;
    private int playerSize = 50;
    private int score = 0;
    private int coins = 0;
    private int currentWeapon = 1;

    private final double START_X = 100;
    private final double START_Y = 100;
    private double playerX = START_X, playerY = START_Y;
    private double velX = 0, velY = 0;
    private final double GRAVITY = 0.5;
    private final double JUMP_FORCE = -12.0;
    private final double FRICTION = 0.85;

    private final double ZOOM = 1.5;

    // --- КРЮК ---
    private boolean isHooking = false;
    private double hookX = 0, hookY = 0;
    private double hookStartX = 0, hookStartY = 0;
    private double hookTargetX = 0, hookTargetY = 0;
    private double hookProgress = 0;
    private final double HOOK_SPEED = 0.2;
    private boolean isHookAttached = false;

    private boolean left, right;
    private boolean isOnGround = false;

    // --- РЕДАКТОР КАРТ ---
    private String selectedTool = "";
    private boolean isDragging = false;
    private Rectangle dragPreview = null;

    // --- МИШЕНИ ---
    private class Target {
        double x, y;
        int size = 30;
        boolean isHit = false;
        Target(double x, double y) { this.x = x; this.y = y; }
    }
    private ArrayList<Target> targets = new ArrayList<>();

    private class Coin {
        double x, y;
        int size = 20;
        Coin(double x, double y) { this.x = x; this.y = y; }
    }
    private ArrayList<Coin> coinsList = new ArrayList<>();

    private class Bullet {
        double x, y, vx, vy;
        Bullet(double x, double y, double vx, double vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private long lastShootTime = 0;
    private final int SHOOT_COOLDOWN = 200;

    private Image catImage;
    private Image gunImage;
    private Image hammerImage;
    private Image cursorFilled;
    private Image cursorOutline;
    private int mouseX = 0, mouseY = 0;

    private ArrayList<Rectangle> platforms = new ArrayList<>();
    private ArrayList<Rectangle> spikes = new ArrayList<>();
    private Timer timer;

    // --- КНОПКИ МЕНЮ ---
    private Rectangle btnPlay1 = new Rectangle(400, 100, 200, 45);
    private Rectangle btnPlay2 = new Rectangle(400, 160, 200, 45);
    private Rectangle btnPlay3 = new Rectangle(400, 220, 200, 45);
    private Rectangle btnEditor = new Rectangle(400, 280, 200, 45);
    private Rectangle btnSettings = new Rectangle(400, 340, 200, 45);
    private Rectangle btnBack = new Rectangle(400, 500, 200, 50);
    private Rectangle btnMenuInGame = new Rectangle(10, 10, 100, 40);

    // --- КНОПКИ РЕДАКТОРА ---
    private Rectangle btnToolPlatform = new Rectangle(10, 50, 150, 40);
    private Rectangle btnToolSpike = new Rectangle(10, 100, 150, 40);
    private Rectangle btnSaveMap = new Rectangle(10, 160, 150, 40);
    private Rectangle btnLoadMap = new Rectangle(10, 210, 150, 40);
    private Rectangle btnSavePlay = new Rectangle(10, 500, 150, 40);
    private Rectangle btnBackEditor = new Rectangle(10, 550, 150, 40);

    private Rectangle sliderRect = new Rectangle(300, 300, 400, 20);

    public Main() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(new Color(30, 30, 30));
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        this.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
                new Point(0, 0), "null"));

        catImage = loadImage("asset/cat.png", "src/asset/cat.png");
        cursorFilled = loadImage("asset/cursor_1.png", "src/asset/cursor_1.png");
        cursorOutline = loadImage("asset/cursor_2.png", "src/asset/cursor_2.png");
        gunImage = loadImage("asset/Pistol.png", "src/asset/Pistol.png");
        hammerImage = loadImage("asset/Hammer.png", "src/asset/Hammer.png");

        loadLevel(1);

        timer = new Timer(16, this);
        timer.start();
    }

    private void loadLevel(int level) {
        currentLevel = level;
        platforms.clear();
        spikes.clear();
        targets.clear();
        coinsList.clear();
        playerX = START_X;
        playerY = START_Y;
        velX = 0; velY = 0;
        bullets.clear();
        isHooking = false;
        hookProgress = 0;
        currentWeapon = 1;

        if (level == 1) {
            platforms.add(new Rectangle(0, 550, WIDTH, 50));
            platforms.add(new Rectangle(300, 450, 200, 20));
            platforms.add(new Rectangle(650, 350, 200, 20));
            platforms.add(new Rectangle(100, 300, 100, 20));
            platforms.add(new Rectangle(800, 200, 150, 20));
            spikes.add(new Rectangle(350, 430, 100, 20));
            spikes.add(new Rectangle(680, 330, 150, 20));

            coinsList.add(new Coin(150, 500));
            coinsList.add(new Coin(350, 400));
            coinsList.add(new Coin(700, 300));

        } else if (level == 2) {
            platforms.add(new Rectangle(0, 580, 200, 20));
            platforms.add(new Rectangle(900, 200, 100, 20));
            spikes.add(new Rectangle(200, 580, 300, 20));
            spikes.add(new Rectangle(0, 560, 200, 20));
            coinsList.add(new Coin(500, 400));
            coinsList.add(new Coin(800, 300));

        } else if (level == 3) {
            platforms.add(new Rectangle(0, 550, 200, 50));
            platforms.add(new Rectangle(400, 500, 100, 20));
            platforms.add(new Rectangle(700, 450, 100, 20));
            platforms.add(new Rectangle(1000, 400, 100, 20));
            platforms.add(new Rectangle(1300, 350, 100, 20));
            platforms.add(new Rectangle(1600, 300, 100, 20));
            platforms.add(new Rectangle(1900, 250, 200, 50));

            spikes.add(new Rectangle(200, 580, 1800, 20));
            spikes.add(new Rectangle(300, 520, 50, 20));
            spikes.add(new Rectangle(600, 470, 50, 20));
            spikes.add(new Rectangle(900, 420, 50, 20));
            spikes.add(new Rectangle(1200, 370, 50, 20));
            spikes.add(new Rectangle(1500, 320, 50, 20));

            targets.add(new Target(450, 400));
            targets.add(new Target(750, 350));
            targets.add(new Target(1050, 300));
            targets.add(new Target(1350, 250));
            targets.add(new Target(1650, 200));

            coinsList.add(new Coin(500, 300));
            coinsList.add(new Coin(1000, 200));
        }
    }

    private Image loadImage(String path1, String path2) {
        Image img = new ImageIcon(path1).getImage();
        if (img.getWidth(null) == -1) {
            img = new ImageIcon(path2).getImage();
        }
        return img;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == GameState.MENU) drawMenu(g2d);
        else if (currentState == GameState.SETTINGS) drawSettings(g2d);
        else if (currentState == GameState.PLAYING) drawGame(g2d);
        else if (currentState == GameState.EDITOR) drawEditor(g2d);
        else if (currentState == GameState.VICTORY) drawVictory(g2d);

        drawCustomCursor(g2d);
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.drawString("Dytec", 400, 60);

        drawMenuButton(g2d, btnPlay1, "PLAY 1 (EASY)", new Color(50, 150, 50), new Color(100, 200, 100));
        drawMenuButton(g2d, btnPlay2, "PLAY 2 (HARD)", new Color(150, 50, 50), new Color(200, 100, 100));
        drawMenuButton(g2d, btnPlay3, "PLAY 3 (TARGETS)", new Color(50, 150, 200), new Color(100, 200, 255));
        drawMenuButton(g2d, btnEditor, "EDITOR", new Color(50, 100, 150), new Color(100, 150, 200));
        drawMenuButton(g2d, btnSettings, "SETTINGS", new Color(100, 100, 100), new Color(150, 150, 150));
    }

    private void drawMenuButton(Graphics2D g2d, Rectangle rect, String text, Color normalColor, Color hoverColor) {
        g2d.setColor(rect.contains(mouseX, mouseY) ? hoverColor : normalColor);
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(text, rect.x + 20, rect.y + 30);
    }

    private void drawSettings(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("SETTINGS", 400, 100);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Player Size: " + playerSize, 350, 250);

        g2d.setColor(Color.GRAY);
        g2d.fillRect(sliderRect.x, sliderRect.y, sliderRect.width, sliderRect.height);

        int fillWidth = (int) (((double)(playerSize - 30) / 220) * sliderRect.width);
        g2d.setColor(new Color(100, 200, 100));
        g2d.fillRect(sliderRect.x, sliderRect.y, fillWidth, sliderRect.height);

        g2d.setColor(Color.WHITE);
        g2d.fillOval(sliderRect.x + fillWidth - 10, sliderRect.y - 10, 40, 40);

        g2d.setColor(btnBack.contains(mouseX, mouseY) ? new Color(200, 200, 200) : Color.GRAY);
        g2d.fillRoundRect(btnBack.x, btnBack.y, btnBack.width, btnBack.height, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("BACK", btnBack.x + 70, btnBack.y + 35);
    }

    private void drawGame(Graphics2D g2d) {
        AffineTransform originalTransform = g2d.getTransform();

        g2d.translate(WIDTH / 2.0, HEIGHT / 2.0);
        g2d.scale(ZOOM, ZOOM);
        g2d.translate(-playerX, -playerY);

        // Платформы
        g2d.setColor(new Color(80, 80, 80));
        for (Rectangle p : platforms) {
            g2d.fillRect(p.x, p.y, p.width, p.height);
            g2d.setColor(new Color(120, 120, 120));
            g2d.drawRect(p.x, p.y, p.width, p.height);
            g2d.setColor(new Color(80, 80, 80));
        }

        // Шипы
        g2d.setColor(new Color(200, 30, 30));
        for (Rectangle s : spikes) {
            int spikeCount = s.width / 15;
            for (int i = 0; i < spikeCount; i++) {
                int x = s.x + i * 15;
                int[] xPoints = {x, x + 7, x + 15};
                int[] yPoints = {s.y + s.height, s.y, s.y + s.height};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }

        // Мишени
        for (Target t : targets) {
            if (!t.isHit) {
                g2d.setColor(new Color(200, 50, 50));
                g2d.fillOval((int)t.x - t.size/2, (int)t.y - t.size/2, t.size, t.size);
                g2d.setColor(Color.WHITE);
                g2d.drawOval((int)t.x - t.size/2, (int)t.y - t.size/2, t.size, t.size);
                g2d.drawLine((int)t.x - 10, (int)t.y, (int)t.x + 10, (int)t.y);
                g2d.drawLine((int)t.x, (int)t.y - 10, (int)t.x, (int)t.y + 10);
            }
        }

        // Монетки
        g2d.setColor(Color.YELLOW);
        for (Coin c : coinsList) {
            g2d.fillOval((int)c.x, (int)c.y, c.size, c.size);
            g2d.setColor(Color.ORANGE);
            g2d.drawOval((int)c.x, (int)c.y, c.size, c.size);
            g2d.setColor(Color.YELLOW);
        }

        // Крюк
        int cx = (int) playerX;
        int cy = (int) playerY;
        if (isHooking) {
            double currentHookX = hookStartX + (hookTargetX - hookStartX) * hookProgress;
            double currentHookY = hookStartY + (hookTargetY - hookStartY) * hookProgress;

            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.setStroke(new BasicStroke(6));
            g2d.drawLine(cx, cy, (int)currentHookX, (int)currentHookY);

            g2d.setColor(new Color(100, 100, 100));
            g2d.setStroke(new BasicStroke(3));
            double sag = Math.max(0, 150 - Math.sqrt(Math.pow(currentHookX-cx, 2) + Math.pow(currentHookY-cy, 2))) * 0.5;
            double midX = (cx + currentHookX) / 2;
            double midY = (cy + currentHookY) / 2 + sag;

            QuadCurve2D curve = new QuadCurve2D.Double(cx, cy, midX, midY, currentHookX, currentHookY);
            g2d.draw(curve);

            g2d.setColor(new Color(200, 200, 200, 150));
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(curve);

            double angle = Math.atan2(currentHookY - cy, currentHookX - cx);
            int size = 14;

            g2d.setColor(new Color(255, 100, 100, 100));
            g2d.fillOval((int) currentHookX - 10, (int) currentHookY - 10, 20, 20);

            AffineTransform old = g2d.getTransform();
            g2d.translate(currentHookX, currentHookY);
            g2d.rotate(angle);

            g2d.setColor(new Color(80, 80, 80));
            int[] baseX = {0, -5, -5, 0};
            int[] baseY = {0, -4, 4, 0};
            g2d.fillPolygon(baseX, baseY, 4);

            g2d.setColor(new Color(180, 180, 180));
            int[] tipX = {0, size, -5};
            int[] tipY = {0, -4, 4};
            g2d.fillPolygon(tipX, tipY, 3);

            g2d.setColor(Color.WHITE);
            g2d.drawLine(0, 0, size - 2, -1);

            g2d.setTransform(old);
        }

        // Чубрик
        if (catImage != null) {
            g2d.drawImage(catImage, cx - playerSize/2, cy - playerSize/2, playerSize, playerSize, this);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillOval(cx - playerSize/2, cy - playerSize/2, playerSize, playerSize);
        }

        // Пули
        g2d.setColor(Color.YELLOW);
        for (Bullet b : bullets) {
            g2d.fillOval((int)b.x, (int)b.y, 10, 10);
        }

        // Оружие
        double worldMouseX = (mouseX - WIDTH/2.0) / ZOOM + playerX;
        double worldMouseY = (mouseY - HEIGHT/2.0) / ZOOM + playerY;
        double angle = Math.atan2(worldMouseY - cy, worldMouseX - cx);

        int gunX = (int) (cx + Math.cos(angle) * (playerSize/2));
        int gunY = (int) (cy + Math.sin(angle) * (playerSize/2));

        AffineTransform old = g2d.getTransform();
        g2d.translate(gunX, gunY);
        g2d.rotate(angle);

        if (currentWeapon == 1 && gunImage != null && gunImage.getWidth(null) > 0) {
            g2d.drawImage(gunImage, 0, -10, 40, 20, this);
        } else if (currentWeapon == 2 && hammerImage != null && hammerImage.getWidth(null) > 0) {
            g2d.drawImage(hammerImage, 0, -15, 45, 45, this);
        }
        g2d.setTransform(old);

        g2d.setTransform(originalTransform);

        // UI
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.drawString("Score: " + score, 20, 40);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Weapon: " + (currentWeapon == 1 ? "Gun (1)" : "Hammer (2)"), 20, 80);

        if (currentState == GameState.PLAYING) {
            g2d.setColor(new Color(200, 50, 50));
            g2d.fillRoundRect(btnMenuInGame.x, btnMenuInGame.y, btnMenuInGame.width, btnMenuInGame.height, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("MENU", btnMenuInGame.x + 20, btnMenuInGame.y + 25);
        }
    }

    private void drawVictory(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 80));
        g2d.drawString("VICTORY!", 250, 250);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("You earned " + coins + " coins!", 300, 350);

        g2d.setColor(Color.GRAY);
        g2d.fillRoundRect(400, 450, 200, 50, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("CONTINUE", 435, 485);
    }

    // --- РЕДАКТОР КАРТ ---
    private void drawEditor(Graphics2D g2d) {
        // Сетка
        g2d.setColor(new Color(50, 50, 50));
        for (int i = 0; i < WIDTH; i += 20) g2d.drawLine(i, 0, i, HEIGHT);
        for (int i = 0; i < HEIGHT; i += 20) g2d.drawLine(0, i, WIDTH, i);

        // Рисуем то, что уже поставили
        g2d.setColor(new Color(80, 80, 80));
        for (Rectangle p : platforms) {
            g2d.fillRect(p.x, p.y, p.width, p.height);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(p.x, p.y, p.width, p.height);
            g2d.setColor(new Color(80, 80, 80));
        }

        g2d.setColor(new Color(200, 30, 30));
        for (Rectangle s : spikes) {
            int spikeCount = s.width / 15;
            for (int i = 0; i < spikeCount; i++) {
                int x = s.x + i * 15;
                int[] xPoints = {x, x + 7, x + 15};
                int[] yPoints = {s.y + s.height, s.y, s.y + s.height};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }

        // Предпросмотр
        if (isDragging && dragPreview != null) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillRect(dragPreview.x, dragPreview.y, dragPreview.width, dragPreview.height);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(dragPreview.x, dragPreview.y, dragPreview.width, dragPreview.height);
        }

        // Панель слева
        g2d.setColor(new Color(40, 40, 40));
        g2d.fillRect(0, 0, 170, HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(170, 0, 170, HEIGHT);

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("EDITOR", 40, 30);

        drawEditorButton(g2d, btnToolPlatform, "Platform", selectedTool.equals("platform"));
        drawEditorButton(g2d, btnToolSpike, "Spike", selectedTool.equals("spike"));
        drawEditorButton(g2d, btnSaveMap, "SAVE .cart", false);
        drawEditorButton(g2d, btnLoadMap, "LOAD .cart", false);
        drawEditorButton(g2d, btnSavePlay, "SAVE & PLAY", false);
        drawEditorButton(g2d, btnBackEditor, "BACK", false);
    }

    private void drawEditorButton(Graphics2D g2d, Rectangle r, String text, boolean isSelected) {
        g2d.setColor(isSelected ? new Color(100, 200, 100) : new Color(80, 80, 80));
        g2d.fillRect(r.x, r.y, r.width, r.height);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(r.x, r.y, r.width, r.height);
        g2d.drawString(text, r.x + 10, r.y + 25);
    }

    private void drawCustomCursor(Graphics2D g2d) {
        boolean isHovering = false;
        if (currentState == GameState.MENU) {
            isHovering = btnPlay1.contains(mouseX, mouseY) || btnPlay2.contains(mouseX, mouseY) || btnPlay3.contains(mouseX, mouseY) || btnEditor.contains(mouseX, mouseY) || btnSettings.contains(mouseX, mouseY);
        } else if (currentState == GameState.SETTINGS) {
            isHovering = btnBack.contains(mouseX, mouseY) || sliderRect.contains(mouseX, mouseY);
        } else if (currentState == GameState.PLAYING) {
            isHovering = btnMenuInGame.contains(mouseX, mouseY);
        } else if (currentState == GameState.EDITOR) {
            isHovering = (mouseX < 170);
        } else if (currentState == GameState.VICTORY) {
            isHovering = true;
        }

        Image cursorToDraw = isHovering ? cursorFilled : cursorOutline;

        if (cursorToDraw != null && cursorToDraw.getWidth(null) > 0) {
            g2d.drawImage(cursorToDraw, mouseX - 5, mouseY - 5, 32, 32, this);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(mouseX - 5, mouseY - 5, 10, 10);
        }
    }

    private boolean isPointOnPlatform(double x, double y) {
        for (Rectangle p : platforms) {
            if (p.contains(x, y)) return true;
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == GameState.PLAYING) {
            if (currentLevel == 3) {
                boolean allHit = true;
                for (Target t : targets) {
                    if (!t.isHit) { allHit = false; break; }
                }
                if (allHit && !targets.isEmpty()) {
                    currentState = GameState.VICTORY;
                    coins += 50;
                    score += 50;
                    return;
                }
            }

            velY += GRAVITY;
            if (left) velX -= 1.0;
            if (right) velX += 1.0;
            velX *= FRICTION;
            if (Math.abs(velX) < 0.1) velX = 0;

            if (isHooking) {
                if (hookProgress < 1.0) {
                    hookProgress += HOOK_SPEED;
                    if (hookProgress > 1.0) hookProgress = 1.0;
                }

                if (hookProgress >= 1.0 && isHookAttached) {
                    double dx = hookX - playerX;
                    double dy = hookY - playerY;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > 10) {
                        velX += (dx / dist * 1.5);
                        velY += (dy / dist * 1.5);
                        if (dist > 300) isHooking = false;
                    } else {
                        isHooking = false;
                    }
                } else if (hookProgress >= 1.0 && !isHookAttached) {
                    isHooking = false;
                }
            }

            playerX += velX;
            if (checkCollisions(playerX, playerY)) { playerX -= velX; velX = 0; }

            isOnGround = false;
            playerY += velY;
            if (checkCollisions(playerX, playerY)) {
                playerY -= velY;
                if (velY > 0) isOnGround = true;
                velY = 0;
            }
            if (playerY >= 550 - 20) isOnGround = true;

            if (!isOnGround || isHooking) {
                Sound.stop();
            }

            if (playerX < 20) { playerX = 20; velX = 0; }
            if (playerX > 2500) { playerX = 2500; velX = 0; }
            if (playerY > HEIGHT) { playerY = 0; velY = 0; }

            if (checkSpikes(playerX, playerY)) {
                playerX = START_X; playerY = START_Y; velX = 0; velY = 0; isHooking = false;
                Sound.stop();
                Sound.play("noplayers.wav");
            }

            Iterator<Coin> coinIt = coinsList.iterator();
            while (coinIt.hasNext()) {
                Coin c = coinIt.next();
                Rectangle playerRect = new Rectangle((int)(playerX - playerSize*0.4), (int)(playerY - playerSize*0.4), (int)(playerSize*0.8), (int)(playerSize*0.8));
                Rectangle coinRect = new Rectangle((int)c.x, (int)c.y, c.size, c.size);
                if (playerRect.intersects(coinRect)) {
                    coinIt.remove();
                    score += 10;
                    Sound.play("click.wav");
                }
            }

            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet b = it.next();
                b.x += b.vx;
                b.y += b.vy;

                for (Target t : targets) {
                    if (!t.isHit) {
                        Rectangle bulletRect = new Rectangle((int)b.x, (int)b.y, 10, 10);
                        Rectangle targetRect = new Rectangle((int)t.x - t.size/2, (int)t.y - t.size/2, t.size, t.size);
                        if (bulletRect.intersects(targetRect)) {
                            t.isHit = true;
                            it.remove();
                            score += 20;
                            break;
                        }
                    }
                }

                if (b.x < 0 || b.x > 2500 || b.y < 0 || b.y > HEIGHT) it.remove();
            }
        }
        repaint();
    }

    // --- СОХРАНЕНИЕ И ЗАГРУЗКА КАРТ (.cart) ---
    private void saveMapToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить карту");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Dytec Map (*.cart)", "cart"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".cart")) {
                file = new File(file.getAbsolutePath() + ".cart");
            }

            try (PrintWriter writer = new PrintWriter(file)) {
                for (Rectangle p : platforms) {
                    writer.println("platform " + p.x + " " + p.y + " " + p.width + " " + p.height);
                }
                for (Rectangle s : spikes) {
                    writer.println("spike " + s.x + " " + s.y + " " + s.width + " " + s.height);
                }
                JOptionPane.showMessageDialog(this, "Карта сохранена!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения!");
            }
        }
    }

    private void loadMapFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Загрузить карту");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Dytec Map (*.cart)", "cart"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            platforms.clear();
            spikes.clear();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length == 5) {
                        String type = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int w = Integer.parseInt(parts[3]);
                        int h = Integer.parseInt(parts[4]);

                        if (type.equals("platform")) {
                            platforms.add(new Rectangle(x, y, w, h));
                        } else if (type.equals("spike")) {
                            spikes.add(new Rectangle(x, y, w, h));
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "Карта загружена!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки!");
            }
        }
    }

    private boolean checkCollisions(double x, double y) {
        int hitboxSize = (int)(playerSize * 0.6);
        Rectangle playerRect = new Rectangle((int) (x - hitboxSize / 2), (int) (y - hitboxSize / 2), hitboxSize, hitboxSize);
        for (Rectangle p : platforms) { if (playerRect.intersects(p)) return true; }
        return false;
    }

    private boolean checkSpikes(double x, double y) {
        int hitboxSize = (int)(playerSize * 0.5);
        Rectangle playerRect = new Rectangle((int) (x - hitboxSize / 2), (int) (y - hitboxSize / 2), hitboxSize, hitboxSize);
        for (Rectangle s : spikes) { if (playerRect.intersects(s)) return true; }
        return false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentState == GameState.PLAYING) {
            if (e.getKeyCode() == KeyEvent.VK_A) {
                left = true;
                if (isOnGround && !isHooking) Sound.loop("run.wav");
            }
            if (e.getKeyCode() == KeyEvent.VK_D) {
                right = true;
                if (isOnGround && !isHooking) Sound.loop("run.wav");
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE && isOnGround) {
                velY = JUMP_FORCE;
                isOnGround = false;
                Sound.stop();
            }
            if (e.getKeyCode() == KeyEvent.VK_1) currentWeapon = 1;
            if (e.getKeyCode() == KeyEvent.VK_2) currentWeapon = 2;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) {
            left = false;
            if (!right) Sound.stop();
        }
        if (e.getKeyCode() == KeyEvent.VK_D) {
            right = false;
            if (!left) Sound.stop();
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        if (currentState == GameState.SETTINGS) {
            if (sliderRect.contains(e.getX(), e.getY()) || e.getY() >= sliderRect.y - 20 && e.getY() <= sliderRect.y + 40) {
                int newSize = (int) (((double)(e.getX() - sliderRect.x) / sliderRect.width) * 220 + 30);
                playerSize = Math.max(30, Math.min(250, newSize));
            }
        }
        else if (currentState == GameState.EDITOR) {
            if (isDragging && dragPreview != null) {
                dragPreview.x = (e.getX() / 20) * 20;
                dragPreview.y = (e.getY() / 20) * 20;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (currentState == GameState.MENU) {
            if (btnPlay1.contains(e.getX(), e.getY())) {
                Sound.play("click.wav");
                loadLevel(1);
                score = 0;
                currentState = GameState.PLAYING;
            } else if (btnPlay2.contains(e.getX(), e.getY())) {
                Sound.play("click.wav");
                loadLevel(2);
                score = 0;
                currentState = GameState.PLAYING;
            } else if (btnPlay3.contains(e.getX(), e.getY())) {
                Sound.play("click.wav");
                loadLevel(3);
                score = 0;
                currentState = GameState.PLAYING;
            } else if (btnEditor.contains(e.getX(), e.getY())) {
                Sound.play("click.wav");
                currentState = GameState.EDITOR;
                platforms.clear();
                spikes.clear();
                platforms.add(new Rectangle(0, 550, 150, 50));
            } else if (btnSettings.contains(e.getX(), e.getY())) {
                Sound.play("click.wav");
                currentState = GameState.SETTINGS;
            }
        } else if (currentState == GameState.SETTINGS) {
            if (btnBack.contains(e.getX(), e.getY())) {
                Sound.play("click.wav");
                currentState = GameState.MENU;
            }
        } else if (currentState == GameState.PLAYING) {
            if (btnMenuInGame.contains(e.getX(), e.getY())) {
                currentState = GameState.MENU;
                Sound.stop();
                return;
            }

            double worldMouseX = (e.getX() - WIDTH/2.0) / ZOOM + playerX;
            double worldMouseY = (e.getY() - HEIGHT/2.0) / ZOOM + playerY;

            if (e.getButton() == MouseEvent.BUTTON1) {
                Sound.stop();
                if (isPointOnPlatform(worldMouseX, worldMouseY)) {
                    isHooking = true;
                    isHookAttached = true;
                    hookX = worldMouseX;
                    hookY = worldMouseY;
                    hookStartX = playerX;
                    hookStartY = playerY;
                    hookTargetX = worldMouseX;
                    hookTargetY = worldMouseY;
                    hookProgress = 0;
                    Sound.play("click.wav");
                } else {
                    isHooking = false;
                    isHookAttached = false;
                }
            }

            if (e.getButton() == MouseEvent.BUTTON3 && currentWeapon == 1) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShootTime > SHOOT_COOLDOWN) {
                    double angle = Math.atan2(worldMouseY - playerY, worldMouseX - playerX);
                    double barrelX = playerX + Math.cos(angle) * 40;
                    double barrelY = playerY + Math.sin(angle) * 40;

                    bullets.add(new Bullet(barrelX, barrelY, Math.cos(angle)*15, Math.sin(angle)*15));
                    lastShootTime = currentTime;
                    Sound.play("shoot.wav");
                }
            }
        } else if (currentState == GameState.EDITOR) {
            if (e.getX() < 170) {
                if (btnToolPlatform.contains(e.getX(), e.getY())) {
                    selectedTool = "platform";
                } else if (btnToolSpike.contains(e.getX(), e.getY())) {
                    selectedTool = "spike";
                } else if (btnSaveMap.contains(e.getX(), e.getY())) {
                    saveMapToFile();
                } else if (btnLoadMap.contains(e.getX(), e.getY())) {
                    loadMapFromFile();
                } else if (btnSavePlay.contains(e.getX(), e.getY())) {
                    Sound.play("click.wav");
                    currentState = GameState.PLAYING;
                    playerX = 100;
                    playerY = 500;
                } else if (btnBackEditor.contains(e.getX(), e.getY())) {
                    currentState = GameState.MENU;
                }
                return;
            }

            if (!selectedTool.isEmpty()) {
                isDragging = true;
                int x = (e.getX() / 20) * 20;
                int y = (e.getY() / 20) * 20;
                dragPreview = new Rectangle(x, y, 100, 20);
            }
        } else if (currentState == GameState.VICTORY) {
            if (e.getX() > 400 && e.getX() < 600 && e.getY() > 450 && e.getY() < 500) {
                currentState = GameState.MENU;
                coins = 0;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (currentState == GameState.PLAYING && e.getButton() == MouseEvent.BUTTON1) {
            isHooking = false;
            isHookAttached = false;
        }
        else if (currentState == GameState.EDITOR && isDragging) {
            if (dragPreview != null && e.getX() > 170) {
                if (selectedTool.equals("platform")) {
                    platforms.add(new Rectangle(dragPreview.x, dragPreview.y, 100, 20));
                } else if (selectedTool.equals("spike")) {
                    spikes.add(new Rectangle(dragPreview.x, dragPreview.y, 100, 20));
                }
            }
            isDragging = false;
            dragPreview = null;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dytec");
        Main game = new Main();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}