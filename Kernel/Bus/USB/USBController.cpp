/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Devices/Storage/StorageManagement.h>

namespace Kernel::USB {

USBController::USBController()
    : m_storage_controller_id(StorageManagement::generate_controller_id())
{
}

u8 USBController::allocate_address()
{
    // FIXME: This can be smarter.
    return m_next_device_index++;
}

}
