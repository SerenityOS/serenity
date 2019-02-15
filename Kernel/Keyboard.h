#pragma once

#include <AK/Types.h>
#include <AK/DoublyLinkedList.h>
#include <AK/CircularQueue.h>
#include <Kernel/CharacterDevice.h>
#include "IRQHandler.h"
#include "KeyCode.h"

class KeyboardClient;

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

    [[gnu::pure]] static Keyboard& the();

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
