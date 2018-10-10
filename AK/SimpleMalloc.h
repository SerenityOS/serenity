#pragma once

#include "Types.h"

namespace SimpleMalloc {

void initialize();
void dump();
byte* allocate(dword);
byte* allocateZeroed(dword);
void free(byte*);
byte* reallocate(byte*, dword);

}

