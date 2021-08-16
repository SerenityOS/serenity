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
package java2d.demos.Colors;


import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java2d.Surface;


/**
 * Creating colors with an alpha value.
 */
@SuppressWarnings("serial")
public class BullsEye extends Surface {

    public BullsEye() {
        setBackground(WHITE);
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        Color[] reds = { RED.darker(), RED };
        for (int N = 0; N < 18; N++) {
            float i = (N + 2) / 2.0f;
            float x = (5 + i * (w / 2 / 10));
            float y = (5 + i * (h / 2 / 10));
            float ew = (w - 10) - (i * w / 10);
            float eh = (h - 10) - (i * h / 10);
            float alpha = (N == 0) ? 0.1f : 1.0f / (19.0f - N);
            if (N >= 16) {
                g2.setColor(reds[N - 16]);
            } else {
                g2.setColor(new Color(0f, 0f, 0f, alpha));
            }
            g2.fill(new Ellipse2D.Float(x, y, ew, eh));
        }
    }

    public static void main(String[] s) {
        createDemoFrame(new BullsEye());
    }
}
