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

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

static Vector<int> collect_fds(Vector<const char*> paths, bool append, bool* err)
{
    int oflag;
    mode_t mode;
    if (append) {
        oflag = O_APPEND;
        mode = 0;
    } else {
        oflag = O_CREAT | O_WRONLY | O_TRUNC;
        mode = S_IROTH | S_IWOTH | S_IRGRP | S_IWGRP | S_IRUSR | S_IWUSR;
    }

    Vector<int> fds;
    for (const char* path : paths) {
        int fd = open(path, oflag, mode);
        if (fd < 0) {
            perror("failed to open file for writing");
            *err = true;
        } else {
            fds.append(fd);
        }
    }
    fds.append(STDOUT_FILENO);
    return fds;
}

static void copy_stdin(Vector<int>& fds, bool* err)
{
    for (;;) {
        char buf[4096];
        ssize_t nread = read(STDIN_FILENO, buf, sizeof(buf));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("read() error");
            *err = true;
            // a failure to read from stdin should lead to an early exit
            return;
        }

        Vector<int> broken_fds;
        for (size_t i = 0; i < fds.size(); ++i) {
            auto fd = fds.at(i);
            int twrite = 0;
            while (twrite != nread) {
                ssize_t nwrite = write(fd, buf + twrite, nread - twrite);
                if (nwrite < 0) {
                    if (errno == EINTR) {
                        continue;
                    } else {
                        perror("write() failed");
                        *err = true;
                        broken_fds.append(fd);
                        // write failures to a successfully opened fd shall
                        // prevent further writes, but shall not block writes
                        // to the other open fds
                        break;
                    }
                } else {
                    twrite += nwrite;
                }
            }
        }

        // remove any fds which we can no longer write to for subsequent copies
        for (auto to_remove : broken_fds)
            fds.remove_first_matching([&](int fd) { return to_remove == fd; });
    }
}

static void close_fds(Vector<int>& fds)
{
    for (int fd : fds) {
        int closed = close(fd);
        if (closed < 0) {
            perror("failed to close output file");
        }
    }
}

static void int_handler(int)
{
    // pass
}

int main(int argc, char** argv)
{
    bool append = false;
    bool ignore_interrupts = false;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(append, "Append, don't overwrite", "append", 'a');
    args_parser.add_option(ignore_interrupts, "Ignore SIGINT", "ignore-interrupts", 'i');
    args_parser.add_positional_argument(paths, "Files to copy stdin to", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (ignore_interrupts) {
        if (signal(SIGINT, int_handler) == SIG_ERR)
            perror("failed to install SIGINT handler");
    }

    bool err_open = false;
    bool err_write = false;
    auto fds = collect_fds(paths, append, &err_open);
    copy_stdin(fds, &err_write);
    close_fds(fds);

    return (err_open || err_write) ? 1 : 0;
}
