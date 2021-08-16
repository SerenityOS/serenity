/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package apple.laf;

import apple.laf.JRSUIConstants.*;

@SuppressWarnings("unchecked")
public class JRSUIState {
//    static HashSet<JRSUIState> states = new HashSet<JRSUIState>();

    final long encodedState;
    long derivedEncodedState;

    static JRSUIState prototype = new JRSUIState(0);
    public static JRSUIState getInstance() {
        return prototype.derive();
    }

    JRSUIState(final Widget widget) {
        this(widget.apply(0));
    }

    JRSUIState(final long encodedState) {
        this.encodedState = derivedEncodedState = encodedState;
    }

    boolean isDerivationSame() {
        return encodedState == derivedEncodedState;
    }

    public <T extends JRSUIState> T derive() {
        if (isDerivationSame()) return (T)this;
        final T derivation = (T)createDerivation();

//        if (!states.add(derivation)) {
//            System.out.println("dupe: " + states.size());
//        }

        return derivation;
    }

    public <T extends JRSUIState> T createDerivation() {
        return (T)new JRSUIState(derivedEncodedState);
    }

    public void reset() {
        derivedEncodedState = encodedState;
    }

    public void set(final Property property) {
        derivedEncodedState = property.apply(derivedEncodedState);
    }

    public void apply(final JRSUIControl control) {
        control.setEncodedState(encodedState);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof JRSUIState)) return false;
        return encodedState == ((JRSUIState)obj).encodedState && getClass().equals(obj.getClass());
    }

    public boolean is(Property property) {
        return (byte)((derivedEncodedState & property.encoding.mask) >> property.encoding.shift) == property.ordinal;
    }

    @Override
    public int hashCode() {
        return (int)(encodedState ^ (encodedState >>> 32)) ^ getClass().hashCode();
    }

    public static class AnimationFrameState extends JRSUIState {
        final int animationFrame;
        int derivedAnimationFrame;

        AnimationFrameState(final long encodedState, final int animationFrame) {
            super(encodedState);
            this.animationFrame = derivedAnimationFrame = animationFrame;
        }

        @Override
        boolean isDerivationSame() {
            return super.isDerivationSame() && (animationFrame == derivedAnimationFrame);
        }

        @Override
        public <T extends JRSUIState> T createDerivation() {
            return (T)new AnimationFrameState(derivedEncodedState, derivedAnimationFrame);
        }

        @Override
        public void reset() {
            super.reset();
            derivedAnimationFrame = animationFrame;
        }

        public void setAnimationFrame(final int frame) {
            this.derivedAnimationFrame = frame;
        }

        @Override
        public void apply(final JRSUIControl control) {
            super.apply(control);
            control.set(Key.ANIMATION_FRAME, animationFrame);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof AnimationFrameState)) return false;
            return animationFrame == ((AnimationFrameState)obj).animationFrame && super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ animationFrame;
        }
    }

    public static class ValueState extends JRSUIState {
        final double value;
        double derivedValue;

        ValueState(final long encodedState, final double value) {
            super(encodedState);
            this.value = derivedValue = value;
        }

        @Override
        boolean isDerivationSame() {
            return super.isDerivationSame() && (value == derivedValue);
        }

        @Override
        public <T extends JRSUIState> T createDerivation() {
            return (T)new ValueState(derivedEncodedState, derivedValue);
        }

        @Override
        public void reset() {
            super.reset();
            derivedValue = value;
        }

        public void setValue(final double value) {
            derivedValue = value;
        }

        @Override
        public void apply(final JRSUIControl control) {
            super.apply(control);
            control.set(Key.VALUE, value);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof ValueState)) return false;
            return value == ((ValueState)obj).value && super.equals(obj);
        }

        @Override
        public int hashCode() {
            final long bits = Double.doubleToRawLongBits(value);
            return super.hashCode() ^ (int)bits ^ (int)(bits >>> 32);
        }
    }

    public static class TitleBarHeightState extends ValueState {
        TitleBarHeightState(final long encodedState, final double value) {
            super(encodedState, value);
        }

        @Override
        public <T extends JRSUIState> T createDerivation() {
            return (T)new TitleBarHeightState(derivedEncodedState, derivedValue);
        }

        @Override
        public void apply(final JRSUIControl control) {
            super.apply(control);
            control.set(Key.WINDOW_TITLE_BAR_HEIGHT, value);
        }
    }

    public static class ScrollBarState extends ValueState {
        final double thumbProportion;
        double derivedThumbProportion;
        final double thumbStart;
        double derivedThumbStart;

        ScrollBarState(final long encodedState, final double value, final double thumbProportion, final double thumbStart) {
            super(encodedState, value);
            this.thumbProportion = derivedThumbProportion = thumbProportion;
            this.thumbStart = derivedThumbStart = thumbStart;
        }

        @Override
        boolean isDerivationSame() {
            return super.isDerivationSame() && (thumbProportion == derivedThumbProportion) && (thumbStart == derivedThumbStart);
        }

        @Override
        public <T extends JRSUIState> T createDerivation() {
            return (T)new ScrollBarState(derivedEncodedState, derivedValue, derivedThumbProportion, derivedThumbStart);
        }

        @Override
        public void reset() {
            super.reset();
            derivedThumbProportion = thumbProportion;
            derivedThumbStart = thumbStart;
        }

        public void setThumbPercent(final double thumbPercent) {
            derivedThumbProportion = thumbPercent;
        }

        public void setThumbStart(final double thumbStart) {
            derivedThumbStart = thumbStart;
        }

        @Override
        public void apply(final JRSUIControl control) {
            super.apply(control);
            control.set(Key.THUMB_PROPORTION, thumbProportion);
            control.set(Key.THUMB_START, thumbStart);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof ScrollBarState)) return false;
            return (thumbProportion == ((ScrollBarState)obj).thumbProportion) && (thumbStart == ((ScrollBarState)obj).thumbStart) && super.equals(obj);
        }

        @Override
        public int hashCode() {
            final long bits = Double.doubleToRawLongBits(thumbProportion) ^ Double.doubleToRawLongBits(thumbStart);
            return super.hashCode() ^ (int)bits ^ (int)(bits >>> 32);
        }
    }
}
