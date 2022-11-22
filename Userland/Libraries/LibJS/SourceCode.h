/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Forward.h>

namespace JS {

class SourceCode : public RefCounted<SourceCode> {
public:
    static NonnullRefPtr<SourceCode> create(String filename, String code);

    String const& filename() const;
    String const& code() const;

    SourceRange range_from_offsets(u32 start_offset, u32 end_offset) const;

private:
    SourceCode(String filename, String code);

    String m_filename;
    String m_code;
};

}
