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

class PS2Controller : public SerialIOController {
public:
    virtual ErrorOr<void> reset_device(PS2PortIndex) = 0;
    virtual ErrorOr<u8> send_command(PS2PortIndex, u8 command) = 0;
    virtual ErrorOr<u8> send_command(PS2PortIndex, u8 command, u8 data) = 0;

    virtual ErrorOr<u8> read_from_device(PS2PortIndex) = 0;
    virtual ErrorOr<void> prepare_for_input(PS2PortIndex) = 0;
    virtual bool irq_process_input_buffer(PS2PortIndex) = 0;

protected:
    PS2Controller() = default;
};

}
