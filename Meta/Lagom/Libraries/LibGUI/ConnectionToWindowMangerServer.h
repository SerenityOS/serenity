/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

namespace GUI {

class ConnectionToWindowMangerServer {
public:
    static ConnectionToWindowMangerServer& the();
    ConnectionToWindowMangerServer() = default;

    void async_set_event_mask(i32);
    void async_set_manager_window(i32);
};

}
