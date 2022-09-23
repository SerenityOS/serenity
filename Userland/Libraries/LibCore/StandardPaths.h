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
    static String home_directory();
    static String desktop_directory();
    static String documents_directory();
    static String downloads_directory();
    static String tempfile_directory();
    static String config_directory();
};

}
