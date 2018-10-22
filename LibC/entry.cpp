#include <Kernel/Syscall.h>

extern "C" int main(int, char**);

extern "C" int _start()
{
    // FIXME: Pass appropriate argc/argv.
    main(0, nullptr);

    // Birger's birthday <3
    return 20150614;
}
