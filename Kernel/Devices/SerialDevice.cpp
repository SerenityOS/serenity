/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/IO.h>

namespace Kernel {

UNMAP_AFTER_INIT SerialDevice::SerialDevice(IOAddress base_addr, unsigned minor)
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

    ScopedSpinLock lock(m_serial_lock);
    if (!(get_line_status() & DataReady))
        return 0;

    return buffer.write_buffered<128>(size, [&](u8* data, size_t data_size) {
        for (size_t i = 0; i < data_size; i++)
            data[i] = m_base_addr.in<u8>();
        return data_size;
    });
}

bool SerialDevice::can_write(const FileDescription&, size_t) const
{
    return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
}

KResultOr<size_t> SerialDevice::write(FileDescription& description, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    ScopedSpinLock lock(m_serial_lock);
    if (!can_write(description, size))
        return EAGAIN;

    return buffer.read_buffered<128>(size, [&](u8 const* data, size_t data_size) {
        for (size_t i = 0; i < data_size; i++)
            put_char(data[i]);
        return data_size;
    });
}

void SerialDevice::put_char(char ch)
{
    while ((get_line_status() & EmptyTransmitterHoldingRegister) == 0)
        ;

    if (ch == '\n' && !m_last_put_char_was_carriage_return)
        m_base_addr.out<u8>('\r');

    m_base_addr.out<u8>(ch);

    m_last_put_char_was_carriage_return = (ch == '\r');
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

    m_base_addr.offset(1).out<u8>(interrupt_enable);
}

void SerialDevice::set_baud(Baud baud)
{
    m_baud = baud;

    m_base_addr.offset(3).out<u8>(m_base_addr.offset(3).in<u8>() | 0x80); // turn on DLAB
    m_base_addr.out<u8>(((u8)(baud)) & 0xff);                             // lower half of divisor
    m_base_addr.offset(1).out<u8>(((u8)(baud)) >> 2);                     // upper half of divisor
    m_base_addr.offset(3).out<u8>(m_base_addr.offset(3).in<u8>() & 0x7f); // turn off DLAB
}

void SerialDevice::set_fifo_control(u8 fifo_control)
{
    m_fifo_control = fifo_control;

    m_base_addr.offset(2).out<u8>(fifo_control);
}

void SerialDevice::set_line_control(ParitySelect parity_select, StopBits stop_bits, WordLength word_length)
{
    m_parity_select = parity_select;
    m_stop_bits = stop_bits;
    m_word_length = word_length;

    m_base_addr.offset(3).out<u8>((m_base_addr.offset(3).in<u8>() & ~0x3f) | parity_select | stop_bits | word_length);
}

void SerialDevice::set_break_enable(bool break_enable)
{
    m_break_enable = break_enable;

    m_base_addr.offset(3).out<u8>(m_base_addr.offset(3).in<u8>() & (break_enable ? 0xff : 0xbf));
}

void SerialDevice::set_modem_control(u8 modem_control)
{
    m_modem_control = modem_control;

    m_base_addr.offset(4).out<u8>(modem_control);
}

u8 SerialDevice::get_line_status() const
{
    return m_base_addr.offset(5).in<u8>();
}

}
