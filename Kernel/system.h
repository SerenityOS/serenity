#pragma once

#include "types.h"
#include <AK/Vector.h>
#include <AK/String.h>

struct KSym {
    dword address;
    String name;
};

Vector<KSym, KmallocEternalAllocator>& ksyms() PURE;
const KSym* ksymbolicate(dword address) PURE;

struct system_t
{
    time_t uptime;
    DWORD nprocess;
    DWORD nblocked;
};

extern system_t system;
