import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;

/** Generates Google Play listing art from Project Blue's original runtime visual language. */
public final class GenerateStoreAssets {
    private static final Color INK = Color.decode("#061827");
    private static final Color PANEL = Color.decode("#0B2939");
    private static final Color EDGE = Color.decode("#1B4755");
    private static final Color TEXT = Color.decode("#E4F2E8");
    private static final Color AQUA = Color.decode("#64E8D2");
    private static final Color GOLD = Color.decode("#FFC078");
    private static final Path OUT = Path.of("store/assets/google-play/ocean-guard");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT.resolve("screenshots"));
        writeIcon();
        writeFeature();
        exportScreenshot("build/device-qa/physical/drag.png", "01-core-gameplay.png", true);
        exportScreenshot("build/device-qa/physical/soak-current.png", "02-intense-combat.png", true);
        exportScreenshot("build/smoke/07-result.png", "03-cleanup-rescue.png", false);
        exportScreenshot("build/smoke/11-upgrades.png", "04-upgrades.png", false);
        exportScreenshot("build/smoke/09-submarines.png", "05-submarine-selection.png", false);
        exportScreenshot("build/smoke/12-achievements.png", "06-achievements.png", false);
        exportScreenshot("build/smoke/03-briefing.png", "07-boss-preparation.png", false);
    }

    private static void writeIcon() throws Exception {
        BufferedImage image = canvas(512, 512);
        Graphics2D g = graphics(image);
        gradient(g, 512, 512);
        g.setColor(new Color(100,232,210,45));
        for (int r : new int[]{170, 220, 275}) g.draw(new Ellipse2D.Float(256-r/2f,256-r/2f,r,r));
        drawSubmarine(g, 256, 266, 2.05f);
        g.dispose();
        ImageIO.write(image, "png", OUT.resolve("app-icon-512.png").toFile());
    }

    private static void writeFeature() throws Exception {
        BufferedImage image = canvas(1024, 500);
        Graphics2D g = graphics(image);
        gradient(g, 1024, 500);
        g.setColor(new Color(27,71,85,125));
        for (int y=75;y<500;y+=95) g.draw(new QuadCurve2D.Float(0,y,280,y-35,560,y+5));
        g.setColor(new Color(100,232,210,38));
        g.fill(new Polygon(new int[]{670,790,900,755},new int[]{0,0,500,500},4));
        drawSubmarine(g, 790, 270, 1.55f);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        g.setColor(TEXT); g.drawString("PROJECT BLUE", 62, 205);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 59));
        g.setColor(AQUA); g.drawString("OCEAN GUARD", 58, 273);
        g.setColor(GOLD); g.fillRoundRect(62,302,405,7,7,7);
        g.dispose();
        ImageIO.write(image, "png", OUT.resolve("feature-graphic-1024x500.png").toFile());
    }

    private static void exportScreenshot(String source, String name, boolean cropAndroid) throws Exception {
        BufferedImage input = ImageIO.read(new File(source));
        BufferedImage frame;
        if (cropAndroid) {
            int y = Math.max(0, (input.getHeight()-1920)/2);
            frame = input.getSubimage(0,y,1080,1920);
        } else {
            frame = new BufferedImage(1080,1920,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = frame.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(input,0,0,1080,1920,null); g.dispose();
        }
        ImageIO.write(frame,"png",OUT.resolve("screenshots").resolve(name).toFile());
    }

    private static BufferedImage canvas(int w,int h) { return new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB); }
    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(3f)); return g;
    }
    private static void gradient(Graphics2D g,int w,int h) {
        g.setPaint(new GradientPaint(0,0,PANEL,0,h,INK)); g.fillRect(0,0,w,h);
        g.setColor(new Color(100,232,210,18));
        for(int i=0;i<45;i++) { int x=(i*193)%w,y=(i*83)%h; g.fillOval(x,y,3+(i%4),3+(i%4)); }
    }
    private static void drawSubmarine(Graphics2D g,int x,int y,float s) {
        AffineTransform old=g.getTransform(); g.translate(x,y); g.scale(s,s);
        g.setColor(GOLD); g.fillRoundRect(-64,-35,128,70,18,18);
        g.setColor(TEXT); g.fillOval(-45,-78,90,156);
        g.setColor(INK); g.fillOval(-30,-57,60,60);
        g.setColor(AQUA); g.fillOval(-20,-47,40,40);
        g.setColor(PANEL); g.fillRoundRect(-25,28,50,31,7,7);
        g.setColor(INK); for(int i=-1;i<=1;i++)g.fillRect(-17,40+i*9,34,5);
        g.setColor(AQUA); g.fill(new Polygon(new int[]{-14,14,0},new int[]{79,79,103},3));
        g.setTransform(old);
    }
}
