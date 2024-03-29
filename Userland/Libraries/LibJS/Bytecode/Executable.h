/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/CellAllocator.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>

namespace JS::Bytecode {

struct PropertyLookupCache {
    WeakPtr<Shape> shape;
    Optional<u32> property_offset;
};

struct GlobalVariableCache : public PropertyLookupCache {
    u64 environment_serial_number { 0 };
};

using EnvironmentVariableCache = Optional<EnvironmentCoordinate>;

struct SourceRecord {
    u32 source_start_offset {};
    u32 source_end_offset {};
};

class Executable final : public Cell {
    JS_CELL(Executable, Cell);
    JS_DECLARE_ALLOCATOR(Executable);

public:
    Executable(
        NonnullOwnPtr<IdentifierTable>,
        NonnullOwnPtr<StringTable>,
        NonnullOwnPtr<RegexTable>,
        Vector<Value> constants,
        NonnullRefPtr<SourceCode const>,
        size_t number_of_property_lookup_caches,
        size_t number_of_global_variable_caches,
        size_t number_of_environment_variable_caches,
        size_t number_of_registers,
        Vector<NonnullOwnPtr<BasicBlock>>,
        bool is_strict_mode);

    virtual ~Executable() override;

    DeprecatedFlyString name;
    Vector<PropertyLookupCache> property_lookup_caches;
    Vector<GlobalVariableCache> global_variable_caches;
    Vector<EnvironmentVariableCache> environment_variable_caches;
    Vector<NonnullOwnPtr<BasicBlock>> basic_blocks;
    NonnullOwnPtr<StringTable> string_table;
    NonnullOwnPtr<IdentifierTable> identifier_table;
    NonnullOwnPtr<RegexTable> regex_table;
    Vector<Value> constants;

    NonnullRefPtr<SourceCode const> source_code;
    size_t number_of_registers { 0 };
    bool is_strict_mode { false };

    ByteString const& get_string(StringTableIndex index) const { return string_table->get(index); }
    DeprecatedFlyString const& get_identifier(IdentifierTableIndex index) const { return identifier_table->get(index); }

    Optional<DeprecatedFlyString const&> get_identifier(Optional<IdentifierTableIndex> const& index) const
    {
        if (!index.has_value())
            return {};
        return get_identifier(*index);
    }

    void dump() const;

private:
    virtual void visit_edges(Visitor&) override;
};

}
