/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteString.h>
#include <AK/StringBuilder.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

static constexpr char TMPDIR_PATTERN[] = "/tmp/overlong_realpath_XXXXXX";
static constexpr char PATH_LOREM_250[] = "This-is-an-annoyingly-long-name-that-should-take-up-exactly-two-hundred-and-fifty-characters-and-is-surprisingly-difficult-to-fill-with-reasonably-meaningful-text-which-is-necessary-because-that-makes-it-easier-for-my-eyes-to-spot-any-corruption-fast";

static constexpr size_t ITERATION_DEPTH = 17;

static void check_result(char const* what, StringView expected, char const* actual)
{
    if (expected != actual)
        FAIL(ByteString::formatted("Expected {} to be \"{}\" ({} characters)", what, actual, actual ? strlen(actual) : 0));
}

TEST_CASE(overlong_realpath)
{
    // We want to construct a path that is over PATH_MAX characters long.
    // This cannot be done in a single step.

    // First, switch to a known environment:
    char tmp_dir[] = "/tmp/overlong_realpath_XXXXXX";

    errno = 0;
    auto* new_dir = mkdtemp(tmp_dir);
    VERIFY(new_dir != nullptr);
    VERIFY(errno == 0);

    errno = 0;
    auto ret = chdir(tmp_dir);
    VERIFY(ret >= 0);
    VERIFY(errno == 0);

    // Then, create a long path.
    StringBuilder expected;
    expected.append({ tmp_dir, strlen(tmp_dir) });

    // But first, demonstrate the functionality at a reasonable depth:
    auto expected_str = expected.to_byte_string();
    check_result("getwd", expected_str, getwd(static_cast<char*>(calloc(1, PATH_MAX))));
    check_result("getcwd", expected_str, getcwd(nullptr, 0));
    check_result("realpath", expected_str, realpath(".", nullptr));

    for (size_t i = 0; i < ITERATION_DEPTH; ++i) {
        ret = mkdir(PATH_LOREM_250, S_IRWXU);
        if (ret < 0) {
            perror("mkdir iter");
            FAIL(ByteString::formatted("Unable to mkdir the overlong path fragment in iteration {}", i));
            return;
        }
        expected.append('/');
        expected.append({ PATH_LOREM_250, strlen(PATH_LOREM_250) });
        ret = chdir(PATH_LOREM_250);
        if (ret < 0) {
            perror("chdir iter");
            FAIL(ByteString::formatted("Unable to chdir to the overlong path fragment in iteration {}", i));
            return;
        }
    }
    outln("cwd should now be ridiculously large");

    // Evaluate
    expected_str = expected.to_byte_string();

    check_result("getwd", {}, getwd(static_cast<char*>(calloc(1, PATH_MAX))));
    check_result("getcwd", expected_str, getcwd(nullptr, 0));
    check_result("realpath", expected_str, realpath(".", nullptr));

    VERIFY(strlen(PATH_LOREM_250) == 250);
    VERIFY(strlen(TMPDIR_PATTERN) + ITERATION_DEPTH * (1 + strlen(PATH_LOREM_250)) == expected_str.length());
    VERIFY(expected_str.length() > PATH_MAX);
}
