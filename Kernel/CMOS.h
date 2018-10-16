#pragma once

#include "types.h"

namespace CMOS {

BYTE read(BYTE index);
void write(BYTE index, BYTE data);

}
