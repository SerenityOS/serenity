/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CompilationUnit.h"
#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/MemoryStream.h>
#include <AK/Noncopyable.h>

namespace Debug::Dwarf {

class AddressRanges {
    AK_MAKE_NONCOPYABLE(AddressRanges);
    AK_MAKE_NONMOVABLE(AddressRanges);

public:
    AddressRanges(ReadonlyBytes range_lists_data, size_t offset, CompilationUnit const& compilation_unit);

    struct Range {
        FlatPtr start { 0 };
        FlatPtr end { 0 };
    };
    void for_each_range(Function<void(Range)>);

private:
    InputMemoryStream m_range_lists_stream;
    CompilationUnit const& m_compilation_unit;
};

}
