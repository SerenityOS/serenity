#pragma once

namespace MyNamespace {

int func();

#define USE_VAR2

#define INT_Z int z

struct StructInHeader {
    int var1;
#ifdef USE_VAR2
    int var2;
#else
    int var3;
#endif
};

}
