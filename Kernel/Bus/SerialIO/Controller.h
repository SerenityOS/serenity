/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/DistinctNumeric.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>

namespace Kernel {

class InputManagement;
class SerialIOController : public AtomicRefCounted<SerialIOController> {
    friend class InputManagement;

public:
    enum class DeviceCommand : u8 {
        GetDeviceID,
        SetSampleRate,
        EnablePacketStreaming,
        DisablePacketStreaming,
        SetDefaults,
    };

    AK_TYPEDEF_DISTINCT_ORDERED_ID(size_t, PortIndex);

    virtual ~SerialIOController() = default;

    virtual ErrorOr<void> reset_device(PortIndex) = 0;
    virtual ErrorOr<void> send_command(PortIndex, DeviceCommand command) = 0;
    virtual ErrorOr<void> send_command(PortIndex, DeviceCommand command, u8 data) = 0;

    virtual ErrorOr<u8> read_from_device(PortIndex) = 0;
    virtual ErrorOr<void> prepare_for_input(PortIndex) = 0;

protected:
    SerialIOController() = default;

private:
    IntrusiveListNode<SerialIOController, NonnullRefPtr<SerialIOController>> m_list_node;
};

}
