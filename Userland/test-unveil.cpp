/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    Vector<StringView> paths_to_test;
    const char* permissions = "r";
    bool should_sleep = false;

    Core::ArgsParser parser;
    parser.add_option(permissions, "Apply these permissions going forward", "permissions", 'p', "unveil-permissions");
    parser.add_option(should_sleep, "Sleep after processing all arguments", "sleep", 's');
    parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Add a path to the unveil list",
        .long_name = "unveil",
        .short_name = 'u',
        .value_name = "path",
        .accept_value = [&](auto* s) {
            StringView path { s };
            if (path.is_empty())
                return false;
            if (unveil(s, permissions) < 0) {
                perror("unveil");
                return false;
            }
            return true;
        } });
    parser.add_option(Core::ArgsParser::Option {
        .requires_argument = false,
        .help_string = "Lock the veil",
        .long_name = "lock",
        .short_name = 'l',
        .accept_value = [&](auto*) {
            if (unveil(nullptr, nullptr) < 0) {
                perror("unveil(nullptr, nullptr)");
                return false;
            }
            return true;
        } });
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Test a path against the veil",
        .name = "path",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](auto* s) {
            if (access(s, X_OK) == 0)
                warnln("'{}' - ok", s);
            else
                warnln("'{}' - fail: {}", s, strerror(errno));
            return true;
        } });

    parser.parse(argc, argv);
    if (should_sleep)
        sleep(INT_MAX);
    return 0;
}
