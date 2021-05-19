/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

#define SERIAL_COM1_ADDR 0x3F8
#define SERIAL_COM2_ADDR 0x2F8
#define SERIAL_COM3_ADDR 0x3E8
#define SERIAL_COM4_ADDR 0x2E8

#define SERIAL_COM1_IRQ 4
#define SERIAL_COM2_IRQ 3
#define SERIAL_COM3_IRQ 4
#define SERIAL_COM4_IRQ 3

class SerialDevice final : public TTY
    , public IRQHandler {
    AK_MAKE_ETERNAL
public:
    SerialDevice(IOAddress base_addr, unsigned minor, u8 irq);
    virtual ~SerialDevice() override;

    void put_char(char);

    enum InterruptEnable : u8 {
        LowPowerMode = 0x01 << 5,
        SleepMode = 0x01 << 4,
        ModemStatusInterrupt = 0x01 << 3,
        ReceiverLineStatusInterrupt = 0x01 << 2,
        TransmitterHoldingRegisterEmptyInterrupt = 0x01 << 1,
        ReceivedDataAvailableInterrupt = 0x01 << 0
    };

    enum Baud : u16 {
        Baud50 = 2304,
        Baud110 = 1047,
        Baud220 = 524,
        Baud300 = 384,
        Baud600 = 192,
        Baud1200 = 96,
        Baud2400 = 48,
        Baud4800 = 24,
        Baud9600 = 12,
        Baud19200 = 6,
        Baud38400 = 3,
        Baud57600 = 2,
        Baud115200 = 1
    };

    enum ParitySelect : u8 {
        None = 0x00 << 3,
        Odd = 0x01 << 3,
        Even = 0x03 << 3,
        Mark = 0x05 << 3,
        Space = 0x07 << 3
    };

    enum StopBits : u8 {
        One = 0x00 << 2,
        Two = 0x01 << 2
    };

    enum WordLength : u8 {
        FiveBits = 0x00,
        SixBits = 0x01,
        SevenBits = 0x02,
        EightBits = 0x03
    };

    enum FIFOControl : u8 {
        EnableFIFO = 0x01 << 0,
        ClearReceiveFIFO = 0x01 << 1,
        ClearTransmitFIFO = 0x01 << 2,
        Enable64ByteFIFO = 0x01 << 5,
        TriggerLevel1 = 0x00 << 6,
        TriggerLevel2 = 0x01 << 6,
        TriggerLevel3 = 0x02 << 6,
        TriggerLevel4 = 0x03 << 6
    };

    enum ModemControl : u8 {
        AutoflowControlEnabled = 0x01 << 5,
        LoopbackMode = 0x01 << 4,
        AuxiliaryOutput2 = 0x01 << 3,
        AuxiliaryOutput1 = 0x01 << 2,
        RequestToSend = 0x01 << 1,
        DataTerminalReady = 0x01 << 0
    };

    enum LineStatus : u8 {
        ErrorInReceivedFIFO = 0x01 << 7,
        EmptyDataHoldingRegisters = 0x01 << 6,
        EmptyTransmitterHoldingRegister = 0x01 << 5,
        BreakInterrupt = 0x01 << 4,
        FramingError = 0x01 << 3,
        ParityError = 0x01 << 2,
        OverrunError = 0x01 << 1,
        DataReady = 0x01 << 0
    };

    // ^File
    virtual bool can_write(const FileDescription&, size_t) const override;

    // ^Device
    virtual mode_t required_mode() const override { return 0620; }
    virtual String device_name() const override;

    // ^TTY
    virtual String const& tty_name() const override { return m_tty_name; }

private:
    // ^TTY
    virtual ssize_t on_tty_write(const UserOrKernelBuffer&, ssize_t) override;
    virtual void echo(u8) override;
    virtual int change_baud(speed_t in_baud, speed_t out_baud) override;
    virtual int change_parity(TTY::ParityMode) override;
    virtual int change_stop_bits(TTY::StopBits) override;
    virtual int change_character_size(TTY::CharacterSize) override;
    virtual int change_receiver_enabled(bool) override;
    virtual int change_ignore_modem_status(bool) override;
    virtual void discard_pending_input() override;
    virtual void discard_pending_output() override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "SerialDevice"; }

    // ^IRQHandler
    virtual void handle_irq(const RegisterState&) override;

    void set_interrupts(u8 irq_mask);
    void set_baud(Baud);
    void set_fifo_control(u8 fifo_control);
    void set_line_control(ParitySelect, StopBits, WordLength);
    void set_break_enable(bool break_enable);
    void set_modem_control(u8 modem_control);
    u8 get_line_status() const;

    String m_tty_name;
    IOAddress m_base_addr;
    bool m_interrupt_enable { false };
    u8 m_fifo_control { 0 };
    Baud m_baud { Baud38400 };
    ParitySelect m_parity_select { None };
    StopBits m_stop_bits { One };
    WordLength m_word_length { EightBits };
    bool m_break_enable { false };
    bool m_ignore_modem_status { false };
    u8 m_modem_control { 0 };
    SpinLock<u8> m_serial_lock;
};

}
