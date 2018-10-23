#pragma once

#include <AK/Types.h>
#include <AK/DoublyLinkedList.h>
#include <AK/CircularQueue.h>
#include <VirtualFileSystem/CharacterDevice.h>
#include "IRQHandler.h"

class Keyboard final : public IRQHandler, public CharacterDevice {
public:
    virtual ~Keyboard() override;
    Keyboard();

private:
    // ^IRQHandler
    virtual void handleIRQ() override;

    // ^CharacterDevice
    virtual ssize_t read(byte* buffer, size_t) override;
    virtual ssize_t write(const byte* buffer, size_t) override;

    CircularQueue<byte, 16> m_queue;
    byte m_modifiers { 0 };
};

