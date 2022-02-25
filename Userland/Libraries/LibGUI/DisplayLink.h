/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGUI/Forward.h>

namespace GUI {

class DisplayLink {
public:
    static i32 register_callback(Function<void(i32)>);
    static bool unregister_callback(i32 callback_id);

    static void notify(Badge<ConnectionToWindowServer>);
};

}
