/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

class MainIdRegister {
public:
    MainIdRegister();

    enum Implementer {
        ArmLimited = 0x41,
    };
    unsigned implementer() const { return (m_value >> 24) & 0xFF; }
    unsigned variant() const { return (m_value >> 20) & 0xF; }
    unsigned architecture() const { return (m_value >> 16) & 0xF; }

    enum PartNum {
        RaspberryPi1 = 0xB76,
        RaspberryPi2 = 0xC07,
        RaspberryPi3 = 0xD03,
        RaspberryPi4 = 0xD08,
    };
    unsigned part_num() const { return (m_value >> 4) & 0xFFF; }

    unsigned revision() const { return m_value & 0xF; }

private:
    unsigned int m_value;
};

}
