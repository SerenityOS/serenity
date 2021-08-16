/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal;

import java.util.EnumSet;

public class MouseEvent {

    public enum Type {
        Released,
        Pressed,
        Wheel,
        Moved,
        Dragged
    }

    public enum Button {
        NoButton,
        Button1,
        Button2,
        Button3,
        WheelUp,
        WheelDown
    }

    public enum Modifier {
        Shift,
        Alt,
        Control
    }

    private final Type type;
    private final Button button;
    private final EnumSet<Modifier> modifiers;
    private final int x;
    private final int y;

    public MouseEvent(Type type, Button button, EnumSet<Modifier> modifiers, int x, int y) {
        this.type = type;
        this.button = button;
        this.modifiers = modifiers;
        this.x = x;
        this.y = y;
    }

    public Type getType() {
        return type;
    }

    public Button getButton() {
        return button;
    }

    public EnumSet<Modifier> getModifiers() {
        return modifiers;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "MouseEvent[" +
                "type=" + type +
                ", button=" + button +
                ", modifiers=" + modifiers +
                ", x=" + x +
                ", y=" + y +
                ']';
    }
}
