
#include "../lib.h"

int func();

__thread int g_tls1 = 0;
__thread int g_tls2 = 0;

static void init_function() __attribute__((constructor));

void init_function()
{
    g_tls1 = 1;
    g_tls2 = 2;
}

int func()
{
    return 3;
}
