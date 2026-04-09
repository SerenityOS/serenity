/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>

namespace FileSystem {

TempFile::~TempFile()
{
    // Temporary files aren't removed by anyone else, so we must do it ourselves.
    auto recursion_mode = RecursionMode::Disallowed;
    if (m_type == Type::Directory)
        recursion_mode = RecursionMode::Allowed;

    auto result = FileSystem::remove(m_path, recursion_mode);
    if (result.is_error())
        warnln("Removal of temporary file failed '{}': {}", m_path, result.error().string_literal());
}

ErrorOr<NonnullOwnPtr<TempFile>> TempFile::create_temp_directory()
{
    char pattern[] = "/tmp/tmp.XXXXXX";

    auto path = TRY(Core::System::mkdtemp(pattern));
    return adopt_nonnull_own_or_enomem(new (nothrow) TempFile(Type::Directory, move(path)));
}

ErrorOr<NonnullOwnPtr<TempFile>> TempFile::create_temp_file()
{
    char file_path[] = "/tmp/tmp.XXXXXX";
    // FIXME: Most callers will probably want to open the file, so find a
    //        way to propagate the fd.
    auto fd = TRY(Core::System::mkstemp(file_path));
    TRY(Core::System::close(fd));

    auto path = ByteString(file_path, strlen(file_path));
    return adopt_nonnull_own_or_enomem(new (nothrow) TempFile(Type::File, move(path)));
}

}
