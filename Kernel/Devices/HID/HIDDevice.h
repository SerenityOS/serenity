/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

class HIDDevice : public CharacterDevice {
public:
    enum class Type {
        Unknown = 0,
        Keyboard,
        Mouse,
    };

    virtual Type instrument_type() const = 0;
    virtual void enable_interrupts() = 0;

protected:
    HIDDevice(MajorNumber major, MinorNumber minor)
        : CharacterDevice(major, minor)
    {
    }

    EntropySource m_entropy_source;
};

}
