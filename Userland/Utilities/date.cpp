/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio settime", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool print_unix_date = false;
    bool print_iso_8601 = false;
    bool print_rfc_3339 = false;
    bool print_rfc_5322 = false;
    const char* set_date = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(set_date, "Set system date and time", "set", 's', "date");
    args_parser.add_option(print_unix_date, "Print date as Unix timestamp", "unix", 'u');
    args_parser.add_option(print_iso_8601, "Print date in ISO 8601 format", "iso-8601", 'i');
    args_parser.add_option(print_rfc_3339, "Print date in RFC 3339 format", "rfc-3339", 'r');
    args_parser.add_option(print_rfc_5322, "Print date in RFC 5322 format", "rfc-5322", 'R');
    args_parser.parse(argc, argv);

    if (set_date != nullptr) {
        auto number = String(set_date).to_uint();

        if (!number.has_value()) {
            fprintf(stderr, "date: Invalid timestamp value");
            return 1;
        }

        timespec ts = { number.value(), 0 };
        if (clock_settime(CLOCK_REALTIME, &ts) < 0) {
            perror("clock_settime");
            return 1;
        }

        return 0;
    }

    // FIXME: this should be improved and will need to be cleaned up
    // when additional output formats and formatting is supported
    if (print_unix_date && print_iso_8601 && print_rfc_3339 && print_rfc_5322) {
        fprintf(stderr, "date: multiple output formats specified\n");
        return 1;
    }

    time_t now = time(nullptr);
    auto date = Core::DateTime::from_timestamp(now);

    if (print_unix_date) {
        printf("%lld\n", (long long)now);
        return 0;
    } else if (print_iso_8601) {
        printf("%s\n", date.to_string("%Y-%m-%dT%H:%M:%S-00:00").characters());
        return 0;
    } else if (print_rfc_5322) {
        printf("%s\n", date.to_string("%a, %d %b %Y %H:%M:%S -0000").characters());
        return 0;
    } else if (print_rfc_3339) {
        printf("%s\n", date.to_string("%Y-%m-%d %H:%M:%S-00:00").characters());
        return 0;
    } else {
        printf("%s\n", date.to_string().characters());
        return 0;
    }
}
