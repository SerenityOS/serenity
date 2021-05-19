/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

// FIXME
static constexpr char TEXT_FAIL[] = "\x1b[01;31m";
static constexpr char TEXT_PASS[] = "\x1b[01;32m";
static constexpr char TEXT_RESET[] = "\x1b[0m";

static constexpr char TMPDIR_PATTERN[] = "/tmp/overlong_realpath_XXXXXX";
static constexpr char PATH_LOREM_250[] = "This-is-an-annoyingly-long-name-that-should-take-up-exactly-two-hundred-and-fifty-characters-and-is-surprisingly-difficult-to-fill-with-reasonably-meaningful-text-which-is-necessary-because-that-makes-it-easier-for-my-eyes-to-spot-any-corruption-fast";

static constexpr size_t ITERATION_DEPTH = 17;

static bool check_result(const char* what, const String& expected, const char* actual)
{
    bool good = expected == actual;
    printf("%s%s%s: %s = \"%s\" (%zu characters)\n", good ? TEXT_PASS : TEXT_FAIL, good ? "GOOD" : "FAIL", TEXT_RESET, what, actual, actual ? strlen(actual) : 0);
    return good;
}

int main()
{
    // We want to construct a path that is over PATH_MAX characters long.
    // This cannot be done in a single step.

    // First, switch to a known environment:
    char* tmp_dir = strdup("/tmp/overlong_realpath_XXXXXX");
    if (!mkdtemp(tmp_dir)) {
        perror("mkdtmp");
        return 1;
    }
    if (chdir(tmp_dir) < 0) {
        perror("chdir tmpdir");
        return 1;
    }

    // Then, create a long path.
    StringBuilder expected;
    expected.append(tmp_dir);

    // But first, demonstrate the functionality at a reasonable depth:
    bool all_good = true;
    auto expected_str = expected.build();
    all_good &= check_result("getwd", expected_str, getwd(static_cast<char*>(calloc(1, PATH_MAX))));
    all_good &= check_result("getcwd", expected_str, getcwd(nullptr, 0));
    all_good &= check_result("realpath", expected_str, realpath(".", nullptr));

    for (size_t i = 0; i < ITERATION_DEPTH; ++i) {
        if (mkdir(PATH_LOREM_250, 0700) < 0) {
            perror("mkdir iter");
            printf("%sFAILED%s in iteration %zu.\n", TEXT_FAIL, TEXT_RESET, i);
            return 1;
        }
        expected.append('/');
        expected.append(PATH_LOREM_250);
        if (chdir(PATH_LOREM_250) < 0) {
            perror("chdir iter");
            printf("%sFAILED%s in iteration %zu.\n", TEXT_FAIL, TEXT_RESET, i);
            return 1;
        }
    }
    printf("cwd should now be ridiculously large.\n");

    // Evaluate
    expected_str = expected.build();

    all_good &= check_result("getwd", {}, getwd(static_cast<char*>(calloc(1, PATH_MAX))));
    all_good &= check_result("getcwd", expected_str, getcwd(nullptr, 0));
    all_good &= check_result("realpath", expected_str, realpath(".", nullptr));

    VERIFY(strlen(PATH_LOREM_250) == 250);
    VERIFY(strlen(TMPDIR_PATTERN) + ITERATION_DEPTH * (1 + strlen(PATH_LOREM_250)) == expected_str.length());
    VERIFY(expected_str.length() > PATH_MAX);

    if (all_good) {
        printf("Overall: %sPASS%s\n", TEXT_PASS, TEXT_RESET);
        return 0;
    } else {
        printf("Overall: %sFAIL%s\n", TEXT_FAIL, TEXT_RESET);
        return 2;
    }
}
