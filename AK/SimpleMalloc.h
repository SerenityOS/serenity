#pragma once

#include "Types.h"

namespace SimpleMalloc {

void initialize();
void dump();
byte* allocate(dword);
byte* allocate_zeroed(dword);
void free(byte*);
byte* reallocate(byte*, dword);

}

