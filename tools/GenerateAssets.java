import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Original geometric glyphs and sine-wave audio, reproducible without third-party assets. */
public final class GenerateAssets {
    private static final Map<Character, String> GLYPHS = new HashMap<>();
    public static void main(String[] args) throws Exception {
        String[] glyphs = {
            "A:01110/10001/10001/11111/10001/10001/10001",
            "B:11110/10001/10001/11110/10001/10001/11110",
            "C:01111/10000/10000/10000/10000/10000/01111",
            "D:11110/10001/10001/10001/10001/10001/11110",
            "E:11111/10000/10000/11110/10000/10000/11111",
            "F:11111/10000/10000/11110/10000/10000/10000",
            "G:01111/10000/10000/10111/10001/10001/01110",
            "H:10001/10001/10001/11111/10001/10001/10001",
            "I:11111/00100/00100/00100/00100/00100/11111",
            "J:00111/00010/00010/00010/10010/10010/01100",
            "K:10001/10010/10100/11000/10100/10010/10001",
            "L:10000/10000/10000/10000/10000/10000/11111",
            "M:10001/11011/10101/10101/10001/10001/10001",
            "N:10001/11001/11001/10101/10011/10011/10001",
            "O:01110/10001/10001/10001/10001/10001/01110",
            "P:11110/10001/10001/11110/10000/10000/10000",
            "Q:01110/10001/10001/10001/10101/10010/01101",
            "R:11110/10001/10001/11110/10100/10010/10001",
            "S:01111/10000/10000/01110/00001/00001/11110",
            "T:11111/00100/00100/00100/00100/00100/00100",
            "U:10001/10001/10001/10001/10001/10001/01110",
            "V:10001/10001/10001/10001/10001/01010/00100",
            "W:10001/10001/10001/10101/10101/10101/01010",
            "X:10001/10001/01010/00100/01010/10001/10001",
            "Y:10001/10001/01010/00100/00100/00100/00100",
            "Z:11111/00001/00010/00100/01000/10000/11111",
            "0:01110/10001/10011/10101/11001/10001/01110",
            "1:00100/01100/00100/00100/00100/00100/01110",
            "2:01110/10001/00001/00010/00100/01000/11111",
            "3:11110/00001/00001/01110/00001/00001/11110",
            "4:00010/00110/01010/10010/11111/00010/00010",
            "5:11111/10000/10000/11110/00001/00001/11110",
            "6:01110/10000/10000/11110/10001/10001/01110",
            "7:11111/00001/00010/00100/01000/01000/01000",
            "8:01110/10001/10001/01110/10001/10001/01110",
            "9:01110/10001/10001/01111/00001/00001/01110",
            " :00000/00000/00000/00000/00000/00000/00000",
            ".:00000/00000/00000/00000/00000/00110/00110",
            ",:00000/00000/00000/00000/00110/00110/00100",
            "-:00000/00000/00000/11111/00000/00000/00000",
            "+:00000/00100/00100/11111/00100/00100/00000",
            "/:00001/00001/00010/00100/01000/10000/10000",
            "%:11001/11010/00010/00100/01000/01011/10011",
            "!:00100/00100/00100/00100/00100/00000/00100",
            "?:01110/10001/00001/00010/00100/00000/00100",
            "::00000/00110/00110/00000/00110/00110/00000",
            ">:10000/01000/00100/00010/00100/01000/10000",
            "<:00001/00010/00100/01000/00100/00010/00001",
            "=:00000/00000/11111/00000/11111/00000/00000"
        };
        for (String g : glyphs) GLYPHS.put(g.charAt(0), g.substring(2));
        GLYPHS.put('Ç', "01111/10000/10000/10000/10000/01111/00100/01000");
        GLYPHS.put('Ğ', "01010/00100/01111/10000/10111/10001/01110");
        GLYPHS.put('İ', "00100/00000/11111/00100/00100/00100/11111");
        GLYPHS.put('Ö', "01010/00000/01110/10001/10001/10001/01110");
        GLYPHS.put('Ş', "01111/10000/01110/00001/00001/11110/00100/01000");
        GLYPHS.put('Ü', "01010/00000/10001/10001/10001/10001/01110");
        for (char lower : new char[]{'ç','ğ','ı','i','ö','ş','ü'}) GLYPHS.put(lower, GLYPHS.get(Character.toUpperCase(lower)));
        Files.createDirectories(Path.of("assets/fonts"));
        Files.createDirectories(Path.of("assets/audio"));
        List<Character> characters = new ArrayList<>();
        for (char ch = 32; ch <= 126; ch++) characters.add(ch);
        for (char ch : new char[]{'Ç','ç','Ğ','ğ','ı','İ','Ö','ö','Ş','ş','Ü','ü'}) characters.add(ch);
        BufferedImage atlas = new BufferedImage(324, 162, BufferedImage.TYPE_INT_ARGB);
        StringBuilder fnt = new StringBuilder("info face=\"Blue Grid\" size=21 bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=0 aa=1 padding=0,0,0,0 spacing=1,1\n"
            + "common lineHeight=27 base=21 scaleW=324 scaleH=162 pages=1 packed=0\npage id=0 file=\"blue.png\"\nchars count=" + characters.size() + "\n");
        for (int i = 0; i < characters.size(); i++) {
            int ch = characters.get(i), x = (i % 18) * 18, y = (i / 18) * 27;
            String glyph = GLYPHS.getOrDefault(Character.toUpperCase((char) ch), GLYPHS.get('?'));
            String[] rows = glyph.split("/");
            for (int row = 0; row < rows.length; row++) for (int col = 0; col < 5; col++) {
                if (rows[row].charAt(col) == '1') for (int dy = 0; dy < 3; dy++) for (int dx = 0; dx < 3; dx++) {
                    atlas.setRGB(x + col * 3 + dx, y + row * 3 + dy, 0xffffffff);
                }
            }
            fnt.append("char id=").append(ch).append(" x=").append(x).append(" y=").append(y)
                .append(" width=15 height=").append(rows.length * 3).append(" xoffset=0 yoffset=0 xadvance=18 page=0 chnl=15\n");
        }
        ImageIO.write(atlas, "png", Path.of("assets/fonts/blue.png").toFile());
        Files.writeString(Path.of("assets/fonts/blue.fnt"), fnt, StandardCharsets.UTF_8);
        wave("pulse", .09, 0);
        wave("collect", .28, 1);
        wave("ocean", 8, 2);
        System.out.println("Generated five original assets.");
    }
    private static void wave(String name, double duration, int kind) throws IOException {
        int rate = 22050, samples = (int) (rate * duration);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream("assets/audio/" + name + ".wav"))) {
            out.writeBytes("RIFF"); le32(out, 36 + samples * 2); out.writeBytes("WAVEfmt ");
            le32(out, 16); le16(out, 1); le16(out, 1); le32(out, rate); le32(out, rate * 2);
            le16(out, 2); le16(out, 16); out.writeBytes("data"); le32(out, samples * 2);
            for (int i = 0; i < samples; i++) {
                double t = (double) i / rate, value;
                if (kind == 0) value = Math.sin(2 * Math.PI * (720 * t - 1800 * t * t)) * Math.exp(-t * 48) * .35;
                else if (kind == 1) value = (Math.sin(2 * Math.PI * 660 * t) + .5 * Math.sin(2 * Math.PI * 990 * t)) * Math.sin(Math.PI * t / duration) * .22;
                else value = (Math.sin(2 * Math.PI * 110 * t) + .5 * Math.sin(2 * Math.PI * 165 * t)
                    + .3 * Math.sin(2 * Math.PI * 220 * t)) * (.07 + .025 * Math.sin(2 * Math.PI * t / 8));
                le16(out, (int) (value * 32767));
            }
        }
    }
    private static void le16(DataOutputStream out, int n) throws IOException { out.writeByte(n); out.writeByte(n >> 8); }
    private static void le32(DataOutputStream out, int n) throws IOException { le16(out, n); le16(out, n >> 16); }
}
