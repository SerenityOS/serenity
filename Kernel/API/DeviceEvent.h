/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct DeviceEvent {
    int state;
    int is_block_device;
    unsigned major_number;
    unsigned minor_number;

    enum State {
        Removed = 0x01,
        Inserted = 0x02,
        Recovered = 0x03,
        FatalError = 0x04,
    };
};
