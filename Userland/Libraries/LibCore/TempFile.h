/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Noncopyable.h>

namespace Core {

class TempFile {
    AK_MAKE_NONCOPYABLE(TempFile);
    AK_MAKE_NONMOVABLE(TempFile);

public:
    enum class Type {
        File,
        Directory
    };

    static NonnullOwnPtr<TempFile> create(Type = Type::File);
    ~TempFile();

    DeprecatedString path() const { return m_path; }

private:
    TempFile(Type);
    static DeprecatedString create_temp(Type);

    Type m_type { Type::File };
    DeprecatedString m_path;
};

}
