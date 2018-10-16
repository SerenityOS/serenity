#pragma once

#include "Types.h"

extern "C" {

void* kcalloc(size_t nmemb, size_t size);
void* kmalloc(size_t size);
void kfree(void* ptr);
void* krealloc(void* ptr, size_t size);

}

