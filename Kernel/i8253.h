#pragma once

#include <AK/Types.h>

#define TICKS_PER_SECOND          1000

namespace PIT {

void initialize();
dword ticks_this_second();
dword seconds_since_boot();

}
