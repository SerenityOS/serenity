/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>

namespace JS {

class SourceCode : public RefCounted<SourceCode> {
public:
    static NonnullRefPtr<SourceCode> create(DeprecatedString filename, DeprecatedString code);

    DeprecatedString const& filename() const;
    DeprecatedString const& code() const;

    SourceRange range_from_offsets(u32 start_offset, u32 end_offset) const;

private:
    SourceCode(DeprecatedString filename, DeprecatedString code);

    void compute_line_break_offsets() const;

    DeprecatedString m_filename;
    DeprecatedString m_code;

    Optional<Vector<size_t>> mutable m_line_break_offsets;
};

}
