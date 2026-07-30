/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibTest/TestCase.h>
#include <unistd.h>

TEST_CASE(truncate_no_file)
{
    int rc = truncate("/tmp/i_do_not_exist", 42);
    EXPECT(rc < 0);
    EXPECT(errno == ENOENT);
}
