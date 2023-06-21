/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/StorageController.h>
#include <Kernel/Devices/Storage/StorageManagement.h>

namespace Kernel {

StorageController::StorageController(u32 hardware_relative_controller_id)
    : m_controller_id(StorageManagement::generate_controller_id())
    , m_hardware_relative_controller_id(hardware_relative_controller_id)
{
}

}
