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

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool recursive = false;
    bool force = false;
    bool verbose = false;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Delete directories recursively", "recursive", 'r');
    args_parser.add_option(force, "Force", "force", 'f');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(paths, "Path(s) to remove", "path");
    args_parser.parse(argc, argv);

    bool had_errors = false;
    for (auto& path : paths) {
        auto result = Core::File::remove(path, recursive ? Core::File::RecursionMode::Allowed : Core::File::RecursionMode::Disallowed, force);

        if (result.is_error()) {
            warnln("rm: cannot remove '{}': {}", path, result.error().error_code);
            had_errors = true;
        }

        if (verbose)
            outln("removed '{}'", path);
    }
    return had_errors ? 1 : 0;
}
