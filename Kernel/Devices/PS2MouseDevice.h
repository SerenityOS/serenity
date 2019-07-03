#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/MousePacket.h>

class PS2MouseDevice final : public IRQHandler
    , public CharacterDevice {
public:
    PS2MouseDevice();
    virtual ~PS2MouseDevice() override;

    static PS2MouseDevice& the();

    // ^CharacterDevice
    virtual bool can_read(FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(FileDescription&) const override { return true; }

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "PS2MouseDevice"; }

    void initialize();
    void prepare_for_input();
    void prepare_for_output();
    void mouse_write(u8);
    u8 mouse_read();
    void wait_then_write(u8 port, u8 data);
    u8 wait_then_read(u8 port);
    void parse_data_packet();
    void expect_ack();

    CircularQueue<MousePacket, 100> m_queue;
    u8 m_data_state { 0 };
    u8 m_data[4];
    bool m_has_wheel { false };
};
