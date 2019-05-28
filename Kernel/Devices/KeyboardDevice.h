#pragma once

#include "IRQHandler.h"
#include "KeyCode.h"
#include <AK/CircularQueue.h>
#include <AK/DoublyLinkedList.h>
#include <AK/Types.h>
#include <Kernel/Devices/CharacterDevice.h>

class KeyboardClient;

class KeyboardDevice final : public IRQHandler
    , public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    using Event = KeyEvent;

    [[gnu::pure]] static KeyboardDevice& the();

    virtual ~KeyboardDevice() override;
    KeyboardDevice();

    void set_client(KeyboardClient* client) { m_client = client; }

    // ^CharacterDevice
    virtual ssize_t read(FileDescriptor&, byte* buffer, ssize_t) override;
    virtual bool can_read(FileDescriptor&) const override;
    virtual ssize_t write(FileDescriptor&, const byte* buffer, ssize_t) override;
    virtual bool can_write(FileDescriptor&) const override { return true; }

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "KeyboardDevice"; }

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
    virtual void on_key_pressed(KeyboardDevice::Event) = 0;
};
