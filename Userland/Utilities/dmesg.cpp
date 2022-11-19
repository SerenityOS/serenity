/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/dmesg", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::Stream::File::open("/sys/kernel/dmesg"sv, Core::Stream::OpenMode::Read));
    auto buffer = TRY(file->read_all());
    out("{}", StringView { buffer });
    return 0;
}
