/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // FIXME: Remove this once we correctly define a proper set of pledge promises
    // (and if "exec" promise is not one of them).
    TRY(Core::System::prctl(PR_SET_NO_NEW_PRIVS, NO_NEW_PRIVS_MODE_ENFORCED, 0, 0));

    if (arguments.strings.size() < 2) {
        warnln("usage: fgrep <str>");
        return 1;
    }
    for (;;) {
        char buffer[4096];
        fgets(buffer, sizeof(buffer), stdin);
        auto str = StringView { buffer, strlen(buffer) };
        if (str.contains(arguments.strings[1]))
            TRY(Core::System::write(1, str.bytes()));
        if (feof(stdin))
            return 0;
        VERIFY(str.to_deprecated_string().characters());
    }
}
