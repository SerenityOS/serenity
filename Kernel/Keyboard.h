#pragma once

#include <AK/Types.h>
#include <AK/DoublyLinkedList.h>
#include <AK/CircularQueue.h>
#include <VirtualFileSystem/CharacterDevice.h>
#include "IRQHandler.h"

class KeyboardClient {
public:
    virtual ~KeyboardClient();
    virtual void onKeyPress(byte) = 0;
};

class Keyboard final : public IRQHandler, public CharacterDevice {
public:
    static Keyboard& the() PURE;

    virtual ~Keyboard() override;
    Keyboard();

    void setClient(KeyboardClient*);

private:
    // ^IRQHandler
    virtual void handleIRQ() override;

    // ^CharacterDevice
    virtual ssize_t read(byte* buffer, size_t) override;
    virtual ssize_t write(const byte* buffer, size_t) override;
    virtual bool hasDataAvailableForRead() const override;

    KeyboardClient* m_client { nullptr };
    CircularQueue<byte, 16> m_queue;
    byte m_modifiers { 0 };
};

