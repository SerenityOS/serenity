#pragma once

#include <AK/String.h>
#include <cxxabi.h>

namespace AK {

inline String demangle(const StringView& name)
{
#ifdef KERNEL
    int status = 0;
    auto* demangled_name = abi::__cxa_demangle(String(name).characters(), nullptr, nullptr, &status);
    auto string = String(status == 0 ? demangled_name : name);
    if (status == 0)
        kfree(demangled_name);
    return string;
#else
    return name;
#endif
}

}

using AK::demangle;
