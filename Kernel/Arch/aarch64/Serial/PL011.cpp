/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/Serial/PL011.h>

namespace Kernel {

// 3.2 Summary of registers
struct PL011Registers {
    u32 data;
    u32 receive_status_or_error_clear;
    u32 unused[4];
    u32 flag;
    u32 unused2;

    u32 irda_low_power_counter;
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
};
static_assert(AssertSize<PL011Registers, 0x4c>());

// Bits of the `flag` register.
// 3.3.3 Flag Register, UARTFR
enum FlagBits {
    ClearToSend = 1 << 0,
    DataSetReady = 1 << 1,
    DataCarrierDetect = 1 << 2,
    UARTBusy = 1 << 3,
    ReceiveFifoEmpty = 1 << 4,
    TransmitFifoFull = 1 << 5,
    ReceiveFifoFull = 1 << 6,
    TransmitFifoEmpty = 1 << 7,
    RingIndicator = 1 << 8,
};

// Bits for the `line_control` register.
// 3.3.7 Line Control Register, UARTLCR_H
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
// 3.3.8 Control Register, UARTCR
//     NOTE: Program the control registers as follows:
//     1. Disable the UART.
//     2. Wait for the end of transmission or reception of the current character.
//     3. Flush the transmit FIFO by setting the FEN bit to 0 in the Line Control Register, UARTLCR_H.
//     4. Reprogram the Control Register, UARTCR.
//     5. Enable the UART
enum ControlBits {
    UARTEnable = 1 << 0,
    SIREnable = 1 << 1,
    SIRLowPowerIrDAModeEnable = 1 << 2,
    // Bits 3-6 are reserved.
    LoopbackEnable = 1 << 7,
    TransmitEnable = 1 << 8,
    ReceiveEnable = 1 << 9,
    DataTransmitReady = 1 << 10,
    RequestToSend = 1 << 11,
    Out1 = 1 << 12,
    Out2 = 1 << 13,
    RTSHardwareFlowControlEnable = 1 << 14,
    CTSHardwareFlowControlEnable = 1 << 15,
};

PL011::PL011(Memory::TypedMapping<PL011Registers volatile> registers_mapping)
    : m_registers(move(registers_mapping))
{
    // Disable UART while changing configuration.
    m_registers->control = 0;

    // FIXME: Should wait for current transmission to end and should flush FIFO.

    m_registers->line_control = EnableFIFOs | WordLength8Bits;

    m_registers->control = UARTEnable | TransmitEnable | ReceiveEnable;
}

ErrorOr<NonnullOwnPtr<PL011>> PL011::initialize(PhysicalAddress physical_address)
{
    auto registers_mapping = TRY(Memory::map_typed_writable<PL011Registers volatile>(physical_address));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) PL011(move(registers_mapping))));
}

void PL011::send(u32 c)
{
    wait_until_we_can_send();
    m_registers->data = c;
}

void PL011::print_str(char const* s, size_t length)
{
    for (size_t i = 0; i < length; ++i) {
        char character = *s++;
        if (character == '\n')
            send('\r');
        send(character);
    }
}

u32 PL011::receive()
{
    wait_until_we_can_receive();

    // Mask out error bits.
    return m_registers->data & 0xFF;
}

void PL011::set_baud_rate(int baud_rate, int uart_frequency_in_hz)
{
    // 3.3.6 Fractional Baud Rate Register, UARTFBRD: """Baud rate divisor BAUDDIV = (FUARTCLK/(16 * Baud rate))""".
    // BAUDDIV is stored as a 16.6 fixed point value, so do computation scaled by (1 << 6) == 64.
    // 64*(FUARTCLK/(16 * Baud rate)) == 4*FUARTCLK/(Baud rate). For rounding, add 0.5 == (Baud rate/2)/(Baud rate).
    u32 baud_rate_divisor_fixed_point = (4 * uart_frequency_in_hz + baud_rate / 2) / baud_rate;

    m_registers->integer_baud_rate_divisor = baud_rate_divisor_fixed_point / 64;
    m_registers->fractional_baud_rate_divisor = baud_rate_divisor_fixed_point % 64;
}

void PL011::wait_until_we_can_send()
{
    while (m_registers->flag & TransmitFifoFull)
        Processor::wait_check();
}

void PL011::wait_until_we_can_receive()
{
    while (m_registers->flag & ReceiveFifoEmpty)
        Processor::wait_check();
}

}
