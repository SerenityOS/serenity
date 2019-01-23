#pragma once

#include <Kernel/CharacterDevice.h>
#include "IRQHandler.h"

class PS2MouseDevice final : public IRQHandler, public CharacterDevice {
public:
    PS2MouseDevice();
    virtual ~PS2MouseDevice() override;

    static PS2MouseDevice& the();

    // ^CharacterDevice
    virtual bool can_read(Process&) const override;
    virtual ssize_t read(Process&, byte* buffer, size_t) override;
    virtual ssize_t write(Process&, const byte* buffer, size_t) override;
    virtual bool can_write(Process&) const override { return true; }

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "PS2MouseDevice"; }

    void initialize();
    void prepare_for_input();
    void prepare_for_output();
    void mouse_write(byte);
    byte mouse_read();
    void wait_then_write(byte port, byte data);
    byte wait_then_read(byte port);

    CircularQueue<byte, 600> m_queue;
    byte m_data_state { 0 };
    signed_byte m_data[3];
};
