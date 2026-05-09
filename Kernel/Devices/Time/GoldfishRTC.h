/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class GoldfishRTC {
public:
    struct Registers;

    GoldfishRTC(Memory::TypedMapping<Registers volatile>);

    static GoldfishRTC* the();

    UnixDateTime boot_time() const { return m_boot_time; }
    UnixDateTime current_time() const;

private:
    Memory::TypedMapping<Registers volatile> m_registers;
    UnixDateTime m_boot_time;
};

}
