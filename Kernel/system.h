#pragma once

#include "types.h"

struct system_t
{
    time_t uptime;
    dword nprocess;
    dword nblocked;
};

extern system_t system;
