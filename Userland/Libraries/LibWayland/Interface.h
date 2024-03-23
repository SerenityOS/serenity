/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWayland/Message.h>

namespace Wayland {

// https://gitlab.freedesktop.org/wayland/wayland/-/blob/main/src/wayland-private.h#L54
struct WireArgumentType {
    enum {
        Integer,
        UnsignedInteger,
        FixedFloat,
        String,
        Object,
        NewId,
        Array,
        FileDescriptor,
    } type;
    bool nullable;
};

struct Argument {
    char const* name;
    const WireArgumentType args[];
};

struct Event {
    char const* name;
    const struct Argument arg[];
};

struct Request {
    char const* name;
    const struct Argument arg[];
};

enum {
    SYNC,
    GET_REGISTRY,
};

typedef int32_t FixedFloat;

struct List {
    
};

struct Interface {
    const char* name;
    uint8_t amount_requests;
    const struct Request** requests;
    uint8_t amount_events;
    const struct Event** events;
};

}
