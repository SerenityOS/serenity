/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>

namespace Core {

class TempFile {

public:
    static ErrorOr<NonnullOwnPtr<TempFile>> create_temp_directory();
    static ErrorOr<NonnullOwnPtr<TempFile>> create_temp_file();

    ~TempFile();

    String const& path() const { return m_path; }

private:
    enum class Type {
        Directory,
        File
    };

    TempFile(Type type, String path)
        : m_type(type)
        , m_path(move(path))
    {
    }

    Type m_type;
    String m_path;
};

}
