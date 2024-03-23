/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibWayland/Interface.h>

namespace Wayland {

class Object {
public:
    Object(uint32_t id, Interface *interface)
        : m_id(id)
        , m_interface(interface)
         {};

protected:
    uint32_t m_id;
    Interface *m_interface;

};

}
