/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <AK/Base64.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/GetPassword.h>
#include <pwd.h>
#include <string.h>
#include <unistd.h>

static String get_salt()
{
    char random_data[12];
    arc4random_buf(random_data, sizeof(random_data));

    StringBuilder builder;
    builder.append("$5$");
    builder.append(encode_base64(ReadonlyBytes(random_data, sizeof(random_data))));

    return builder.build();
}

static void write_passwd_file(struct passwd* pwd)
{
    StringBuilder new_passwd_file;

    setpwent();

    struct passwd* p;
    while ((p = getpwent())) {
        if (p->pw_uid == pwd->pw_uid) {
            p = pwd;
        }

        new_passwd_file.appendf("%s:%s:%u:%u:%s:%s:%s\n",
            p->pw_name, p->pw_passwd, p->pw_uid, p->pw_gid, p->pw_gecos, p->pw_dir, p->pw_shell);
    }
    endpwent();

    auto file = Core::File::open("/etc/passwd", Core::IODevice::OpenMode::WriteOnly);
    if (file.is_error()) {
        fprintf(stderr, "Core::File::open: %s\n", file.error().characters());
        exit(1);
    }
    file.value()->write(new_passwd_file.build());
}

int main(int argc, char** argv)
{
    if (geteuid() != 0) {
        fprintf(stderr, "Not running as root :^(\n");
        return 1;
    }

    if (pledge("stdio wpath rpath cpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc/passwd", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    bool del = false;
    bool lock = false;
    bool unlock = false;
    const char* username = nullptr;

    auto args_parser = Core::ArgsParser();
    args_parser.add_option(del, "Delete password", "delete", 'd');
    args_parser.add_option(lock, "Lock password", "lock", 'l');
    args_parser.add_option(unlock, "Unlock password", "unlock", 'u');
    args_parser.add_positional_argument(username, "Username", "username", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    uid_t current_user = getuid();

    struct passwd pwd;
    if (username) {
        pwd = *getpwnam(username);
        if (current_user != 0 && current_user != pwd.pw_uid) {
            fprintf(stderr, "You can't modify passwd for %s.\n", username);
            return 1;
        }
    } else {
        pwd = *getpwuid(current_user);
    }

    String pw_string;
    if (del) {
        pwd.pw_passwd = const_cast<char*>("");
    } else if (lock) {
        StringBuilder builder;
        builder.append('!');
        builder.append(pwd.pw_passwd);
        pw_string = builder.build();
        pwd.pw_passwd = const_cast<char*>(pw_string.characters());
    } else if (unlock) {
        if (pwd.pw_passwd[0] == '!') {
            pwd.pw_passwd += 1;
        }
    } else {
        auto new_password = Core::get_password("New password: ");
        if (new_password.is_error()) {
            fprintf(stderr, strerror(new_password.error()));
            return 1;
        }

        pwd.pw_passwd = crypt(new_password.value().characters(), get_salt().characters());
    }

    write_passwd_file(&pwd);

    return 0;
}
