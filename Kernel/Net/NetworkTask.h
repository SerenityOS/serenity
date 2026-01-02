/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Tasks/Thread.h>
namespace Kernel {

class NetworkTask {
public:
    static void spawn();
    static bool is_current();
    static Thread* the_thread();
};
}
