/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct AUXRegisters;

class AUX {
public:
    AUX();
    static AUX& the();

    enum class Peripheral {
        MiniUART,
        SPI1,
        SPI2,
    };

    void set_peripheral_enabled(Peripheral, bool);

private:
    Memory::TypedMapping<AUXRegisters volatile> m_registers;
};

}
