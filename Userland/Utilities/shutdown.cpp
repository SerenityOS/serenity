/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    auto file = TRY(Core::Stream::File::open("/sys/firmware/power_state", Core::Stream::OpenMode::Write));

    const String file_contents = "2";
    TRY(file->write(file_contents.bytes()));
    file->close();

    return 0;
}
