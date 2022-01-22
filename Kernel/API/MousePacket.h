/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct MousePacket {
    int x { 0 };
    int y { 0 };
    int z { 0 };
    int w { 0 };

    enum Button {
        LeftButton = 0x01,
        RightButton = 0x02,
        MiddleButton = 0x04,
        BackwardButton = 0x08,
        ForwardButton = 0x10,
    };

    unsigned char buttons { 0 };
    bool is_relative { true };
};
