#include "ExecSpace.h"
#include <cstdio>

typedef int (*MainFunctionPtr)(void);

int main(int, char**)
{
    MappedFile f("_test.o");
    if (!f.isValid()) {
        fprintf(stderr, "Failed to map file :(\n");
        return 1;
    }

    ExecSpace space;
    space.loadELF(std::move(f));

    auto func = reinterpret_cast<MainFunctionPtr>(space.symbolPtr("EntryPoint"));
    printf("func: %p\n", func);

    int z = func();
    printf("func() returned %d\n", z);

    return 0;
}
