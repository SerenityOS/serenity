/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Storage/StorageController.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

StorageController::StorageController()
    : m_controller_id(StorageManagement::generate_controller_id())
{
}

}
