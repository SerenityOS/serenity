/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/SD/Registers.h>
#include <Kernel/Devices/Storage/SD/SDHostController.h>

namespace Kernel::RPi {

class SDHostController : public ::SDHostController {
public:
    static SDHostController& the();
    SDHostController();
    virtual ~SDHostController() override = default;

protected:
    // ^SDHostController
    virtual SD::HostControlRegisterMap volatile* get_register_map_base_address() override { return m_registers; }

private:
    SD::HostControlRegisterMap volatile* m_registers;
};

}
