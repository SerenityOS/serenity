/*
 * Copyright (c) 2021 Jesse Buhagiar <jooster669@gmail.com>
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
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/GetPassword.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

// Function Definitions
extern "C" int main(int arch, char** argv);
int unveil_paths(const char*);

// Unveil paths, given the current user's path and the command they want to execute
int unveil_paths(const char* command)
{
    // Get the system path, split it and attempt to unveil all the paths.
    // We do NOT error out on an invalid path
    auto paths = String(getenv("PATH")).split(':');
    int num_unveils = 0;
    char path_buf[256];

    // Unveil each path
    for (const auto& path : paths) {
        if (unveil(path.characters(), "x") == 0)
            num_unveils++;
    }

    // Now unveil the command
    auto command_path = realpath(command, &path_buf[0]);
    if (command_path) {
        if (unveil(command_path, "x") == 0)
            num_unveils++;
    }

    return num_unveils;
}

// <kling> linusg: quaker: "please" feels quite long, how about "pls" :P
// <kling> "pls rm -r crap" has a nice ring to it
// lol
int main(int argc, char** argv)
{
    Vector<const char*> command;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command, "Command to run at elevated privilege level", "command");

    args_parser.parse(argc, argv);

    if (pledge("stdio tty rpath exec id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (seteuid(0) < 0) {
        perror("seteuid");
        return 1;
    }

    if (unveil("/etc/sudoers", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/shadow", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/group", "r") < 0) {
        perror("unveil");
        return 1;
    }

    // Unveil all paths in the user's PATH, as well as the command they've specified.
    auto unveil_count = unveil_paths(command.at(0));
    if (unveil_count == 0) {
        warnln("Failed to unveil paths!");
        return 1;
    }

    // Lock veil
    unveil(nullptr, nullptr);

    const char* username = getlogin();
    auto sudoers_file_or_error = Core::File::open("/etc/sudoers", Core::IODevice::ReadOnly);
    bool user_found = false;
    if (sudoers_file_or_error.is_error()) {
        warnln("couldn't open /etc/sudoers!");
        return 1;
    }

    for (auto line = sudoers_file_or_error.value()->line_begin(); !line.at_end(); ++line) {
        auto line_str = *line;

        // Skip any comments
        if (line_str.starts_with("#"))
            continue;

        // Our user is in the sudoers file!
        if (line_str.to_string() == username) {
            user_found = true;
            break;
        }
    }

    // User isn't in the sudoer's file
    if (!user_found) {
        warnln("{} is not in the sudoers file!", username);
        return 2;
    }

    // The user was in the sudoers file, now let's ask for their password to ensure that it's actually them...
    uid_t current_uid = getuid();
    auto account_or_error = (username)
        ? Core::Account::from_name(username)
        : Core::Account::from_uid(current_uid);

    if (account_or_error.is_error()) {
        warnln("Core::Account::from_name: {}", account_or_error.error());
        return 1;
    }

    const auto& account = account_or_error.value();
    if (current_uid != 0 && account.has_password()) {
        auto password = Core::get_password();
        if (password.is_error()) {
            warnln("{}", password.error());
            return 1;
        }

        if (!account.authenticate(password.value().characters())) {
            warnln("Incorrect or disabled password.");
            return 1;
        }
    }

    // TODO: Support swapping users instead of just defaulting to root
    setgid(0);
    setuid(0);

    // Build the arguments list passed to `execvpe`
    Vector<const char*> exec_args;
    for (size_t i = 0; i < command.size(); i++) {
        exec_args.append(command.at(i));
    }

    // Always terminate with a NULL (to signal end of args list)
    exec_args.append(nullptr);

    // Build the environment arguments
    StringBuilder builder;

    // Build SUDO_USER envvar
    builder.append("SUDO_USER=");
    builder.append(username);
    auto sudo_user = builder.build();
    builder.clear();

    // Build SUDO_COMMAND envvar
    builder.append("SUDO_COMMAND=");
    builder.append(command.at(0));
    auto sudo_command = builder.build();
    builder.clear();

    const char* envs[] = { "PROMPT=\\X\\u@\\h:\\w\\a\\e[33;1m\\h\\e[0m \\e[34;1m\\w\\e[0m \\p ",
        "TERM=xterm", "PAGER=more", "PATH=/bin:/usr/bin:/usr/local/bin",
        sudo_user.characters(), sudo_command.characters(), nullptr };

    // Execute the desired command
    int rc = execvpe(command.at(0), const_cast<char**>(exec_args.data()), const_cast<char**>(envs));
    if (rc < 0) {
        perror("execvpe");
        exit(1);
    }

    return 0;
}
