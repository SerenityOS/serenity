/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>
#include <YAK/Vector.h>
#include <Kernel/KBuffer.h>

namespace Kernel {

typedef void* (*ModuleInitPtr)();
typedef void* (*ModuleFiniPtr)();

struct Module {
    String name;
    Vector<KBuffer> sections;

    ModuleInitPtr module_init { nullptr };
    ModuleFiniPtr module_fini { nullptr };
};

}
