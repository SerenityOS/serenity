/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <inttypes.h>
#include <sys/stat.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    DeprecatedString target;
    int max_file_size = 1024 * 1024;
    int count = 1024;

    Core::ArgsParser args_parser;
    TRY(args_parser.add_option(max_file_size, "Maximum file size to generate", "max-size", 's', "size"));
    TRY(args_parser.add_option(count, "Number of truncations to run", "number", 'n', "number"));
    TRY(args_parser.add_positional_argument(target, "Target file path", "target"));
    TRY(args_parser.parse(arguments));

    int fd = creat(target.characters(), 0666);
    if (fd < 0) {
        perror("Couldn't create target file");
        return EXIT_FAILURE;
    }
    close(fd);

    for (int i = 0; i < count; i++) {
        auto new_file_size = AK::get_random<uint64_t>() % (max_file_size + 1);
        printf("(%d/%d)\tTruncating to %" PRIu64 " bytes...\n", i + 1, count, new_file_size);
        TRY(Core::System::truncate(target, new_file_size));
    }

    TRY(Core::System::unlink(target));

    return EXIT_SUCCESS;
}
