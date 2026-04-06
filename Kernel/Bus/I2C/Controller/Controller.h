/*
 * Copyright (c) 2024-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <Kernel/Bus/I2C/I2C.h>

// I²C-bus specification and user manual: https://www.nxp.com/docs/en/user-guide/UM10204.pdf

namespace Kernel::I2C {

// Target-transmitter to controller-receiver transfer
struct ReadTransfer {
    Address target_address; // 7-bit or 10-bit I²C target address
    Bytes data_read;
};

// Controller-transmitter to target-receiver transfer
struct WriteTransfer {
    Address target_address; // 7-bit or 10-bit I²C target address
    ReadonlyBytes data_to_write;
};

using Transfer = Variant<ReadTransfer, WriteTransfer>;

class Controller {
public:
    virtual ~Controller() = default;

    virtual ErrorOr<void> do_transfers(Span<Transfer>) = 0;

protected:
    Controller() = default;
};

}
