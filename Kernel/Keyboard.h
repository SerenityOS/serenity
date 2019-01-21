#pragma once

#include <AK/Types.h>
#include <AK/DoublyLinkedList.h>
#include <AK/CircularQueue.h>
#include <VirtualFileSystem/CharacterDevice.h>
#include "IRQHandler.h"

class KeyboardClient;

enum KeyCode : byte {
    Key_Invalid = 0,
    Key_Escape,
    Key_Tab,
    Key_Backspace,
    Key_Return,
    Key_Insert,
    Key_Delete,
    Key_PrintScreen,
    Key_SysRq,
    Key_Home,
    Key_End,
    Key_Left,
    Key_Up,
    Key_Right,
    Key_Down,
    Key_PageUp,
    Key_PageDown,
    Key_Shift,
    Key_Control,
    Key_Alt,
    Key_CapsLock,
    Key_NumLock,
    Key_ScrollLock,
    Key_F1,
    Key_F2,
    Key_F3,
    Key_F4,
    Key_F5,
    Key_F6,
    Key_F7,
    Key_F8,
    Key_F9,
    Key_F10,
    Key_F11,
    Key_F12,
    Key_Space,
    Key_ExclamationPoint,
    Key_DoubleQuote,
    Key_Hashtag,
    Key_Dollar,
    Key_Percent,
    Key_Ampersand,
    Key_Apostrophe,
    Key_LeftParen,
    Key_RightParen,
    Key_Asterisk,
    Key_Plus,
    Key_Comma,
    Key_Minus,
    Key_Period,
    Key_Slash,
    Key_0,
    Key_1,
    Key_2,
    Key_3,
    Key_4,
    Key_5,
    Key_6,
    Key_7,
    Key_8,
    Key_9,
    Key_Colon,
    Key_Semicolon,
    Key_LessThan,
    Key_Equal,
    Key_GreaterThan,
    Key_QuestionMark,
    Key_AtSign,
    Key_A,
    Key_B,
    Key_C,
    Key_D,
    Key_E,
    Key_F,
    Key_G,
    Key_H,
    Key_I,
    Key_J,
    Key_K,
    Key_L,
    Key_M,
    Key_N,
    Key_O,
    Key_P,
    Key_Q,
    Key_R,
    Key_S,
    Key_T,
    Key_U,
    Key_V,
    Key_W,
    Key_X,
    Key_Y,
    Key_Z,
    Key_LeftBracket,
    Key_RightBracket,
    Key_Backslash,
    Key_Circumflex,
    Key_Underscore,
    Key_LeftBrace,
    Key_RightBrace,
    Key_Pipe,
    Key_Tilde,
    Key_Backtick,
};

class Keyboard final : public IRQHandler, public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    enum Modifier {
        Mod_Alt = 0x01,
        Mod_Ctrl = 0x02,
        Mod_Shift = 0x04,
        Is_Press = 0x80,
    };

    struct Event {
        KeyCode key { Key_Invalid };
        byte character { 0 };
        byte flags { 0 };
        bool alt() const { return flags & Mod_Alt; }
        bool ctrl() const { return flags & Mod_Ctrl; }
        bool shift() const { return flags & Mod_Shift; }
        bool is_press() const { return flags & Is_Press; }
    };

    static Keyboard& the() PURE;

    virtual ~Keyboard() override;
    Keyboard();

    void set_client(KeyboardClient* client) { m_client = client; }

    // ^CharacterDevice
    virtual ssize_t read(Process&, byte* buffer, size_t) override;
    virtual bool can_read(Process&) const override;
    virtual ssize_t write(Process&, const byte* buffer, size_t) override;
    virtual bool can_write(Process&) const override { return true; }

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "Keyboard"; }

    void key_state_changed(byte raw, bool pressed);
    void update_modifier(byte modifier, bool state)
    {
        if (state)
            m_modifiers |= modifier;
        else
            m_modifiers &= ~modifier;
    }

    KeyboardClient* m_client { nullptr };
    CircularQueue<Event, 16> m_queue;
    byte m_modifiers { 0 };
};

class KeyboardClient {
public:
    virtual ~KeyboardClient();
    virtual void on_key_pressed(Keyboard::Event) = 0;
};
