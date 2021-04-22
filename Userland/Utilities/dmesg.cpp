/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibCore/File.h>
#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc/dmesg", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto f = Core::File::construct("/proc/dmesg");
    if (!f->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "open: failed to open /proc/dmesg: %s\n", f->error_string());
        return 1;
    }
    const auto& b = f->read_all();
    for (size_t i = 0; i < b.size(); ++i)
        putchar(b[i]);
    return 0;
}
