/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/IO.h>

namespace Kernel {

UNMAP_AFTER_INIT SerialDevice::SerialDevice(u32 base_addr, unsigned minor)
    : CharacterDevice(4, minor)
    , m_base_addr(base_addr)
{
    initialize();
}

UNMAP_AFTER_INIT SerialDevice::~SerialDevice()
{
}

bool SerialDevice::can_read(const FileDescription&, size_t) const
{
    return (get_line_status() & DataReady) != 0;
}

KResultOr<size_t> SerialDevice::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    if (!(get_line_status() & DataReady))
        return 0;

    return buffer.write_buffered<128>(size, [&](u8* data, size_t data_size) {
        for (size_t i = 0; i < data_size; i++)
            data[i] = IO::in8(m_base_addr);
        return data_size;
    });
}

bool SerialDevice::can_write(const FileDescription&, size_t) const
{
    return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
}

KResultOr<size_t> SerialDevice::write(FileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    if (!(get_line_status() & EmptyTransmitterHoldingRegister))
        return 0;

    return buffer.read_buffered<128>(size, [&](u8 const* data, size_t data_size) {
        for (size_t i = 0; i < data_size; i++)
            IO::out8(m_base_addr, data[i]);
        return data_size;
    });
}

String SerialDevice::device_name() const
{
    return String::formatted("ttyS{}", minor() - 64);
}

UNMAP_AFTER_INIT void SerialDevice::initialize()
{
    set_interrupts(false);
    set_baud(Baud38400);
    set_line_control(None, One, EightBits);
    set_fifo_control(EnableFIFO | ClearReceiveFIFO | ClearTransmitFIFO | TriggerLevel4);
    set_modem_control(RequestToSend | DataTerminalReady);
}

UNMAP_AFTER_INIT void SerialDevice::set_interrupts(bool interrupt_enable)
{
    m_interrupt_enable = interrupt_enable;

    IO::out8(m_base_addr + 1, interrupt_enable);
}

void SerialDevice::set_baud(Baud baud)
{
    m_baud = baud;

    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) | 0x80); // turn on DLAB
    IO::out8(m_base_addr + 0, ((u8)(baud)) & 0xff);             // lower half of divisor
    IO::out8(m_base_addr + 1, ((u8)(baud)) >> 2);               // upper half of divisor
    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) & 0x7f); // turn off DLAB
}

void SerialDevice::set_fifo_control(u8 fifo_control)
{
    m_fifo_control = fifo_control;

    IO::out8(m_base_addr + 2, fifo_control);
}

void SerialDevice::set_line_control(ParitySelect parity_select, StopBits stop_bits, WordLength word_length)
{
    m_parity_select = parity_select;
    m_stop_bits = stop_bits;
    m_word_length = word_length;

    IO::out8(m_base_addr + 3, (IO::in8(m_base_addr + 3) & ~0x3f) | parity_select | stop_bits | word_length);
}

void SerialDevice::set_break_enable(bool break_enable)
{
    m_break_enable = break_enable;

    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) & (break_enable ? 0xff : 0xbf));
}

void SerialDevice::set_modem_control(u8 modem_control)
{
    m_modem_control = modem_control;

    IO::out8(m_base_addr + 4, modem_control);
}

u8 SerialDevice::get_line_status() const
{
    return IO::in8(m_base_addr + 5);
}

}
