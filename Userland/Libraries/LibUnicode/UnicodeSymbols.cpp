/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibUnicode/UnicodeSymbols.h>

#if ENABLE_UNICODE_DATA
#    if defined(__serenity__)
#        include <LibDl/dlfcn.h>
#        include <LibDl/dlfcn_integration.h>
#    else
#        include <dlfcn.h>
#    endif
#else
#    include <AK/Function.h>
#endif

namespace Unicode::Detail {

#if !ENABLE_UNICODE_DATA

template<typename T>
struct FunctionStub;

template<typename ReturnType, typename... ParameterTypes>
struct FunctionStub<Function<ReturnType(ParameterTypes...)>> {
    static constexpr auto make_stub()
    {
        if constexpr (IsVoid<ReturnType>)
            return [](ParameterTypes...) {};
        else
            return [](ParameterTypes...) -> ReturnType { return {}; };
    }
};

#endif

// This loader supports 3 modes:
//
// 1. When the Unicode data generators are enabled, and the target is Serenity, the symbols are
//    dynamically loaded from the shared library containing them.
//
// 2. When the Unicode data generators are enabled, and the target is Lagom, the symbols are
//    dynamically loaded from the main program.
//
// 3. When the Unicode data generators are disabled, the symbols are stubbed out to empty lambdas.
//    This allows callers to remain agnostic as to whether the generators are enabled.
Symbols const& Symbols::ensure_loaded()
{
    static Symbols symbols {};

    static bool initialized = false;
    if (initialized)
        return symbols;

#if ENABLE_UNICODE_DATA
#    if defined(__serenity__)
    static void* libunicodedata = MUST(__dlopen("libunicodedata.so.serenity", RTLD_NOW));

    auto load_symbol = [&]<typename T>(T& dest, char const* name) {
        dest = reinterpret_cast<T>(MUST(__dlsym(libunicodedata, name)));
    };
#    else
    static void* libunicodedata = dlopen(nullptr, RTLD_NOW);
    VERIFY(libunicodedata);

    auto load_symbol = [&]<typename T>(T& dest, char const* name) {
        dest = reinterpret_cast<T>(dlsym(libunicodedata, name));
        VERIFY(dest);
    };
#    endif
#else
    auto load_symbol = []<typename T>(T& dest, char const*) {
        dest = +FunctionStub<Function<RemovePointer<T>>>::make_stub();
    };
#endif

    load_symbol(symbols.code_point_display_name, "unicode_code_point_display_name");
    load_symbol(symbols.canonical_combining_class, "unicode_canonical_combining_class");
    load_symbol(symbols.simple_uppercase_mapping, "unicode_simple_uppercase_mapping");
    load_symbol(symbols.simple_lowercase_mapping, "unicode_simple_lowercase_mapping");
    load_symbol(symbols.special_case_mapping, "unicode_special_case_mapping");
    load_symbol(symbols.general_category_from_string, "unicode_general_category_from_string");
    load_symbol(symbols.code_point_has_general_category, "unicode_code_point_has_general_category");
    load_symbol(symbols.property_from_string, "unicode_property_from_string");
    load_symbol(symbols.code_point_has_property, "unicode_code_point_has_property");
    load_symbol(symbols.script_from_string, "unicode_script_from_string");
    load_symbol(symbols.code_point_has_script, "unicode_code_point_has_script");
    load_symbol(symbols.code_point_has_script_extension, "unicode_code_point_has_script_extension");

    initialized = true;
    return symbols;
}

}
