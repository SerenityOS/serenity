#pragma once

#include <VirtualFileSystem/CharacterDevice.h>
#include "IRQHandler.h"

class MouseClient;

class PS2MouseDevice final : public IRQHandler, public CharacterDevice {
public:
    PS2MouseDevice();
    virtual ~PS2MouseDevice() override;

    static PS2MouseDevice& the();

    void set_client(MouseClient* client) { m_client = client; }

private:
    virtual bool has_data_available_for_reading() const override;
    virtual ssize_t read(byte* buffer, size_t buffer_size) override;
    virtual ssize_t write(const byte* buffer, size_t buffer_size) override;

    virtual void handle_irq() override;

    void initialize();
    void prepare_for_input();
    void prepare_for_output();
    void mouse_write(byte);
    byte mouse_read();
    void wait_then_write(byte port, byte data);
    byte wait_then_read(byte port);

    MouseClient* m_client { nullptr };
    byte m_data_state { 0 };
    signed_byte m_data[3];
};

class MouseClient {
public:
    virtual ~MouseClient();
    virtual void did_receive_mouse_data(int dx, int dy, bool left_button, bool right_button) = 0;
};
