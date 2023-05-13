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
#include <Kernel/Locking/LockRank.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

enum class PS2DeviceCommand {
    GetDeviceID,
    SetSampleRate,
    EnablePacketStreaming,
    DisablePacketStreaming,
    SetDefaults,
};

class PS2Device;
class PS2Controller : public SerialIOController {
public:
    virtual ErrorOr<Array<u8, 2>> read_device_id_while_device_port_locked(PS2PortIndex) = 0;
    virtual ErrorOr<void> reset_while_device_port_locked(PS2PortIndex) = 0;

    virtual Spinlock<LockRank::None>& device_port_spinlock(PS2PortIndex) = 0;
    virtual ErrorOr<void> send_command_while_device_port_locked(PS2PortIndex, PS2DeviceCommand command) = 0;
    virtual ErrorOr<void> send_command_while_device_port_locked(PS2PortIndex, PS2DeviceCommand command, u8 data) = 0;

    virtual ErrorOr<u8> read_from_device_while_device_port_locked(PS2PortIndex) = 0;
    virtual bool irq_process_input_buffer(PS2PortIndex) = 0;

protected:
    ErrorOr<NonnullOwnPtr<PS2Device>> detect_device_on_port(PS2PortIndex);

    PS2Controller() = default;
};

}
