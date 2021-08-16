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
package java2d.demos.Arcs_Curves;


import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
import java2d.AnimatingSurface;
import static java.awt.Color.*;


/**
 * Arc2D Open, Chord & Pie arcs; Animated Pie Arc.
 */
@SuppressWarnings("serial")
public class Arcs extends AnimatingSurface {

    private static String[] types = { "Arc2D.OPEN", "Arc2D.CHORD", "Arc2D.PIE" };
    private static final int CLOSE = 0;
    private static final int OPEN = 1;
    private static final int FORWARD = 0;
    private static final int BACKWARD = 1;
    private static final int DOWN = 2;
    private static final int UP = 3;
    private int aw, ah; // animated arc width & height
    private int x, y;
    private int angleStart = 45;
    private int angleExtent = 270;
    private int mouth = CLOSE;
    private int direction = FORWARD;

    public Arcs() {
        setBackground(WHITE);
    }

    @Override
    public void reset(int w, int h) {
        x = 0;
        y = 0;
        aw = w / 12;
        ah = h / 12;
    }

    @Override
    public void step(int w, int h) {
        // Compute direction
        if (x + aw >= w - 5 && direction == FORWARD) {
            direction = DOWN;
        }
        if (y + ah >= h - 5 && direction == DOWN) {
            direction = BACKWARD;
        }
        if (x - aw <= 5 && direction == BACKWARD) {
            direction = UP;
        }
        if (y - ah <= 5 && direction == UP) {
            direction = FORWARD;
        }

        // compute angle start & extent
        if (mouth == CLOSE) {
            angleStart -= 5;
            angleExtent += 10;
        }
        if (mouth == OPEN) {
            angleStart += 5;
            angleExtent -= 10;
        }
        if (direction == FORWARD) {
            x += 5;
            y = 0;
        }
        if (direction == DOWN) {
            x = w;
            y += 5;
        }
        if (direction == BACKWARD) {
            x -= 5;
            y = h;
        }
        if (direction == UP) {
            x = 0;
            y -= 5;
        }
        if (angleStart == 0) {
            mouth = OPEN;
        }
        if (angleStart > 45) {
            mouth = CLOSE;
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        // Draw Arcs
        g2.setStroke(new BasicStroke(5.0f));
        for (int i = 0; i < types.length; i++) {
            Arc2D arc = new Arc2D.Float(i);
            arc.setFrame((i + 1) * w * .2, (i + 1) * h * .2, w * .17, h * .17);
            arc.setAngleStart(45);
            arc.setAngleExtent(270);
            g2.setColor(BLUE);
            g2.draw(arc);
            g2.setColor(GRAY);
            g2.fill(arc);
            g2.setColor(BLACK);
            g2.drawString(types[i], (int) ((i + 1) * w * .2), (int) ((i + 1) * h
                    * .2 - 3));
        }

        // Draw Animated Pie Arc
        Arc2D pieArc = new Arc2D.Float(Arc2D.PIE);
        pieArc.setFrame(0, 0, aw, ah);
        pieArc.setAngleStart(angleStart);
        pieArc.setAngleExtent(angleExtent);
        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        switch (direction) {
            case DOWN:
                at.rotate(Math.toRadians(90));
                break;
            case BACKWARD:
                at.rotate(Math.toRadians(180));
                break;
            case UP:
                at.rotate(Math.toRadians(270));
        }
        g2.setColor(BLUE);
        g2.fill(at.createTransformedShape(pieArc));
    }

    public static void main(String[] argv) {
        createDemoFrame(new Arcs());
    }
}
