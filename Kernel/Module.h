#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/KBuffer.h>

struct Module {
    String name;
    Vector<KBuffer> sections;
};

typedef void* (*ModuleInitPtr)();
typedef void* (*ModuleFiniPtr)();
