/*
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java2d.demos.Images;


import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java2d.Surface;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;


/**
 * Render a filled star & duke into a BufferedImage, save the BufferedImage
 * as a JPEG, display the BufferedImage, using the decoded JPEG BufferedImage
 * display the JPEG flipped BufferedImage.
 */
@SuppressWarnings("serial")
public class JPEGFlip extends Surface {

    private static Image img;

    public JPEGFlip() {
        setBackground(WHITE);
        img = getImage("duke.png");
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        int hh = h / 2;

        BufferedImage bi = new BufferedImage(w, hh, BufferedImage.TYPE_INT_RGB);
        Graphics2D big = bi.createGraphics();

        // .. use rendering hints from J2DCanvas ..
        big.setRenderingHints(g2.getRenderingHints());

        big.setBackground(getBackground());
        big.clearRect(0, 0, w, hh);

        big.setColor(GREEN.darker());
        GeneralPath p = new GeneralPath(Path2D.WIND_NON_ZERO);
        p.moveTo(-w / 2.0f, -hh / 8.0f);
        p.lineTo(+w / 2.0f, -hh / 8.0f);
        p.lineTo(-w / 4.0f, +hh / 2.0f);
        p.lineTo(+0.0f, -hh / 2.0f);
        p.lineTo(+w / 4.0f, +hh / 2.0f);
        p.closePath();
        big.translate(w / 2, hh / 2);
        big.fill(p);

        float scale = 0.09f;
        int iw = (int) (scale * w);
        int ih = (int) (img.getHeight(null) * iw / img.getWidth(null));
        big.drawImage(img, -iw / 2, -ih / 2, iw, ih, this);

        g2.drawImage(bi, 0, 0, this);
        g2.setFont(new Font("Dialog", Font.PLAIN, 10));
        g2.setColor(BLACK);
        g2.drawString("BufferedImage", 4, 12);


        BufferedImage bi1 = null;
        ImageOutputStream ios = null;
        // To write the jpeg to a file uncomment the File* lines and
        // comment out the ByteArray*Stream lines.
        //FileOutputStream out = null;
        ByteArrayOutputStream out = null;
        //FileInputStream in = null;
        ByteArrayInputStream in = null;
        try {
            //File file = new File("images", "test.jpg");
            //out = new FileOutputStream(file);
            out = new ByteArrayOutputStream();
            ios = ImageIO.createImageOutputStream(out);
            ImageWriter encoder =
                    ImageIO.getImageWritersByFormatName("JPEG").next();
            JPEGImageWriteParam param = new JPEGImageWriteParam(null);

            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(1.0f);

            encoder.setOutput(ios);
            encoder.write(null, new IIOImage(bi, null, null), param);

            //in = new FileInputStream(file);
            in = new ByteArrayInputStream(out.toByteArray());
            bi1 = ImageIO.read(in);
        } catch (Exception ex) {
            g2.setColor(RED);
            g2.drawString("Error encoding or decoding the image", 5, hh * 2 - 5);
            return;
        } finally {
            if (ios != null) {
                try {
                    ios.close();
                } catch (IOException ex) {
                    Logger.getLogger(JPEGFlip.class.getName()).log(Level.SEVERE,
                            null, ex);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(JPEGFlip.class.getName()).log(Level.SEVERE,
                            null, ex);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(JPEGFlip.class.getName()).log(Level.SEVERE,
                            null, ex);
                }
            }
        }

        if (bi1 == null) {
            g2.setColor(RED);
            g2.drawString("Error reading the image", 5, hh * 2 - 5);
            return;
        }

        g2.drawImage(bi1, w, hh * 2, -w, -hh, null);

        g2.drawString("JPEGImage Flipped", 4, hh * 2 - 4);
        g2.drawLine(0, hh, w, hh);
    }

    public static void main(String[] s) {
        createDemoFrame(new JPEGFlip());
    }
}
