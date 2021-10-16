/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/GPIO.h>
#include <Kernel/Prekernel/Arch/aarch64/MMIO.h>
#include <Kernel/Prekernel/Arch/aarch64/Timer.h>
#include <Kernel/Prekernel/Arch/aarch64/UART.h>

namespace Prekernel {

// "13.4 Register View" / "11.5 Register View"
struct UARTRegisters {
    u32 data;
    u32 receive_status_or_error_clear;
    u32 unused[4];
    u32 flag;
    u32 unused2;

    u32 unused_ilpr;
    u32 integer_baud_rate_divisor;    // Only the lowest 16 bits are used.
    u32 fractional_baud_rate_divisor; // Only the lowest 6 bits are used.
    u32 line_control;

    u32 control;
    u32 interrupt_fifo_level_select;
    u32 interrupt_mask_set_clear;
    u32 raw_interrupt_status;

    u32 masked_interrupt_status;
    u32 interrupt_clear;
    u32 dma_control;
    u32 test_control;
};

// Bits of the `flag` register.
// See "FR register" in Broadcom doc for details.
enum FlagBits {
    ClearToSend = 1 << 0,
    UnsupportedDSR = 1 << 1,
    UnsupportedDCD = 1 << 2,
    UARTBusy = 1 << 3,
    ReceiveFifoEmpty = 1 << 4,
    TransmitFifoFull = 1 << 5,
    ReceiveFifoFull = 1 << 6,
    TransmitFifoEmpty = 1 << 7,
};

// Bits for the `line_control` register.
// See "LCRH register" in Broadcom doc for details.
enum LineControlBits {
    SendBreak = 1 << 0,
    EnableParityCheckingAndGeneration = 1 << 1,
    EvenParity = 1 << 2,
    TransmitTwoStopBits = 1 << 3,
    EnableFIFOs = 1 << 4,

    WordLength5Bits = 0b00 << 5,
    WordLength6Bits = 0b01 << 5,
    WordLength7Bits = 0b10 << 5,
    WordLength8Bits = 0b11 << 5,

    StickParity = 1 << 7,
};

// Bits for the `control` register.
// See "CR register" in Broadcom doc for details. From there:
//     NOTE: Program the control registers as follows:
//     1. Disable the UART.
//     2. Wait for the end of transmission or reception of the current character.
//     3. Flush the transmit FIFO by setting the FEN bit to 0 in the Line Control Register, UART_LCRH.
//     4. Reprogram the Control Register, UART_CR.
//     5. Enable the UART
enum ControlBits {
    UARTEnable = 1 << 0,
    UnsupportedSIREN = 1 << 1,
    UnsupportedSIRLP = 1 << 2,
    // Bits 3-6 are reserved.
    LoopbackEnable = 1 << 7,
    TransmitEnable = 1 << 8,
    ReceiveEnable = 1 << 9,
    UnsupportedDTR = 1 << 10,
    RequestToSend = 1 << 11,
    UnsupportedOut1 = 1 << 12,
    UnsupportedOut2 = 1 << 13,
    RTSHardwareFlowControlEnable = 1 << 14,
    CTSHardwareFlowControlEnable = 1 << 15,
};

UART::UART()
    : m_registers(MMIO::the().peripheral<UARTRegisters>(0x20'1000))
{
    // Disable UART while changing configuration.
    m_registers->control = 0;

    // FIXME: Should wait for current transmission to end and should flush FIFO.

    constexpr int baud_rate = 115'200;

    // Set UART clock so that the baud rate divisor ends up as 1.0.
    // FIXME: Not sure if this is a good UART clock rate.
    u32 rate_in_hz = Timer::the().set_clock_rate(Timer::ClockID::UART, 16 * baud_rate);

    // The BCM's PL011 UART is alternate function 0 on pins 14 and 15.
    auto& gpio = Prekernel::GPIO::the();
    gpio.set_pin_function(14, Prekernel::GPIO::PinFunction::Alternate0);
    gpio.set_pin_function(15, Prekernel::GPIO::PinFunction::Alternate0);
    gpio.set_pin_pull_up_down_state(Array { 14, 15 }, Prekernel::GPIO::PullUpDownState::Disable);

    // Clock and pins are configured. Turn UART on.
    set_baud_rate(baud_rate, rate_in_hz);
    m_registers->line_control = EnableFIFOs | WordLength8Bits;

    m_registers->control = UARTEnable | TransmitEnable | ReceiveEnable;
}

UART& UART::the()
{
    static UART instance;
    return instance;
}

void UART::send(u32 c)
{
    wait_until_we_can_send();
    m_registers->data = c;
}

u32 UART::receive()
{
    wait_until_we_can_receive();

    // Mask out error bits.
    return m_registers->data & 0xFF;
}

void UART::set_baud_rate(int baud_rate, int uart_frequency_in_hz)
{
    // Broadcom doc: """Baud rate divisor BAUDDIV = (FUARTCLK/(16 * Baud rate))""".
    // BAUDDIV is stored as a 16.6 fixed point value, so do computation scaled by (1 << 6) == 64.
    // 64*(FUARTCLK/(16 * Baud rate)) == 4*FUARTCLK/(Baud rate). For rounding, add 0.5 == (Baud rate/2)/(Baud rate).
    u32 baud_rate_divisor_fixed_point = (4 * uart_frequency_in_hz + baud_rate / 2) / baud_rate;

    m_registers->integer_baud_rate_divisor = baud_rate_divisor_fixed_point / 64;
    m_registers->fractional_baud_rate_divisor = baud_rate_divisor_fixed_point % 64;
}

void UART::wait_until_we_can_send()
{
    while (m_registers->flag & TransmitFifoFull)
        ;
}

void UART::wait_until_we_can_receive()
{
    while (m_registers->flag & ReceiveFifoEmpty)
        ;
}

}
