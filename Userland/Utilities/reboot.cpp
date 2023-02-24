/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    auto file = TRY(Core::File::open("/sys/kernel/power_state"sv, Core::File::OpenMode::Write));

    const DeprecatedString file_contents = "1";
    // FIXME: This should write the entire span.
    TRY(file->write_some(file_contents.bytes()));
    file->close();

    return 0;
}
