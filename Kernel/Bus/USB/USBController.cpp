/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Devices/Storage/StorageManagement.h>

namespace Kernel::USB {

USBController::USBController()
    : m_storage_controller_id(StorageManagement::generate_controller_id())
{
}

ErrorOr<void> USBController::reset_pipe(Device& device, Pipe& pipe)
{
    if (pipe.type() == Pipe::Type::Control)
        return {};

    TRY(device.control_transfer(USB_REQUEST_TYPE_STANDARD | USB_REQUEST_RECIPIENT_ENDPOINT | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
        USB_REQUEST_CLEAR_FEATURE, USB_FEATURE_ENDPOINT_HALT, pipe.endpoint_address(), 0, nullptr));

    return {};
}

}
