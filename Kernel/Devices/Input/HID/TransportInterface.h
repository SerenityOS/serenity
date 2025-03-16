/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Kernel::HID {

class TransportInterface {
public:
    virtual ~TransportInterface() = default;

    using InputReportCallback = Function<void(ReadonlyBytes)>;
    virtual ErrorOr<void> start_receiving_input_reports(InputReportCallback&&) = 0;
};

}
