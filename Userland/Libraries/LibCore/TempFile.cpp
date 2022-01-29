/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TempFile.h"
#include <AK/Random.h>
#include <LibCore/File.h>
#include <fcntl.h>
#include <stdlib.h>
#include <sys/stat.h>

namespace Core {

NonnullOwnPtr<TempFile> TempFile::create(Type type)
{
    return adopt_own(*new TempFile(type));
}

String TempFile::create_temp(Type type)
{
    char name_template[] = "/tmp/tmp.XXXXXX";
    switch (type) {
    case Type::File: {
        auto fd = mkstemp(name_template);
        VERIFY(fd >= 0);
        close(fd);
        break;
    }
    case Type::Directory: {
        auto fd = mkdtemp(name_template);
        VERIFY(fd != nullptr);
        break;
    }
    }
    return String { name_template };
}

TempFile::TempFile(Type type)
    : m_type(type)
    , m_path(create_temp(type))
{
}

TempFile::~TempFile()
{
    File::RecursionMode recursion_allowed { File::RecursionMode::Disallowed };
    if (m_type == Type::Directory)
        recursion_allowed = File::RecursionMode::Allowed;

    auto rc = File::remove(m_path.characters(), recursion_allowed, false);
    if (rc.is_error()) {
        warnln("File::remove failed: {}", rc.error().string_literal());
    }
}

}
