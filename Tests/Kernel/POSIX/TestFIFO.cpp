/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibCore/System.h>
#include <LibTest/TestCase.h>

namespace {

ErrorOr<ByteString> make_fifo()
{
    auto path = "/tmp/test-fifo"sv;
    (void)Core::System::unlink(path);

    TRY(Core::System::mkfifo(path, 0600));
    return path;
}

}

TEST_CASE(empty_path)
{
    auto error = Core::System::mkfifo(""sv, 0600);
    EXPECT(error.is_error());
    EXPECT_EQ(error.error().code(), ENOENT);
}

TEST_CASE(read_nonblock)
{
    auto path = TRY_OR_FAIL(make_fifo());
    int fd = TRY_OR_FAIL(Core::System::open(path, O_RDONLY | O_NONBLOCK));
    EXPECT(fd >= 0);

    close(fd);
    unlink(path.characters());
}

TEST_CASE(write_nonblock)
{
    auto path = TRY_OR_FAIL(make_fifo());
    auto error = Core::System::open(path, O_WRONLY | O_NONBLOCK);
    EXPECT(error.is_error());
    EXPECT_EQ(error.error().code(), ENXIO);

    int read_fd = TRY_OR_FAIL(Core::System::open(path, O_RDONLY | O_NONBLOCK));
    int write_fd = TRY_OR_FAIL(Core::System::open(path, O_WRONLY | O_NONBLOCK));

    EXPECT(read_fd >= 0);
    EXPECT(write_fd >= 0);

    close(read_fd);
    close(write_fd);
    unlink(path.characters());
}
