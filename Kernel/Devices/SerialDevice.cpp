/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/IO.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

UNMAP_AFTER_INIT SerialDevice::SerialDevice(IOAddress base_addr, unsigned minor, u8 irq, termios initial_termios)
    : TTY(4, minor, initial_termios)
    , IRQHandler(irq)
    , m_tty_name(String::formatted("/dev/ttyS{}", minor - 64))
    , m_base_addr(base_addr)
{
    set_interrupts(ReceivedDataAvailableInterrupt);
    set_fifo_control(EnableFIFO | ClearReceiveFIFO | ClearTransmitFIFO | TriggerLevel4);
    // FIXME: TTY currently knows nothing about modem control
    set_modem_control(RequestToSend | DataTerminalReady);
    reload_current_termios();
    enable_irq();
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

bool SerialDevice::can_write(const FileDescription&, size_t) const
{
    return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
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

void SerialDevice::wait_until_pending_output_completes()
{
    while ((get_line_status() & EmptyDataHoldingRegisters) == 0)
        ;
}

int SerialDevice::change_baud(speed_t in_baud, speed_t out_baud)
{
    dbgln_if(SERIAL_DEVICE_DEBUG, "{}: set baud in={:x} out={:x}", m_tty_name, in_baud, out_baud);
    if (in_baud != out_baud) {
        dbgln("{}: Input and output speed must be the same", m_tty_name);
        return -ENOTSUP;
    }

    auto maybe_baud = serial_baud_from_termios(in_baud);
    if (!maybe_baud.has_value()) {
        dbgln("{}: Tried to set unsupported baud speed", m_tty_name);
        return -ENOTSUP;
    }

    dbgln_if(SERIAL_DEVICE_DEBUG, "{}: baud rate divisor={}", m_tty_name, (u16)maybe_baud.value());
    set_baud(maybe_baud.value());
    return 0;
}

Optional<speed_t> SerialDevice::termios_baud_from_serial(Baud baud)
{
    switch (baud) {
    case Baud::Baud50:
        return B50;
    case Baud::Baud110:
        return B110;
    case Baud::Baud300:
        return B300;
    case Baud::Baud600:
        return B600;
    case Baud::Baud1200:
        return B1200;
    case Baud::Baud2400:
        return B2400;
    case Baud::Baud4800:
        return B4800;
    case Baud::Baud9600:
        return B9600;
    case Baud::Baud19200:
        return B19200;
    case Baud::Baud38400:
        return B38400;
    case Baud::Baud57600:
        return B57600;
    case Baud::Baud115200:
        return B115200;
    default:
        return {};
    }
}

Optional<SerialDevice::Baud> SerialDevice::serial_baud_from_termios(speed_t baud)
{
    switch (baud) {
    case B50:
        return Baud::Baud50;
    case B110:
        return Baud::Baud110;
    case B300:
        return Baud::Baud300;
    case B600:
        return Baud::Baud600;
    case B1200:
        return Baud::Baud1200;
    case B2400:
        return Baud::Baud2400;
    case B4800:
        return Baud::Baud4800;
    case B9600:
        return Baud::Baud9600;
    case B19200:
        return Baud::Baud19200;
    case B38400:
        return Baud::Baud38400;
    case B57600:
        return Baud::Baud57600;
    case B115200:
        return Baud::Baud115200;
    default:
        return {};
    }
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

int SerialDevice::change_receiver_enabled(bool should_enable)
{
    if (!should_enable) {
        disable_irq();
        set_interrupts(0);
    } else {
        enable_irq();
        set_interrupts(ReceivedDataAvailableInterrupt);
    }
    return 0;
}

int SerialDevice::change_ignore_modem_status(bool should_enable)
{
    m_ignore_modem_status = should_enable;
    return 0;
}

void SerialDevice::handle_irq(const RegisterState&)
{
    u8 interrupt_identification = m_base_addr.offset(2).in<u8>();
    dbgln_if(SERIAL_DEVICE_DEBUG, "{}: interrupt {:02x}", m_tty_name, interrupt_identification);
    if ((interrupt_identification & 0x01) != 0)
        return; // This interrupt wasn't for us

    // FIXME: parity/etc. error checking on reads -- probably will implement a read_char() method
    // FIXME: no locking here -- correctness issues
    if ((interrupt_identification & 0x0e) == 0x04) {
        dbgln_if(SERIAL_DEVICE_DEBUG, "{}: received Data Available interrupt", m_tty_name);
        while (get_line_status() & LineStatus::DataReady) {
            u8 ch = m_base_addr.offset(0).in<u8>();
            Processor::deferred_call_queue([this, ch] { emit(ch, true); });
        }
    } else if ((interrupt_identification & 0x0e) == 0x0c) {
        dbgln_if(SERIAL_DEVICE_DEBUG, "{}: Time-out Interrupt Pending interrupt", m_tty_name);
        while (get_line_status() & LineStatus::DataReady) {
            u8 ch = m_base_addr.offset(0).in<u8>();
            Processor::deferred_call_queue([this, ch] { emit(ch, true); });
        }
    } else {
        dbgln("{}: Unknown interrupt with ISR {:02x}. We should have disabled those that we can't handle!", m_tty_name, interrupt_identification);
    }
}
}
