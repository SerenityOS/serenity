/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Emanuele Torre <torreemanuele6@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/GetPassword.h>
#include <stdio.h>
#include <stdlib.h>
#include <termios.h>
#include <unistd.h>

namespace Core {

Result<String, OSError> get_password(StringView const& prompt)
{
    if (write(STDOUT_FILENO, prompt.characters_without_null_termination(), prompt.length()) < 0)
        return OSError(errno);

    termios original {};
    if (tcgetattr(STDIN_FILENO, &original) < 0)
        return OSError(errno);

    termios no_echo = original;
    no_echo.c_lflag &= ~ECHO;
    if (tcsetattr(STDIN_FILENO, TCSAFLUSH, &no_echo) < 0)
        return OSError(errno);

    char* password = nullptr;
    size_t n = 0;

    auto line_length = getline(&password, &n, stdin);
    auto saved_errno = errno;

    tcsetattr(STDIN_FILENO, TCSAFLUSH, &original);
    putchar('\n');

    if (line_length < 0)
        return OSError(saved_errno);

    VERIFY(line_length != 0);

    // Remove trailing '\n' read by getline().
    password[line_length - 1] = '\0';

    String s(password);
    free(password);
    return s;
}
}
