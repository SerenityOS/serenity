/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/ATA/ATAController.h>
#include <Kernel/Devices/Storage/StorageManagement.h>

namespace Kernel {

ATAController::ATAController()
    : StorageController(StorageManagement::generate_relative_ata_controller_id({}))
{
}

}
