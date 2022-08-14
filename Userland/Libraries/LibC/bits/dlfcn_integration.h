/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/String.h>

struct DlErrorMessage {
    DlErrorMessage(String&& other)
        : text(move(other))
    {
    }

    // The virtual destructor is required because we're passing this
    // struct to the dynamic loader - whose operator delete differs
    // from the one in libc.so
    virtual ~DlErrorMessage() = default;

    String text;
};

struct __Dl_info;
typedef struct __Dl_info Dl_info;

typedef Result<void, DlErrorMessage> (*DlCloseFunction)(void*);
typedef Result<void*, DlErrorMessage> (*DlOpenFunction)(char const*, int);
typedef Result<void*, DlErrorMessage> (*DlSymFunction)(void*, char const*);
typedef Result<void, DlErrorMessage> (*DlAddrFunction)(void*, Dl_info*);

extern "C" {
extern DlCloseFunction __dlclose;
extern DlOpenFunction __dlopen;
extern DlSymFunction __dlsym;
extern DlAddrFunction __dladdr;
}
