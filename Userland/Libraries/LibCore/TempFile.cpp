/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TempFile.h"
#include <LibCore/DeprecatedFile.h>
#include <LibCore/System.h>

namespace Core {

ErrorOr<NonnullOwnPtr<TempFile>> TempFile::create_temp_directory()
{
    char pattern[] = "/tmp/tmp.XXXXXX";

    auto path = TRY(Core::System::mkdtemp(pattern));
    return adopt_nonnull_own_or_enomem(new (nothrow) TempFile(Type::Directory, path));
}

ErrorOr<NonnullOwnPtr<TempFile>> TempFile::create_temp_file()
{
    char file_path[] = "/tmp/tmp.XXXXXX";
    TRY(Core::System::mkstemp(file_path));

    auto string = TRY(String::from_utf8({ file_path, sizeof file_path }));
    return adopt_nonnull_own_or_enomem(new (nothrow) TempFile(Type::File, string));
}

TempFile::~TempFile()
{
    // Temporary files aren't removed by anyone else, so we must do it ourselves.
    auto recursion_mode = DeprecatedFile::RecursionMode::Disallowed;
    if (m_type == Type::Directory)
        recursion_mode = DeprecatedFile::RecursionMode::Allowed;

    auto result = DeprecatedFile::remove(m_path, recursion_mode);
    if (result.is_error()) {
        warnln("Removal of temporary file failed: {}", result.error().string_literal());
    }
}

}
