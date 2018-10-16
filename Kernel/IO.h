#pragma once

#include "types.h"

namespace IO {

BYTE in8(WORD port);
WORD in16(WORD port);
DWORD in32(WORD port);

void out8(WORD port, BYTE data);
void out16(WORD port, WORD data);
void out32(WORD port, DWORD data);

}
