/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Forward.h>

namespace Core {

class StandardPaths {
public:
    static ByteString home_directory();
    static ByteString desktop_directory();
    static ByteString documents_directory();
    static ByteString downloads_directory();
    static ByteString tempfile_directory();
    static ByteString config_directory();
    static ByteString data_directory();
    static ErrorOr<ByteString> runtime_directory();
    static ErrorOr<Vector<String>> font_directories();
};

}
