/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Sections.h>

namespace Kernel {

SerialDevice::SerialDevice(NonnullOwnPtr<IOWindow> registers_io_window, unsigned minor)
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Serial, minor)
    , m_registers_io_window(move(registers_io_window))
{
    set_interrupts(false);
    set_baud(Baud38400);
    set_line_control(None, One, EightBits);
    set_fifo_control(EnableFIFO | ClearReceiveFIFO | ClearTransmitFIFO | TriggerLevel4);
    set_modem_control(RequestToSend | DataTerminalReady);
}

SerialDevice::~SerialDevice() = default;

bool SerialDevice::can_read(OpenFileDescription const&, u64) const
{
    return (get_line_status() & DataReady) != 0;
}

ErrorOr<size_t> SerialDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker lock(m_serial_lock);
    if (!(get_line_status() & DataReady))
        return 0;

    return buffer.write_buffered<128>(size, [&](Bytes bytes) {
        for (auto& byte : bytes)
            byte = m_registers_io_window->read8(0);
        return bytes.size();
    });
}

bool SerialDevice::can_write(OpenFileDescription const&, u64) const
{
    return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
}

ErrorOr<size_t> SerialDevice::write(OpenFileDescription& description, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker lock(m_serial_lock);
    if (!can_write(description, size))
        return EAGAIN;

    return buffer.read_buffered<128>(size, [&](ReadonlyBytes bytes) {
        for (auto const& byte : bytes)
            put_char(byte);
        return bytes.size();
    });
}

void SerialDevice::put_char(char ch)
{
    while ((get_line_status() & EmptyTransmitterHoldingRegister) == 0)
        Processor::wait_check();

    if (ch == '\n' && !m_last_put_char_was_carriage_return)
        m_registers_io_window->write8(0, '\r');

    m_registers_io_window->write8(0, ch);

    m_last_put_char_was_carriage_return = (ch == '\r');
}

void SerialDevice::set_interrupts(bool interrupt_enable)
{
    m_interrupt_enable = interrupt_enable;

    m_registers_io_window->write8(1, interrupt_enable);
}

void SerialDevice::set_baud(Baud baud)
{
    m_baud = baud;

    m_registers_io_window->write8(3, m_registers_io_window->read8(3) | 0x80); // turn on DLAB
    m_registers_io_window->write8(0, ((u8)(baud)) & 0xff);                    // lower half of divisor
    m_registers_io_window->write8(1, ((u8)(baud)) >> 2);                      // lower half of divisor
    m_registers_io_window->write8(3, m_registers_io_window->read8(3) & 0x7f); // turn off DLAB
}

void SerialDevice::set_fifo_control(u8 fifo_control)
{
    m_fifo_control = fifo_control;
    m_registers_io_window->write8(2, fifo_control);
}

void SerialDevice::set_line_control(ParitySelect parity_select, StopBits stop_bits, WordLength word_length)
{
    m_parity_select = parity_select;
    m_stop_bits = stop_bits;
    m_word_length = word_length;

    m_registers_io_window->write8(3, (m_registers_io_window->read8(3) & ~0x3f) | parity_select | stop_bits | word_length);
}

void SerialDevice::set_break_enable(bool break_enable)
{
    m_break_enable = break_enable;

    m_registers_io_window->write8(3, m_registers_io_window->read8(3) & (break_enable ? 0xff : 0xbf));
}

void SerialDevice::set_modem_control(u8 modem_control)
{
    m_modem_control = modem_control;

    m_registers_io_window->write8(4, modem_control);
}

u8 SerialDevice::get_line_status() const
{
    return m_registers_io_window->read8(5);
}

}
