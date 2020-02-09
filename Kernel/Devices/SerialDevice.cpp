/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Devices/SerialDevice.h>
#include <LibBareMetal/IO.h>

SerialDevice::SerialDevice(int base_addr, unsigned minor)
    : CharacterDevice(4, minor)
    , m_base_addr(base_addr)
{
    initialize();
}

SerialDevice::~SerialDevice()
{
}

bool SerialDevice::can_read(const FileDescription&) const
{
    return (get_line_status() & DataReady) != 0;
}

ssize_t SerialDevice::read(FileDescription&, u8* buffer, ssize_t size)
{
    if (!size)
        return 0;

    if (!(get_line_status() & DataReady))
        return 0;

    buffer[0] = IO::in8(m_base_addr);

    return 1;
}

bool SerialDevice::can_write(const FileDescription&) const
{
    return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
}

ssize_t SerialDevice::write(FileDescription&, const u8* buffer, ssize_t size)
{
    if (!size)
        return 0;

    if (!(get_line_status() & EmptyTransmitterHoldingRegister))
        return 0;

    IO::out8(m_base_addr, buffer[0]);

    return 1;
}

void SerialDevice::initialize()
{
    set_interrupts(0);
    set_baud(Baud38400);
    set_line_control(None, One, EightBits);
    set_fifo_control(EnableFIFO | ClearReceiveFIFO | ClearTransmitFIFO | TriggerLevel4);
    set_modem_control(RequestToSend | DataTerminalReady);
}

void SerialDevice::set_interrupts(char interrupt_enable)
{
    m_interrupt_enable = interrupt_enable;

    IO::out8(m_base_addr + 1, interrupt_enable);
}

void SerialDevice::set_baud(Baud baud)
{
    m_baud = baud;

    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) | 0x80); // turn on DLAB
    IO::out8(m_base_addr + 0, ((char)(baud)) >> 2);             // lower half of divisor
    IO::out8(m_base_addr + 1, ((char)(baud)) & 0xff);           // upper half of divisor
    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) & 0x7f); // turn off DLAB
}

void SerialDevice::set_fifo_control(char fifo_control)
{
    m_fifo_control = fifo_control;

    IO::out8(m_base_addr + 2, fifo_control);
}

void SerialDevice::set_line_control(ParitySelect parity_select, StopBits stop_bits, WordLength word_length)
{
    m_parity_select = parity_select;
    m_stop_bits = stop_bits;
    m_word_length = word_length;

    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) & (0xc0 | parity_select | stop_bits | word_length));
}

void SerialDevice::set_break_enable(bool break_enable)
{
    m_break_enable = break_enable;

    IO::out8(m_base_addr + 3, IO::in8(m_base_addr + 3) & (break_enable ? 0xff : 0xbf));
}

void SerialDevice::set_modem_control(char modem_control)
{
    m_modem_control = modem_control;

    IO::out8(m_base_addr + 4, modem_control);
}

char SerialDevice::get_line_status() const
{
    return IO::in8(m_base_addr + 5);
}
