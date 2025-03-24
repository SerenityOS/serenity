/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Arch/aarch64/RPi/AUXPeripherals.h>
#include <Kernel/Arch/aarch64/RPi/GPIO.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/MiniUART.h>
#include <Kernel/Arch/aarch64/RPi/Timer.h>

namespace Kernel::RPi {

// bcm2711-peripherals.pdf "Table 2. Auxiliary peripherals Address Map"
struct MiniUARTRegisters {
    u32 io_data;
    u32 interrupt_enable;
    u32 interrupt_identify;
    u32 line_control;
    u32 modem_control;
    u32 line_status;
    u32 modem_status;
    u32 scratch;
    u32 extra_control;
    u32 extra_status;
    u32 baud_rate;
};
static_assert(AssertSize<MiniUARTRegisters, 0x6c - 0x40>());

// "Table 8. AUX_MU_LCR_REG Register"
enum LineControl {
    DataSize8Bits = 1,
    Break = 1 << 6,
    DLABAccess = 1 << 7,
};

// "Table 13. AUX_MU_CNTL_REG Register"
enum ExtraControl {
    ReceiverEnable = 1,
    TransmitterEnable = 2,
};

// "Table 10. AUX_MU_LSR_REG Register"
enum LineStatus {
    DataReady = 0,
    ReceiverOverrun = 1,
    TransmitterEmpty = 1 << 5,
    TransmitterIdle = 1 << 6,
};

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<MiniUART>> MiniUART::create()
{
    return Device::try_create_device<MiniUART>();
}

// FIXME: Consider not hardcoding the minor number and allocate it dynamically.
UNMAP_AFTER_INIT MiniUART::MiniUART()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Serial, 0)
    , m_registers(MMIO::the().peripheral<MiniUARTRegisters>(0x21'5040).release_value_but_fixme_should_propagate_errors())
{
    auto& gpio = GPIO::the();
    gpio.set_pin_function(40, GPIO::PinFunction::Alternate5); // TXD1
    gpio.set_pin_function(41, GPIO::PinFunction::Alternate5); // RXD1
    gpio.set_pin_pull_up_down_state(Array { 40, 41 }, GPIO::PullUpDownState::Disable);

    // The mini UART peripheral needs to be enabled before we can configure it.
    AUX::set_peripheral_enabled(AUX::Peripheral::MiniUART, true);

    set_baud_rate(115'200);
    m_registers->line_control = DataSize8Bits;
    m_registers->extra_control = ReceiverEnable | TransmitterEnable;
}

UNMAP_AFTER_INIT MiniUART::~MiniUART() = default;

bool MiniUART::can_read(OpenFileDescription const&, u64) const
{
    return false;
}

ErrorOr<size_t> MiniUART::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    // FIXME: Implement reading from the MiniUART.
    return ENOTIMPL;
}

bool MiniUART::can_write(OpenFileDescription const&, u64) const
{
    return (m_registers->line_status & TransmitterEmpty) != 0;
}

ErrorOr<size_t> MiniUART::write(Kernel::OpenFileDescription& description, u64, Kernel::UserOrKernelBuffer const& buffer, size_t size)
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

void MiniUART::put_char(u8 ch)
{
    while ((m_registers->line_status & TransmitterEmpty) == 0)
        Processor::wait_check();

    if (ch == '\n' && !m_last_put_char_was_carriage_return)
        m_registers->io_data = '\r';

    m_registers->io_data = ch;

    m_last_put_char_was_carriage_return = (ch == '\r');
}

// The mini UAT's clock is generated from the system (VideoCore) clock.
// See section "2.2.1. Mini UART implementation details"
void MiniUART::set_baud_rate(u32 baud_rate)
{
    auto system_clock = Timer::get_clock_rate(Timer::ClockID::V3D);
    m_registers->baud_rate = system_clock / (8 * baud_rate) - 1;
}

}
