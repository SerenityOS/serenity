/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Types.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct GPIOControlRegisters;

// Can configure the general-purpose I/O registers on a Raspberry Pi.
class GPIO {
public:
    GPIO();

    enum class PinFunction {
        Input = 0,
        Output = 1,
        Alternate0 = 0b100,
        Alternate1 = 0b101,
        Alternate2 = 0b110,
        Alternate3 = 0b111,
        Alternate4 = 0b011,
        Alternate5 = 0b010,
    };

    static void initialize();
    static bool is_initialized();
    static GPIO& the();

    void set_pin_function(unsigned pin_number, PinFunction);

    enum class PullUpDownState {
        Disable = 0,
        PullDown = 1,
        PullUp = 2,
    };

    template<size_t N>
    void set_pin_pull_up_down_state(Array<int, N> pins, PullUpDownState state)
    {
        u32 enable[2] = {};
        for (int pin : pins) {
            if (pin < 32)
                enable[0] |= (1 << pin);
            else
                enable[1] |= (1 << (pin - 32));
        }
        internal_enable_pins(enable, state);
    }

    void set_pin_high_detect_enable(unsigned pin_number, bool enable);

private:
    void internal_enable_pins(u32 enable[2], PullUpDownState state);

    Memory::TypedMapping<GPIOControlRegisters volatile> m_registers;
};

}
