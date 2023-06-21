/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/GPIO.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/SDHostController.h>

namespace Kernel::RPi {

Singleton<SDHostController> s_sdhc;

SDHostController& SDHostController::the()
{
    return *s_sdhc;
}

SDHostController::SDHostController()
    : ::SDHostController()
{
    auto& gpio = GPIO::the();
    gpio.set_pin_function(21, GPIO::PinFunction::Alternate3); // CD
    gpio.set_pin_high_detect_enable(21, true);

    gpio.set_pin_function(22, GPIO::PinFunction::Alternate3); // SD1_CLK
    gpio.set_pin_function(23, GPIO::PinFunction::Alternate3); // SD1_CMD

    gpio.set_pin_function(24, GPIO::PinFunction::Alternate3); // SD1_DAT0
    gpio.set_pin_function(25, GPIO::PinFunction::Alternate3); // SD1_DAT1
    gpio.set_pin_function(26, GPIO::PinFunction::Alternate3); // SD1_DAT2
    gpio.set_pin_function(27, GPIO::PinFunction::Alternate3); // SD1_DAT3

    m_registers = MMIO::the().peripheral<SD::HostControlRegisterMap>(0x30'0000);
}

}
