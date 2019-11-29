#pragma once

#include <AK/String.h>
#include <cxxabi.h>

namespace AK {

inline String demangle(const char* name)
{
    int status = 0;
    auto* demangled_name = abi::__cxa_demangle(name, nullptr, nullptr, &status);
    auto string = String(status == 0 ? demangled_name : name);
    if (status == 0)
        kfree(demangled_name);
    return string;
}

}

using AK::demangle;

