/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/String.h>

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

    String path() const { return m_path; }

private:
    TempFile(Type);
    static String create_temp(Type);

    Type m_type { Type::File };
    String m_path;
};

}
