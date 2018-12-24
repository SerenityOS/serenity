#pragma once

#include <AK/AKString.h>
#include <AK/Vector.h>

struct KSym {
    dword address;
    const char* name;
};

const KSym* ksymbolicate(dword address) PURE;
void load_ksyms();

extern bool ksyms_ready;
extern dword ksym_lowest_address;
extern dword ksym_highest_address;
