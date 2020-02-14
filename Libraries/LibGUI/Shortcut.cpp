/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/Shortcut.h>

namespace GUI {

static String key_code_to_string(KeyCode key)
{
    switch (key) {
    case Key_Escape:
        return "Escape";
    case Key_Tab:
        return "Tab";
    case Key_Backspace:
        return "Backspace";
    case Key_Return:
        return "Return";
    case Key_Insert:
        return "Insert";
    case Key_Delete:
        return "Delete";
    case Key_PrintScreen:
        return "PrintScreen";
    case Key_SysRq:
        return "SysRq";
    case Key_Home:
        return "Home";
    case Key_End:
        return "End";
    case Key_Left:
        return "Left";
    case Key_Up:
        return "Up";
    case Key_Right:
        return "Right";
    case Key_Down:
        return "Down";
    case Key_PageUp:
        return "PageUp";
    case Key_PageDown:
        return "PageDown";
    case Key_Shift:
        return "Shift";
    case Key_Control:
        return "Control";
    case Key_Alt:
        return "Alt";
    case Key_CapsLock:
        return "CapsLock";
    case Key_NumLock:
        return "NumLock";
    case Key_ScrollLock:
        return "ScrollLock";
    case Key_F1:
        return "F1";
    case Key_F2:
        return "F2";
    case Key_F3:
        return "F3";
    case Key_F4:
        return "F4";
    case Key_F5:
        return "F5";
    case Key_F6:
        return "F6";
    case Key_F7:
        return "F7";
    case Key_F8:
        return "F8";
    case Key_F9:
        return "F9";
    case Key_F10:
        return "F10";
    case Key_F11:
        return "F11";
    case Key_F12:
        return "F12";
    case Key_Space:
        return "Space";
    case Key_ExclamationPoint:
        return "!";
    case Key_DoubleQuote:
        return "\"";
    case Key_Hashtag:
        return "#";
    case Key_Dollar:
        return "$";
    case Key_Percent:
        return "%";
    case Key_Ampersand:
        return "&";
    case Key_Apostrophe:
        return "'";
    case Key_LeftParen:
        return "(";
    case Key_RightParen:
        return ")";
    case Key_Asterisk:
        return "*";
    case Key_Plus:
        return "+";
    case Key_Comma:
        return ",";
    case Key_Minus:
        return "-";
    case Key_Period:
        return ",";
    case Key_Slash:
        return "/";
    case Key_0:
        return "0";
    case Key_1:
        return "1";
    case Key_2:
        return "2";
    case Key_3:
        return "3";
    case Key_4:
        return "4";
    case Key_5:
        return "5";
    case Key_6:
        return "6";
    case Key_7:
        return "7";
    case Key_8:
        return "8";
    case Key_9:
        return "9";
    case Key_Colon:
        return ":";
    case Key_Semicolon:
        return ";";
    case Key_LessThan:
        return "<";
    case Key_Equal:
        return "=";
    case Key_GreaterThan:
        return ">";
    case Key_QuestionMark:
        return "?";
    case Key_AtSign:
        return "@";
    case Key_A:
        return "A";
    case Key_B:
        return "B";
    case Key_C:
        return "C";
    case Key_D:
        return "D";
    case Key_E:
        return "E";
    case Key_F:
        return "F";
    case Key_G:
        return "G";
    case Key_H:
        return "H";
    case Key_I:
        return "I";
    case Key_J:
        return "J";
    case Key_K:
        return "K";
    case Key_L:
        return "L";
    case Key_M:
        return "M";
    case Key_N:
        return "N";
    case Key_O:
        return "O";
    case Key_P:
        return "P";
    case Key_Q:
        return "Q";
    case Key_R:
        return "R";
    case Key_S:
        return "S";
    case Key_T:
        return "T";
    case Key_U:
        return "U";
    case Key_V:
        return "V";
    case Key_W:
        return "W";
    case Key_X:
        return "X";
    case Key_Y:
        return "Y";
    case Key_Z:
        return "Z";
    case Key_LeftBracket:
        return "[";
    case Key_RightBracket:
        return "]";
    case Key_Backslash:
        return "\\";
    case Key_Circumflex:
        return "^";
    case Key_Underscore:
        return "_";
    case Key_LeftBrace:
        return "{";
    case Key_RightBrace:
        return "}";
    case Key_Pipe:
        return "|";
    case Key_Tilde:
        return "~";
    case Key_Backtick:
        return "`";

    case Key_Invalid:
        return "Invalid";
    default:
        ASSERT_NOT_REACHED();
    }
}

String Shortcut::to_string() const
{
    Vector<String, 8> parts;

    if (m_modifiers & Mod_Ctrl)
        parts.append("Ctrl");
    if (m_modifiers & Mod_Shift)
        parts.append("Shift");
    if (m_modifiers & Mod_Alt)
        parts.append("Alt");
    if (m_modifiers & Mod_Logo)
        parts.append("Logo");

    parts.append(key_code_to_string(m_key));

    StringBuilder builder;
    for (int i = 0; i < parts.size(); ++i) {
        builder.append(parts[i]);
        if (i != parts.size() - 1)
            builder.append('+');
    }
    return builder.to_string();
}

}
