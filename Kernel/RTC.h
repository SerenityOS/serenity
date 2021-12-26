#pragma once

#include <Kernel/UnixTypes.h>

namespace RTC {

void initialize();
time_t now();
time_t boot_time();
void read_registers(unsigned& year, unsigned& month, unsigned& day, unsigned& hour, unsigned& minute, unsigned& second);

}

