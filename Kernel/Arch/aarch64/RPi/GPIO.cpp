/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/RPi/GPIO.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>

namespace Kernel::RPi {

// See BCM2835-ARM-Peripherals.pdf section "6 General Purpose I/O" or bcm2711-peripherals.pdf "Chapter 5. General Purpose I/O".

// "6.1 Register View" / "5.2 Register View"

struct PinData {
    u32 bits[2];
    u32 reserved;
};

struct GPIOControlRegisters {
    u32 function_select[6]; // Every u32 stores a 3-bit function code for 10 pins.
    u32 reserved;
    PinData output_set;
    PinData output_clear;
    PinData level;
    PinData event_detect_status;
    PinData rising_edge_detect_enable;
    PinData falling_edge_detect_enable;
    PinData high_detect_enable;
    PinData low_detect_enable;
    PinData async_rising_edge_detect_enable;
    PinData async_falling_edge_detect_enable;
    u32 pull_up_down_enable;
    PinData pull_up_down_enable_clock;
    u32 test;
};

static Singleton<GPIO> s_the;

GPIO::GPIO()
    : m_registers(MMIO::the().peripheral<GPIOControlRegisters>(0x20'0000).release_value_but_fixme_should_propagate_errors())
{
}

void GPIO::initialize()
{
    s_the.ensure_instance();
}

bool GPIO::is_initialized()
{
    return s_the.is_initialized();
}

GPIO& GPIO::the()
{
    VERIFY(is_initialized());
    return s_the;
}

void GPIO::set_pin_function(unsigned pin_number, PinFunction function)
{
    // pin_number must be <= 53. We can't VERIFY() that since this function runs too early to print assertion failures.

    unsigned function_select_index = pin_number / 10;
    unsigned function_select_bits_start = (pin_number % 10) * 3;

    u32 function_bits = m_registers->function_select[function_select_index];
    function_bits = (function_bits & ~(0b111 << function_select_bits_start)) | (static_cast<u32>(function) << function_select_bits_start);
    m_registers->function_select[function_select_index] = function_bits;
}

void GPIO::internal_enable_pins(u32 enable[2], PullUpDownState state)
{
    // Section "GPIO Pull-up/down Clock Registers (GPPUDCLKn)":
    // The GPIO Pull-up/down Clock Registers control the actuation of internal pull-downs on
    // the respective GPIO pins. These registers must be used in conjunction with the GPPUD
    // register to effect GPIO Pull-up/down changes. The following sequence of events is
    // required:
    // 1. Write to GPPUD to set the required control signal (i.e. Pull-up or Pull-Down or neither
    //    to remove the current Pull-up/down)
    m_registers->pull_up_down_enable = static_cast<u32>(state);

    // 2. Wait 150 cycles – this provides the required set-up time for the control signal
    Aarch64::Asm::wait_cycles(150);

    // 3. Write to GPPUDCLK0/1 to clock the control signal into the GPIO pads you wish to
    //    modify – NOTE only the pads which receive a clock will be modified, all others will
    //    retain their previous state.
    m_registers->pull_up_down_enable_clock.bits[0] = enable[0];
    m_registers->pull_up_down_enable_clock.bits[1] = enable[1];

    // 4. Wait 150 cycles – this provides the required hold time for the control signal
    Aarch64::Asm::wait_cycles(150);

    // 5. Write to GPPUD to remove the control signal
    m_registers->pull_up_down_enable = 0;

    // 6. Write to GPPUDCLK0/1 to remove the clock
    m_registers->pull_up_down_enable_clock.bits[0] = 0;
    m_registers->pull_up_down_enable_clock.bits[1] = 0;

    // bcm2711-peripherals.pdf documents GPIO_PUP_PDN_CNTRL_REG[4] registers that store 2 bits state per register, similar to function_select.
    // I don't know if the RPi3 has that already, so this uses the old BCM2835 approach for now.
}

void GPIO::set_pin_high_detect_enable(unsigned pin_number, bool enable)
{
    if (enable) {
        if (pin_number < 32)
            m_registers->high_detect_enable.bits[0] = m_registers->high_detect_enable.bits[0] | (1 << pin_number);
        else
            m_registers->high_detect_enable.bits[1] = m_registers->high_detect_enable.bits[1] | (1 << (pin_number - 32));
    } else {
        if (pin_number < 32)
            m_registers->high_detect_enable.bits[0] = m_registers->high_detect_enable.bits[0] & ~(1 << pin_number);
        else
            m_registers->high_detect_enable.bits[1] = m_registers->high_detect_enable.bits[1] & ~(1 << (pin_number - 32));
    }
}

}
