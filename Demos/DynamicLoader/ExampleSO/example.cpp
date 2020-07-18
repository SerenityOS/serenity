

#include <Kernel/Syscall.h>
// This will go into TLS
// __thread int g_lib_var;
int g_lib_var1;
int g_lib_var2 = 5;
__thread int g_tls_lib_var1;
__thread int g_tls_lib_var2;
__thread int g_tls_lib_var3;

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
        g_lib_var1 = 2;
        x = 3 + g_lib_var2;
        local_dbgputstr("A ctor\n", 8);
        // asm("int3");
        g_tls_lib_var2 = 2;
        g_tls_lib_var3 = 3;
        g_tls_lib_var1 = 1;
    }
};

A a;

int libfunc3()
{
    return g_tls_lib_var3;
}

int libfunc2()
{
    // asm("int3");
    return g_tls_lib_var1 + g_tls_lib_var2 + g_tls_lib_var3;
}

int libfunc()
{
    // somevar = 4;

    return 4 + g_lib_var1 + a.x;
    // return g_tls_lib_var1;
}

// int libfunc_tls()
// {
//     // g_tls_lib_var = 5;
//     // g_tls_lib_var2 = 6;
//     return g_tls_lib_var;
// }
