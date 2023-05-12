/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/SerialIO/Controller.h>
#include <Kernel/Bus/SerialIO/PS2/Definitions.h>

namespace Kernel {

enum class PS2DeviceCommand {
    GetDeviceID,
    SetSampleRate,
    EnablePacketStreaming,
    DisablePacketStreaming,
    SetDefaults,
};

class PS2Controller : public SerialIOController {

protected:
    PS2Controller() = default;
};

}
