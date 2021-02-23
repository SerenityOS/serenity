/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
static const char* TEXT_FAIL = "\x1b[01;31m";
static const char* TEXT_PASS = "\x1b[01;32m";
static const char* TEXT_RESET = "\x1b[0m";

static const char* TMPDIR_PATTERN = "/tmp/overlong_realpath_XXXXXX";
static const char* PATH_LOREM_250 = "This-is-an-annoyingly-long-name-that-should-take-up-exactly-two-hundred-and-fifty-characters-and-is-surprisingly-difficult-to-fill-with-reasonably-meaningful-text-which-is-necessary-because-that-makes-it-easier-for-my-eyes-to-spot-any-corruption-fast";

static const size_t ITERATION_DEPTH = 17;

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
