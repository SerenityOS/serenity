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
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int remove(bool recursive, const char* path)
{
    struct stat path_stat;
    if (lstat(path, &path_stat) < 0) {
        perror("lstat");
        return 1;
    }

    if (S_ISDIR(path_stat.st_mode) && recursive) {
        DIR* derp = opendir(path);
        if (!derp) {
            return 1;
        }

        while (auto* de = readdir(derp)) {
            if (strcmp(de->d_name, ".") != 0 && strcmp(de->d_name, "..") != 0) {
                StringBuilder builder;
                builder.append(path);
                builder.append('/');
                builder.append(de->d_name);
                int s = remove(true, builder.to_string().characters());
                if (s < 0)
                    return s;
            }
        }
        int s = rmdir(path);
        if (s < 0) {
            perror("rmdir");
            return 1;
        }
    } else {
        int rc = unlink(path);
        if (rc < 0) {
            perror("unlink");
            return 1;
        }
    }
    return 0;
}

int main(int argc, char** argv)
{
    CArgsParser args_parser("rm");
    args_parser.add_arg("r", "Delete directory recursively.");
    args_parser.add_required_single_value("path");

    CArgsParserResult args = args_parser.parse(argc, argv);
    Vector<String> values = args.get_single_values();
    if (values.size() == 0) {
        args_parser.print_usage();
        return 1;
    }

    return remove(args.is_present("r"), values[0].characters());
}
