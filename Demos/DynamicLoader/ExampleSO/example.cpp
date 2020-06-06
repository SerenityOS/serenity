

#include <Kernel/Syscall.h>
// This will go into TLS
// __thread int somevar;
int g_lib_var = 2;

void local_dbgputstr(const char* str, int len)
{
    // return str[0] + len;
    constexpr unsigned int function = Kernel::SC_dbgputch;
    for (int i = 0; i < len; ++i) {
        unsigned int result;
        asm volatile("int $0x82"
                     : "=a"(result)
                     : "a"(function), "d"((u32)str[i])
                     : "memory");
    }
}

class A {
public:
    int x;
    A()
    {
        x = 3;
        local_dbgputstr("A ctor\n", 8);
    }
};

A a;

int libfunc()
{
    // somevar = 4;

    return 4 + g_lib_var + a.x;
}
