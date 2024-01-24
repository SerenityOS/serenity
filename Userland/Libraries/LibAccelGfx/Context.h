/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/OwnPtr.h>

#ifndef AK_OS_MACOS
// Make sure egl.h doesn't give us definitions from X11 headers
#    define EGL_NO_X11
#    include <EGL/egl.h>
#    undef EGL_NO_X11
#endif

namespace AccelGfx {

class Context {
public:
    static ErrorOr<NonnullOwnPtr<Context>> create();

    Context()
    {
    }

    virtual ~Context()
    {
    }

    virtual void activate() = 0;
};

}
