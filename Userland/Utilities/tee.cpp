/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/stat.h>
#include <unistd.h>

static ErrorOr<Vector<int>> collect_fds(Vector<StringView> paths, bool append)
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
    TRY(fds.try_ensure_capacity(paths.size() + 1));
    for (auto path : paths) {
        int fd = TRY(Core::System::open(path, oflag, mode));
        fds.unchecked_append(fd);
    }
    fds.unchecked_append(STDOUT_FILENO);
    return fds;
}

static ErrorOr<void> copy_stdin(Vector<int>& fds, bool* err)
{
    for (;;) {
        Array<u8, 4096> buffer;
        auto buffer_span = buffer.span();
        auto nread = TRY(Core::System::read(STDIN_FILENO, buffer_span));
        buffer_span = buffer_span.trim(nread);
        if (nread == 0)
            break;

        Vector<int> broken_fds;
        for (size_t i = 0; i < fds.size(); ++i) {
            auto fd = fds.at(i);
            auto nwrite_or_error = Core::System::write(fd, buffer_span);
            if (nwrite_or_error.is_error()) {
                if (nwrite_or_error.error().code() == EINTR) {
                    continue;
                } else {
                    warnln("{}", nwrite_or_error.release_error());
                    *err = true;
                    broken_fds.append(fd);
                    // write failures to a successfully opened fd shall
                    // prevent further writes, but shall not block writes
                    // to the other open fds
                    break;
                }
            }
        }

        // remove any fds which we can no longer write to for subsequent copies
        for (auto to_remove : broken_fds)
            fds.remove_first_matching([&](int fd) { return to_remove == fd; });
    }

    return {};
}

static ErrorOr<void> close_fds(Vector<int>& fds)
{
    for (int fd : fds)
        TRY(Core::System::close(fd));

    return {};
}

static void int_handler(int)
{
    // pass
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool append = false;
    bool ignore_interrupts = false;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(append, "Append, don't overwrite", "append", 'a');
    args_parser.add_option(ignore_interrupts, "Ignore SIGINT", "ignore-interrupts", 'i');
    args_parser.add_positional_argument(paths, "Files to copy stdin to", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (ignore_interrupts)
        TRY(Core::System::signal(SIGINT, int_handler));

    auto fds = TRY(collect_fds(paths, append));
    bool err_write = false;
    TRY(copy_stdin(fds, &err_write));
    TRY(close_fds(fds));

    return err_write ? 1 : 0;
}
