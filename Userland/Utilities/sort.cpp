/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main([[maybe_unused]] Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"sv));

    Vector<String> lines;

    for (;;) {
        char* buffer = nullptr;
        ssize_t buflen = 0;
        size_t n;
        errno = 0;
        buflen = getline(&buffer, &n, stdin);
        if (buflen == -1 && errno != 0) {
            perror("getline");
            exit(1);
        }
        if (buflen == -1)
            break;
        lines.append({ buffer, AK::ShouldChomp::Chomp });
    }

    quick_sort(lines);

    for (auto& line : lines) {
        outln("{}", line);
    }

    return 0;
}
