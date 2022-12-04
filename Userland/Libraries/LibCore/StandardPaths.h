/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Core {

class StandardPaths {
public:
    static DeprecatedString home_directory();
    static DeprecatedString desktop_directory();
    static DeprecatedString documents_directory();
    static DeprecatedString downloads_directory();
    static DeprecatedString tempfile_directory();
    static DeprecatedString config_directory();
};

}
