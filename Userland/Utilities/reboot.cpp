/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    auto file = TRY(Core::Stream::File::open("/sys/kernel/power_state"sv, Core::Stream::OpenMode::Write));

    const String file_contents = "1";
    TRY(file->write(file_contents.bytes()));
    file->close();

    return 0;
}
