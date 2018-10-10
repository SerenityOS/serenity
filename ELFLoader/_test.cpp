#include <stdio.h>

extern "C" const char hello_string[] = "Hello World!";

extern "C" int foo()
{
    volatile int i = 3;
    i = 4;
    return i;
}

extern "C" int bar()
{
    return foo();
}

extern "C" int baz()
{
    return bar();
}

extern "C" int EntryPoint()
{
    puts(hello_string);
    printf("abc!\n");
    printf("def!\n");
    return 10 + baz();
}

