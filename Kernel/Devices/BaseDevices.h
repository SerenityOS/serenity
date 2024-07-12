/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/Generic/ConsoleDevice.h>
#include <Kernel/Devices/Generic/DeviceControlDevice.h>
#include <Kernel/Devices/Generic/NullDevice.h>

namespace Kernel {

struct BaseDevices {
    NonnullRefPtr<NullDevice> const null_device;
    NonnullRefPtr<ConsoleDevice> const console_device;
    NonnullRefPtr<DeviceControlDevice> const device_control_device;
};

}
