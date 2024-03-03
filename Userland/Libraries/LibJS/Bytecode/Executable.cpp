/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Bytecode/RegexTable.h>
#include <LibJS/SourceCode.h>

namespace JS::Bytecode {

JS_DEFINE_ALLOCATOR(Executable);

Executable::Executable(
    NonnullOwnPtr<IdentifierTable> identifier_table,
    NonnullOwnPtr<StringTable> string_table,
    NonnullOwnPtr<RegexTable> regex_table,
    Vector<Value> constants,
    NonnullRefPtr<SourceCode const> source_code,
    size_t number_of_property_lookup_caches,
    size_t number_of_global_variable_caches,
    size_t number_of_environment_variable_caches,
    size_t number_of_registers,
    Vector<NonnullOwnPtr<BasicBlock>> basic_blocks,
    bool is_strict_mode)
    : basic_blocks(move(basic_blocks))
    , string_table(move(string_table))
    , identifier_table(move(identifier_table))
    , regex_table(move(regex_table))
    , constants(move(constants))
    , source_code(move(source_code))
    , number_of_registers(number_of_registers)
    , is_strict_mode(is_strict_mode)
{
    property_lookup_caches.resize(number_of_property_lookup_caches);
    global_variable_caches.resize(number_of_global_variable_caches);
    environment_variable_caches.resize(number_of_environment_variable_caches);
}

Executable::~Executable() = default;

void Executable::dump() const
{
    warnln("\033[37;1mJS bytecode executable\033[0m \"{}\"", name);
    for (auto& block : basic_blocks)
        block->dump(*this);

    warnln("");
}

void Executable::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto constant : constants)
        visitor.visit(constant);
}

}
