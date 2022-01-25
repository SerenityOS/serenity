/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <sys/types.h>

struct DeviceEvent {
    int state;
    int is_block_device;
    dev_t device_id;

    enum State {
        Removed = 0x01,
        Inserted = 0x02,
        Recovered = 0x03,
        FatalError = 0x04,
    };
};
