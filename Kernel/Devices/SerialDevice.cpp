/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define SERIAL_COM1_ADDR 0x3F8
#define SERIAL_COM2_ADDR 0x2F8
#define SERIAL_COM3_ADDR 0x3E8
#define SERIAL_COM4_ADDR 0x2E8

UNMAP_AFTER_INIT NonnullRefPtr<SerialDevice> SerialDevice::must_create(size_t com_number)
{
    // FIXME: This way of blindly doing release_value is really not a good thing, find
    // a way to propagate errors back.
    RefPtr<SerialDevice> serial_device;
    switch (com_number) {
    case 0: {
        serial_device = DeviceManagement::try_create_device<SerialDevice>(IOAddress(SERIAL_COM1_ADDR), 64).release_value();
        break;
    }
    case 1: {
        serial_device = DeviceManagement::try_create_device<SerialDevice>(IOAddress(SERIAL_COM2_ADDR), 65).release_value();
        break;
    }
    case 2: {
        serial_device = DeviceManagement::try_create_device<SerialDevice>(IOAddress(SERIAL_COM3_ADDR), 66).release_value();
        break;
    }
    case 3: {
        serial_device = DeviceManagement::try_create_device<SerialDevice>(IOAddress(SERIAL_COM4_ADDR), 67).release_value();
        break;
    }
    default:
        break;
    }
    return serial_device.release_nonnull();
}

UNMAP_AFTER_INIT SerialDevice::SerialDevice(IOAddress base_addr, unsigned minor)
    : CharacterDevice(4, minor)
    , m_base_addr(base_addr)
{
    initialize();
}

UNMAP_AFTER_INIT SerialDevice::~SerialDevice()
{
}

bool SerialDevice::can_read(const OpenFileDescription&, size_t) const
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
            byte = m_base_addr.in<u8>();
        return bytes.size();
    });
}

bool SerialDevice::can_write(const OpenFileDescription&, size_t) const
{
    return (get_line_status() & EmptyTransmitterHoldingRegister) != 0;
}

ErrorOr<size_t> SerialDevice::write(OpenFileDescription& description, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker lock(m_serial_lock);
    if (!can_write(description, size))
        return EAGAIN;

    return buffer.read_buffered<128>(size, [&](ReadonlyBytes bytes) {
        for (const auto& byte : bytes)
            put_char(byte);
        return bytes.size();
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
