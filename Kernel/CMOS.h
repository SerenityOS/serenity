#pragma once

#include <AK/Types.h>

namespace CMOS {

u8 read(u8 index);
void write(u8 index, u8 data);

}
