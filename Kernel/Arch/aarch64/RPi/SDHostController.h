/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/SD/Registers.h>
#include <Kernel/Devices/Storage/SD/SDHostController.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

class SDHostController : public ::SDHostController {
public:
    SDHostController(Memory::TypedMapping<SD::HostControlRegisterMap volatile>);
    virtual ~SDHostController() override = default;

protected:
    // ^SDHostController
    virtual SD::HostControlRegisterMap volatile* get_register_map_base_address() override { return m_registers.ptr(); }

private:
    Memory::TypedMapping<SD::HostControlRegisterMap volatile> m_registers;
};

}
