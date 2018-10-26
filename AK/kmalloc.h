#pragma once

#include "Compiler.h"

#ifdef SERENITY
#ifdef USERLAND
#include <LibC/stdlib.h>
#else
#include <Kernel/kmalloc.h>
#endif
#else
#include <new>

#include "Types.h"

extern "C" {

void* kcalloc(size_t nmemb, size_t size);
void* kmalloc(size_t size) MALLOC_ATTR;
void kfree(void* ptr);
void* krealloc(void* ptr, size_t size);

}

#endif

