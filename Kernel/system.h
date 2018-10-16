#pragma once

#include "types.h"

struct system_t
{
    time_t uptime;
    DWORD nprocess;
    DWORD nblocked;
};

extern system_t system;
