/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Emanuele Torre <torreemanuele6@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/GetPassword.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <termios.h>
#include <unistd.h>

namespace Core {

ErrorOr<SecretString> get_password(StringView prompt)
{
    TRY(Core::System::write(STDOUT_FILENO, prompt.bytes()));

    auto original = TRY(Core::System::tcgetattr(STDIN_FILENO));

    termios no_echo = original;
    no_echo.c_lflag &= ~ECHO;
    TRY(Core::System::tcsetattr(STDIN_FILENO, TCSAFLUSH, no_echo));

    char* password = nullptr;
    size_t n = 0;

    auto line_length = getline(&password, &n, stdin);
    auto saved_errno = errno;

    tcsetattr(STDIN_FILENO, TCSAFLUSH, &original);
    putchar('\n');

    if (line_length < 0)
        return Error::from_errno(saved_errno);

    VERIFY(line_length != 0);

    // Remove trailing '\n' read by getline().
    password[line_length - 1] = '\0';

    return TRY(SecretString::take_ownership(password, line_length));
}
}
