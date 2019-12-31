#include <AK/String.h>

#include <dlfcn.h>
#include <stdio.h>

int main()
{
    void* handle = dlopen("/usr/lib/libDynamicLib.so", RTLD_LAZY | RTLD_GLOBAL);

    if (!handle) {
        printf("Failed to dlopen! %s\n", dlerror());
        return 1;
    }

    // Test getting an external variable from the library and read it out
    int* ptr_global = (int*)dlsym(handle, "global_lib_variable");

    if (!ptr_global) {
        printf("Failed to dlsym for \"global_lib_variable\"! %s\n", dlerror());
        return 2;
    }

    printf("Found global lib variable address: %p\n", ptr_global);

    printf("Global lib variable is %d\n", *ptr_global);

    // Test getting a method from the library and calling it
    void (*lib_func)(void) = (void (*)(void))dlsym(handle, "global_lib_function");

    printf("Found global lib function address: %p\n", lib_func);

    if (!lib_func) {
        printf("Failed to dlsym for \"global_lib_function\"! %s\n", dlerror());
        return 2;
    }

    lib_func();

    printf("I think I called my lib function!\n");

    // Test getting a method that takes and returns arugments now
    const char* (*other_func)(int) = (const char* (*)(int))dlsym(handle, "other_lib_function");

    printf("Found other lib function address %p\n", other_func);

    if (!other_func) {
        printf("Failed to dlsym for \"other_lib_function\"! %s\n", dlerror());
        return 2;
    }

    // Call it twice with different arguments
    String formatted_result = other_func(10);

    printf("(%d + %d = %d) %s\n", *ptr_global, 10, *ptr_global + 10, formatted_result.characters());

    *ptr_global = 17;

    formatted_result = other_func(5);

    printf("(%d + %d = %d) %s\n", *ptr_global, 5, *ptr_global + 5, formatted_result.characters());

    int ret = dlclose(handle);

    if (ret < 0) {
        printf("Failed to dlclose! %s\n", dlerror());
        return 3;
    }

    printf("Bye for now!\n");

    return 0;
}