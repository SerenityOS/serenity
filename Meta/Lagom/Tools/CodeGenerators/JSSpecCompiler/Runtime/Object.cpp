/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Runtime/Object.h"
#include "Function.h"

namespace JSSpecCompiler::Runtime {

Optional<DataProperty&> Property::get_data_property_or_diagnose(Realm* realm, QualifiedName name, Location current_location)
{
    if (!has<DataProperty>()) {
        realm->diag().error(current_location,
            "{} must be a data property", name.to_string());
        realm->diag().note(location(),
            "defined as an accessor property here");
        return {};
    }
    return get<DataProperty>();
}

static StringView well_known_symbol_to_sv(WellKnownSymbol symbol)
{
    static Array string_value = {
#define STRING_VALUE(enum_name, spec_name) "@@" #spec_name##sv,
        ENUMERATE_WELL_KNOWN_SYMBOLS(STRING_VALUE)
#undef STRING_VALUE
    };
    return string_value[to_underlying(symbol)];
}

void Object::do_dump(Printer& printer) const
{
    printer.block([&] {
        for (auto const& [key, value] : m_properties) {
            key.visit(
                [&](Slot const& slot) { printer.format("[[{}]]", slot.key); },
                [&](StringPropertyKey const& string_property) { printer.format("{}", string_property.key); },
                [&](WellKnownSymbol const& symbol) { printer.format("{}", well_known_symbol_to_sv(symbol)); });
            printer.format(": ");
            value.visit(
                [&](DataProperty const& data) {
                    printer.format(
                        "[{}{}{}] ",
                        data.is_configurable ? "c" : "",
                        data.is_enumerable ? "e" : "",
                        data.is_writable ? "w" : "");
                    data.value->dump(printer);
                },
                [&](AccessorProperty const& accessor) {
                    printer.format(
                        "[{}{}] AccessorProperty",
                        accessor.is_configurable ? "c" : "",
                        accessor.is_enumerable ? "e" : "");
                    printer.block([&] {
                        if (accessor.getter.has_value())
                            printer.formatln("get: {},", accessor.getter.value()->name());
                        if (accessor.setter.has_value())
                            printer.formatln("set: {},", accessor.setter.value()->name());
                    });
                });
            printer.formatln(",");
        }
    });
}

}
