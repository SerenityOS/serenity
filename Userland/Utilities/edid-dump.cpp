/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringUtils.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibEDID/EDID.h>
#include <LibMain/Main.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath"));

    StringView display_connector_file_name;
    Core::ArgsParser args;
    args.add_positional_argument(display_connector_file_name, "Display Connector Device Path", "display connector file name", Core::ArgsParser::Required::Yes);

    args.parse(arguments);

    auto edid = TRY(EDID::Parser::from_framebuffer_device(display_connector_file_name, 0));

    out("{}", StringView { edid.bytes() });
    return 0;
}
