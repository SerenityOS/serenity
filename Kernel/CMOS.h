#pragma once

#include <AK/Types.h>

namespace CMOS {

byte read(byte index);
void write(byte index, byte data);

}
