/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <Kernel/Bus/SerialIO/PS2/Device.h>

namespace Kernel {

ErrorOr<NonnullOwnPtr<PS2Device>> PS2Controller::detect_device_on_port(PS2PortIndex port_index)
{
    Array<u8, 2> device_id { 0, 0 };
    {
        SpinlockLocker locker(device_port_spinlock(port_index));
        // NOTE: We disable packet streaming to ensure we always get proper device IDs.
        // Later on the actual driver will reset and enable the device.
        if (auto response_or_error = send_command_while_device_port_locked(port_index, PS2DeviceCommand::DisablePacketStreaming); response_or_error.is_error())
            return Error::from_errno(ENODEV);

        auto device_id_or_error = read_device_id_while_device_port_locked(port_index);
        if (device_id_or_error.is_error()) {
            dmesgln("PS2: {}: Failed to initialize device at port {} due to error when trying to read device ID", controller_type_name(), port_index);
            return Error::from_errno(ENODEV);
        }
        device_id = device_id_or_error.release_value();
    }

    auto device_or_error = PS2Device::probe_for_appropriate_device(*this, port_index, device_id);
    if (device_or_error.is_error()) {
        if (device_or_error.error().code() != ENODEV) {
            dmesgln("PS2: {}: Failed to initialize device at port {}", controller_type_name(), port_index);
            return device_or_error.release_error();
        }
        dmesgln("PS2: {}: Failed to initialize device at port {}: no device found", controller_type_name(), port_index);
        return Error::from_errno(ENODEV);
    }
    return device_or_error.release_value();
}

}
