/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/LexicalPath.h>
#include <AK/LogStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibSymbolClient/Client.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/rpc", "crw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/symbol", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/usr/src", "b") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    char hostname[256];
    if (gethostname(hostname, sizeof(hostname)) < 0) {
        perror("gethostname");
        return 1;
    }

    Core::ArgsParser args_parser;
    pid_t pid = 0;
    args_parser.add_positional_argument(pid, "PID", "pid");
    args_parser.parse(argc, argv);
    Core::EventLoop loop;

    // FIXME: Support multiple threads in the same process!
    auto symbols = SymbolClient::symbolicate_thread(pid, pid);
    for (auto& symbol : symbols) {
        out("{:p}  ", symbol.address);
        if (!symbol.name.is_empty())
            out("{} ", symbol.name);
        if (!symbol.filename.is_empty()) {
            bool linked = false;

            out("(");

            // See if we can find the sources in /usr/src
            // FIXME: I'm sure this can be improved!
            auto full_path = LexicalPath::canonicalized_path(String::formatted("/usr/src/serenity/dummy/{}", symbol.filename));
            if (access(full_path.characters(), F_OK) == 0) {
                linked = true;
                out("\033]8;;file://{}{}\033\\", hostname, full_path);
            }

            out("\033[34;1m{}\033[0m:{}", LexicalPath(symbol.filename).basename(), symbol.line_number);

            if (linked)
                out("\033]8;;\033\\");

            out(")");
        }
        outln("");
    }
    return 0;
}
