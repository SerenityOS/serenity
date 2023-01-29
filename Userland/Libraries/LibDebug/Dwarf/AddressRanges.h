/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CompilationUnit.h"
#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <LibCore/Stream.h>

namespace Debug::Dwarf {

struct Range {
    FlatPtr start { 0 };
    FlatPtr end { 0 };
};

class AddressRangesV5 {
    AK_MAKE_NONCOPYABLE(AddressRangesV5);
    AK_MAKE_NONMOVABLE(AddressRangesV5);

public:
    AddressRangesV5(NonnullOwnPtr<Core::Stream::Stream> range_lists_stream, CompilationUnit const& compilation_unit);

    ErrorOr<void> for_each_range(Function<void(Range)>);

private:
    NonnullOwnPtr<Core::Stream::Stream> m_range_lists_stream;
    CompilationUnit const& m_compilation_unit;
};

class AddressRangesV4 {
    AK_MAKE_NONCOPYABLE(AddressRangesV4);
    AK_MAKE_NONMOVABLE(AddressRangesV4);

public:
    AddressRangesV4(NonnullOwnPtr<Core::Stream::Stream> ranges_stream, CompilationUnit const&);

    ErrorOr<void> for_each_range(Function<void(Range)>);

private:
    NonnullOwnPtr<Core::Stream::Stream> m_ranges_stream;
    CompilationUnit const& m_compilation_unit;
};

}
