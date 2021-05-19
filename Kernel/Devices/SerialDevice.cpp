/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/IO.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

UNMAP_AFTER_INIT SerialDevice::SerialDevice(IOAddress base_addr, unsigned minor)
    : TTY(4, minor)
    , m_tty_name(String::formatted("/dev/ttyS{}", minor - 64))
    , m_base_addr(base_addr)
{
    set_interrupts(0);
    set_fifo_control(EnableFIFO | ClearReceiveFIFO | ClearTransmitFIFO | TriggerLevel4);
    // FIXME: TTY currently knows nothing about modem control
    set_modem_control(RequestToSend | DataTerminalReady);
    TTY::load_default_settings();
}

UNMAP_AFTER_INIT SerialDevice::~SerialDevice()
{
}

void SerialDevice::put_char(char ch)
{
    while ((get_line_status() & EmptyTransmitterHoldingRegister) == 0)
        ;
    m_base_addr.out<u8>(ch);
}

String SerialDevice::device_name() const
{
    return String::formatted("ttyS{}", minor() - 64);
}

void SerialDevice::set_interrupts(u8 interrupt_mask)
{
    m_base_addr.offset(1).out<u8>(interrupt_mask);
}

void SerialDevice::set_baud(Baud baud)
{
    m_baud = baud;

    m_base_addr.offset(3).out<u8>(m_base_addr.offset(3).in<u8>() | 0x80); // turn on DLAB
    m_base_addr.out<u8>(((u16)(baud)) & 0xff);                            // lower half of divisor
    m_base_addr.offset(1).out<u8>(((u16)(baud)) >> 8);                    // upper half of divisor
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

ssize_t SerialDevice::on_tty_write(const UserOrKernelBuffer& buffer, ssize_t size)
{
    if (!size)
        return 0;

    ScopedSpinLock lock(m_serial_lock);
    if (!(get_line_status() & EmptyTransmitterHoldingRegister))
        return -EAGAIN;

    auto result = buffer.read_buffered<128>(size, [&](u8 const* data, size_t data_size) {
        for (size_t i = 0; i < data_size; i++)
            put_char(data[i]);
        return data_size;
    });
    if (result.is_error())
        return result.error();
    return (ssize_t)result.value();
}

void SerialDevice::echo(u8 ch)
{
    put_char(ch);
}

void SerialDevice::discard_pending_input()
{
    set_fifo_control(EnableFIFO | ClearReceiveFIFO | TriggerLevel4);
}

void SerialDevice::discard_pending_output()
{
    set_fifo_control(EnableFIFO | ClearTransmitFIFO | TriggerLevel4);
}

int SerialDevice::change_baud(speed_t in_baud, speed_t out_baud)
{
    if (in_baud != out_baud) {
        dbgln("{}: Input and output speed must be the same", m_tty_name);
        return -ENOTSUP;
    }
    switch (in_baud) {
    case B50:
        set_baud(Baud::Baud50);
        break;
    case B110:
        set_baud(Baud::Baud110);
        break;
    case B300:
        set_baud(Baud::Baud300);
        break;
    case B600:
        set_baud(Baud::Baud600);
        break;
    case B1200:
        set_baud(Baud::Baud1200);
        break;
    case B2400:
        set_baud(Baud::Baud2400);
        break;
    case B4800:
        set_baud(Baud::Baud4800);
        break;
    case B9600:
        set_baud(Baud::Baud9600);
        break;
    case B19200:
        set_baud(Baud::Baud19200);
        break;
    case B38400:
        set_baud(Baud::Baud38400);
        break;
    case B57600:
        set_baud(Baud::Baud57600);
        break;
    case B115200:
        set_baud(Baud::Baud115200);
        break;
    default:
        dbgln("{}: Tried to set unsupported baud speed", m_tty_name);
        return -EINVAL;
    }
    return 0;
}

int SerialDevice::change_parity(TTY::ParityMode parity_mode)
{
    switch (parity_mode) {
    case TTY::ParityMode::None:
        set_line_control(ParitySelect::None, m_stop_bits, m_word_length);
        break;
    case TTY::ParityMode::Odd:
        set_line_control(ParitySelect::Odd, m_stop_bits, m_word_length);
        break;
    case TTY::ParityMode::Even:
        set_line_control(ParitySelect::Even, m_stop_bits, m_word_length);
        break;
    }
    return 0;
}

int SerialDevice::change_stop_bits(TTY::StopBits stop_bits)
{
    switch (stop_bits) {
    case TTY::StopBits::One:
        set_line_control(m_parity_select, StopBits::One, m_word_length);
        break;
    case TTY::StopBits::Two:
        set_line_control(m_parity_select, StopBits::Two, m_word_length);
        break;
    }
    return 0;
}

int SerialDevice::change_character_size(TTY::CharacterSize character_size)
{
    switch (character_size) {
    case TTY::CharacterSize::FiveBits:
        set_line_control(m_parity_select, m_stop_bits, WordLength::FiveBits);
        break;
    case TTY::CharacterSize::SixBits:
        set_line_control(m_parity_select, m_stop_bits, WordLength::SixBits);
        break;
    case TTY::CharacterSize::SevenBits:
        set_line_control(m_parity_select, m_stop_bits, WordLength::SevenBits);
        break;
    case TTY::CharacterSize::EightBits:
        set_line_control(m_parity_select, m_stop_bits, WordLength::EightBits);
        break;
    }
    return 0;
}

int SerialDevice::change_receiver_enabled(bool)
{
    // TODO: implement this once we can receive data
    return 0;
}

int SerialDevice::change_ignore_modem_status(bool should_enable)
{
    m_ignore_modem_status = should_enable;
    return 0;
}

}
