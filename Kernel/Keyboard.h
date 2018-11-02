#pragma once

#include <AK/Types.h>
#include <AK/DoublyLinkedList.h>
#include <AK/CircularQueue.h>
#include <VirtualFileSystem/CharacterDevice.h>
#include "IRQHandler.h"

class KeyboardClient;

class Keyboard final : public IRQHandler, public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    enum Modifier {
        Mod_Alt = 0x01,
        Mod_Ctrl = 0x02,
        Mod_Shift = 0x04,
    };

    struct Key {
        byte character { 0 };
        byte modifiers { 0 };
        bool alt() { return modifiers & Mod_Alt; }
        bool ctrl() { return modifiers & Mod_Ctrl; }
        bool shift() { return modifiers & Mod_Shift; }
    };

    static Keyboard& the() PURE;

    virtual ~Keyboard() override;
    Keyboard();

    void setClient(KeyboardClient* client) { m_client = client; }

private:
    // ^IRQHandler
    virtual void handleIRQ() override;

    // ^CharacterDevice
    virtual ssize_t read(byte* buffer, size_t) override;
    virtual ssize_t write(const byte* buffer, size_t) override;
    virtual bool hasDataAvailableForRead() const override;

    void emit(byte);

    KeyboardClient* m_client { nullptr };
    CircularQueue<Key, 16> m_queue;
    byte m_modifiers { 0 };
};

class KeyboardClient {
public:
    virtual ~KeyboardClient();
    virtual void onKeyPress(Keyboard::Key) = 0;
};
