#include <AK/kstdio.h>
#include <AK/String.h>
#include <assert.h>
#include <stdio.h>


// FIXME: Things defined in crt0 >:(
//     We need to figure out a better way to get these symbols defined and available
//     Even if we're linking a shared object.
__thread int errno;
char* __static_environ[] = { nullptr }; // We don't get the environment without some libc workarounds..
char** environ = __static_environ;
bool __environ_is_malloced = false;
extern unsigned __stack_chk_guard;
unsigned __stack_chk_guard = (unsigned)0xc0000c13;

[[noreturn]] void __stack_chk_fail()
{
    ASSERT_NOT_REACHED();
}

// FIXME: Because we need to call printf, and we don't have access to the stout file descriptor
//     from the main executable, we need to create our own copy in __stdio_init :/
//     Same deal for malloc init. We're essentially manually calling __libc_init here.
extern "C" void __stdio_init();
extern "C" void __malloc_init();

class Global {
public:
    Global(int i)
        : m_i(i)
    {
        __malloc_init();
        __stdio_init();
    }

    int get_i() const { return m_i; }

private:
    int m_i = 0;
};

// This object exists to call __stdio_init and __malloc_init. Also to show that global vars work
Global g_glob { 5 };

extern "C" {
int global_lib_variable = 1234;

void global_lib_function()
{
    printf("Hello from Dynamic Lib! g_glob::m_i == %d\n", g_glob.get_i());
}

const char* other_lib_function(int my_argument)
{
    dbgprintf("Hello from Dynamic Lib, now from the debug port! g_glob::m_i == %d\n", g_glob.get_i());

    int sum = my_argument + global_lib_variable;

    // FIXME: We can't just return AK::String::format across the lib boundary here.
    //     It will use malloc from our DSO's copy of LibC, and then probably be free'd into
    //     the malloc of the main program which would be what they call 'very crash'.
    //     Feels very Windows :)
    static String s_string;
    s_string = String::format("Here's your string! Sum of argument and global_lib_variable: %d", sum);
    return s_string.characters();
}
}
