/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

namespace Main {

struct Arguments {
    int argc {};
    char** argv {};
    Span<StringView> strings;
};

}

ErrorOr<int> serenity_main(Main::Arguments);
