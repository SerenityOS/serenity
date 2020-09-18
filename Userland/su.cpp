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
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/GetPassword.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

extern "C" int main(int, char**);

int main(int argc, char** argv)
{
    const char* user = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(user, "User to switch to (defaults to user with UID 0)", "user", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (geteuid() != 0)
        fprintf(stderr, "Not running as root :(\n");

    auto account_or_error = (user) ? Core::Account::from_name(user) : Core::Account::from_uid(0);
    if (account_or_error.is_error()) {
        fprintf(stderr, "Core::Account::from_name: %s\n", account_or_error.error().characters());
        return 1;
    }

    Core::Account account = account_or_error.value();

    if (getuid() != 0 && account.has_password()) {
        auto password = Core::get_password();
        if (password.is_error()) {
            fprintf(stderr, "%s\n", strerror(password.error()));
            return 1;
        }

        if (!account.authenticate(password.value().characters())) {
            fprintf(stderr, "Incorrect or disabled password.\n");
            return 1;
        }
    }

    if (!account.login()) {
        perror("Core::Account::login");
        return 1;
    }

    execl(account.shell().characters(), account.shell().characters(), nullptr);
    perror("execl");
    return 1;
}
