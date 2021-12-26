#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/MousePacket.h>
#include <Kernel/IRQHandler.h>

class PS2MouseDevice final : public IRQHandler, public CharacterDevice {
public:
    PS2MouseDevice();
    virtual ~PS2MouseDevice() override;

    static PS2MouseDevice& the();

    // ^CharacterDevice
    virtual bool can_read(Process&) const override;
    virtual ssize_t read(Process&, byte*, ssize_t) override;
    virtual ssize_t write(Process&, const byte*, ssize_t) override;
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
    void parse_data_packet();

    CircularQueue<MousePacket, 100> m_queue;
    byte m_data_state { 0 };
    byte m_data[3];
};
