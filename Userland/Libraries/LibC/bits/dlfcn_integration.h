/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Result.h>

struct DlErrorMessage {
    DlErrorMessage(ByteString&& other)
        : text(move(other))
    {
    }

    // The virtual destructor is required because we're passing this
    // struct to the dynamic loader - whose operator delete differs
    // from the one in libc.so
    virtual ~DlErrorMessage() = default;

    ByteString text;
};

struct __Dl_info;
typedef struct __Dl_info Dl_info;
