#pragma once

#ifdef SERENITY
#include <Kernel/kmalloc.h>
#else
#include <new>

#include "Types.h"

extern "C" {

void* kcalloc(size_t nmemb, size_t size);
void* kmalloc(size_t size) __attribute__ ((malloc));
void kfree(void* ptr);
void* krealloc(void* ptr, size_t size);

}

#endif

