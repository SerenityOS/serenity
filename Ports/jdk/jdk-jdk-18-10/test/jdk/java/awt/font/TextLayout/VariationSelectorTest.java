/* @test
 * @summary Verify two identical 'a's are rendered
 * @bug 8187100
 * @ignore Requires a special font installed.
 */
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

public class VariationSelectorTest {
    // A font supporting Unicode variation selectors is required
    // At least DejaVu 2.20 from 2007
    private static final Font FONT = new Font("DejaVu Sans", Font.PLAIN, 12);

    public static void main(String[] args) {
        final String fontName = FONT.getFontName();
        if (!fontName.equals("DejaVuSans")) {
            System.err.println("*** Warning: Font DejaVuSans not installed.");
            System.err.println("*** Using font: " + fontName);
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.add(new MyComponent());
            frame.setSize(200, 200);
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        });
    }

    private static class MyComponent extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            FontRenderContext frc = g2d.getFontRenderContext();
            String text = "a";
            GlyphVector gv = FONT.layoutGlyphVector(
                                 frc, text.toCharArray(), 0, text.length(),
                                 Font.LAYOUT_LEFT_TO_RIGHT);
            System.out.println("'a'=" + gv.getNumGlyphs());
            g2d.drawString("=" + gv.getNumGlyphs() + " ('a')", 100, 50);
            g2d.drawGlyphVector(gv, 80, 50);
            String text2 = "a\ufe00";
            GlyphVector gv2 = FONT.layoutGlyphVector(
                                 frc, text2.toCharArray(), 0, text2.length(),
                                 Font.LAYOUT_LEFT_TO_RIGHT);
            g2d.drawGlyphVector(gv2, 80, 100);
            System.out.println("'a'+VS=" + gv2.getNumGlyphs());
            g2d.drawString("=" + gv2.getNumGlyphs() + " ('a'+VS)", 100, 100);
            if ((gv.getNumGlyphs() == 1) && (gv2.getNumGlyphs() == 1)) {
                System.out.println("PASS");
                g2d.drawString("PASS", 10, 15);
            } else {
                System.err.println("FAIL");
                g2d.drawString("FAIL", 10, 15);
            }
        }
    }
}
