import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Original Android launcher geometry, drawn locally into a PNG-backed Windows ICO. */
public final class GenerateWindowsIcon {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output .ico path");
        BufferedImage icon = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        var g = icon.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(256.0 / 108, 256.0 / 108);
            g.setColor(new Color(0x071E30));
            g.fillRect(0, 0, 108, 108);
            Path2D hull = new Path2D.Double();
            hull.moveTo(54, 16);
            hull.curveTo(27, 16, 27, 82, 54, 92);
            hull.curveTo(81, 82, 81, 16, 54, 16);
            hull.closePath();
            g.setColor(new Color(0x50E1D0));
            g.fill(hull);
            g.setColor(new Color(0x103B50));
            g.fillOval(41, 27, 26, 26);
            g.setColor(new Color(0xF4AE63));
            g.fillRect(25, 62, 13, 19);
            g.fillRect(70, 62, 13, 19);
        } finally { g.dispose(); }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(icon, "png", png);
        ByteBuffer ico = ByteBuffer.allocate(22 + png.size()).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) 1);
        ico.putInt(0); // 0 width/height means 256 pixels; no palette.
        ico.putShort((short) 1).putShort((short) 32).putInt(png.size()).putInt(22);
        ico.put(png.toByteArray());
        Files.write(Path.of(args[0]), ico.array());
    }
}
