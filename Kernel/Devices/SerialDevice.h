/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/IO.h>

namespace Kernel {

#define SERIAL_COM1_ADDR 0x3F8
#define SERIAL_COM2_ADDR 0x2F8
#define SERIAL_COM3_ADDR 0x3E8
#define SERIAL_COM4_ADDR 0x2E8

class SerialDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    SerialDevice(IOAddress base_addr, unsigned minor);
    virtual ~SerialDevice() override;

    // ^CharacterDevice
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;

    void put_char(char);

    enum InterruptEnable {
        LowPowerMode = 0x01 << 5,
        SleepMode = 0x01 << 4,
        ModemStatusInterrupt = 0x01 << 3,
        ReceiverLineStatusInterrupt = 0x01 << 2,
        TransmitterHoldingRegisterEmptyInterrupt = 0x01 << 1,
        ReceivedDataAvailableInterrupt = 0x01 << 0
    };

    enum Baud {
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

    enum ParitySelect {
        None = 0x00 << 3,
        Odd = 0x01 << 3,
        Even = 0x03 << 3,
        Mark = 0x05 << 3,
        Space = 0x07 << 3
    };

    enum StopBits {
        One = 0x00 << 2,
        Two = 0x01 << 2
    };

    enum WordLength {
        FiveBits = 0x00,
        SixBits = 0x01,
        SevenBits = 0x02,
        EightBits = 0x03
    };

    enum FIFOControl {
        EnableFIFO = 0x01 << 0,
        ClearReceiveFIFO = 0x01 << 1,
        ClearTransmitFIFO = 0x01 << 2,
        Enable64ByteFIFO = 0x01 << 5,
        TriggerLevel1 = 0x00 << 6,
        TriggerLevel2 = 0x01 << 6,
        TriggerLevel3 = 0x02 << 6,
        TriggerLevel4 = 0x03 << 6
    };

    enum ModemControl {
        AutoflowControlEnabled = 0x01 << 5,
        LoopbackMode = 0x01 << 4,
        AuxiliaryOutput2 = 0x01 << 3,
        AuxiliaryOutput1 = 0x01 << 2,
        RequestToSend = 0x01 << 1,
        DataTerminalReady = 0x01 << 0
    };

    enum LineStatus {
        ErrorInReceivedFIFO = 0x01 << 7,
        EmptyDataHoldingRegisters = 0x01 << 6,
        EmptyTransmitterHoldingRegister = 0x01 << 5,
        BreakInterrupt = 0x01 << 4,
        FramingError = 0x01 << 3,
        ParityError = 0x01 << 2,
        OverrunError = 0x01 << 1,
        DataReady = 0x01 << 0
    };

    // ^Device
    virtual mode_t required_mode() const override { return 0620; }
    virtual String device_name() const override;

private:
    friend class PCISerialDevice;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "SerialDevice"; }

    void initialize();
    void set_interrupts(bool interrupt_enable);
    void set_baud(Baud);
    void set_fifo_control(u8 fifo_control);
    void set_line_control(ParitySelect, StopBits, WordLength);
    void set_break_enable(bool break_enable);
    void set_modem_control(u8 modem_control);
    u8 get_line_status() const;

    IOAddress m_base_addr;
    bool m_interrupt_enable { false };
    u8 m_fifo_control { 0 };
    Baud m_baud { Baud38400 };
    ParitySelect m_parity_select { None };
    StopBits m_stop_bits { One };
    WordLength m_word_length { EightBits };
    bool m_break_enable { false };
    u8 m_modem_control { 0 };
    bool m_last_put_char_was_carriage_return { false };
    SpinLock<u8> m_serial_lock;
};

}
