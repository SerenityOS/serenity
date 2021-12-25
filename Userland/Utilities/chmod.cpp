/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@gmail.com>
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Vector.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath fattr"));

    if (arguments.strings.size() < 3) {
        warnln("usage: chmod <octal-mode> <path...>");
        warnln("       chmod [[ugoa][+-=][rwx...],...] <path...>");
        return 1;
    }

    auto mask = TRY(Core::FilePermissionsMask::parse(arguments.strings[1]));

    for (size_t i = 2; i < arguments.strings.size(); ++i) {
        auto current_access = TRY(Core::System::stat(arguments.strings[i]));
        TRY(Core::System::chmod(arguments.strings[i], mask.apply(current_access.st_mode)));
    }

    return 0;
}
