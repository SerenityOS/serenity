/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Runtime {

enum class CallbackType {
    ForkPrepare,
    ForkChild,
    ForkParent,
    __Count,
};

void run_pthread_callbacks(CallbackType);
void register_pthread_callback(CallbackType, void (*callback)());

}

extern "C" {
void __pthread_maybe_cancel();
}
