/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/GetPassword.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static constexpr mode_t EXPECTED_PERMS = (S_IFREG | S_IRUSR);

// Function Definitions
extern "C" int main(int arch, char** argv);
bool unveil_paths(const char*);

// Unveil paths, given the current user's path and the command they want to execute
bool unveil_paths(const char* command)
{
    // Unveil all trusted paths with browse permissions
    auto trusted_directories = Array { "/bin", "/usr/bin", "/usr/local/bin" };
    for (auto directory : trusted_directories) {
        if (unveil(directory, "b") < 0) {
            perror("unveil");
            return false;
        }
    }

    // Attempt to unveil command via `realpath`
    auto command_path = Core::File::real_path_for(command);

    // Command found via `realpath` (meaning it was probably a locally executed program)
    if (!command_path.is_empty()) {
        if (unveil(command_path.characters(), "x") == 0)
            return true;
        return false;
    }

    auto command_path_system = Core::find_executable_in_path(command);
    if (command_path_system.is_empty())
        return false;
    if (unveil(command_path_system.characters(), "x") < 0)
        return false;
    return true;
}

int main(int argc, char** argv)
{
    Vector<const char*> command;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command, "Command to run at elevated privilege level", "command");
    args_parser.parse(argc, argv);

    // Unveil command path.
    // Fail silently to prevent disclosing whether the specified path is valid
    auto command_path = LexicalPath(Core::File::real_path_for(command.at(0))).dirname();
    unveil(command_path.characters(), "b");

    if (pledge("stdio tty rpath exec id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc/plsusers", "r") < 0) {
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

    // Find and unveil the user's command executable.
    // Fail silently to prevent disclosing whether the specified path is valid
    unveil_paths(command.at(0));

    // Lock veil
    unveil(nullptr, nullptr);

    // Call `seteuid` so we can access `/etc/plsusers`
    if (seteuid(0) < 0) {
        perror("seteuid");
        return 1;
    }

    // Check the permissions and owner of /etc/plsusers. This ensures the integrity of the file.
    struct stat pls_users_stat;
    if (stat("/etc/plsusers", &pls_users_stat) < 0) {
        perror("stat");
        return 1;
    }

    if (pls_users_stat.st_mode != EXPECTED_PERMS) {
        warnln("Error: /etc/plsusers has incorrect permissions.");
        return 4;
    }

    if (pls_users_stat.st_uid != 0 && pls_users_stat.st_gid != 0) {
        warnln("Error: /etc/plsusers is not owned by root.");
        return 4;
    }

    auto pls_users_file_or_error = Core::File::open("/etc/plsusers", Core::OpenMode::ReadOnly);
    if (pls_users_file_or_error.is_error()) {
        warnln("Error: Could not open /etc/plsusers: {}", pls_users_file_or_error.error());
        return 1;
    }

    const char* username = getlogin();
    bool user_found = false;
    for (auto line = pls_users_file_or_error.value()->line_begin(); !line.at_end(); ++line) {
        auto line_str = *line;

        // Skip any comments
        if (line_str.starts_with("#"))
            continue;

        // Our user is in the plsusers file!
        if (line_str.to_string() == username) {
            user_found = true;
            break;
        }
    }

    // User isn't in the plsusers file
    if (!user_found) {
        warnln("{} is not in the plsusers file!", username);
        return 2;
    }

    // The user was in the plsusers file, now let's ask for their password to ensure that it's actually them...
    auto account_or_error = Core::Account::from_name(username);

    if (account_or_error.is_error()) {
        warnln("Core::Account::from_name: {}", account_or_error.error());
        return 1;
    }

    const auto& account = account_or_error.value();
    uid_t current_uid = getuid();
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
    if (setgid(0) < 0) {
        perror("setgid");
        return 1;
    }

    if (setuid(0) < 0) {
        perror("setuid");
        return 1;
    }

    // Build the arguments list passed to `execvpe`
    Vector<const char*> exec_args;
    for (const auto& arg : command) {
        exec_args.append(arg);
    }

    // Always terminate with a NULL (to signal end of args list)
    exec_args.append(nullptr);

    // Build the environment arguments
    StringBuilder builder;
    Vector<String> env_args_str;

    // TERM envvar
    char* env_term = getenv("TERM");

    if (env_term != nullptr) {
        builder.append("TERM=");
        builder.append(env_term);
        env_args_str.append(builder.build());
    }

    Vector<const char*> env_args;
    for (auto& arg : env_args_str) {
        env_args.append(arg.characters());
    }

    // Arguments list must be terminated with NULL argument
    env_args.append(nullptr);

    // Execute the desired command
    if (execvpe(command.at(0), const_cast<char**>(exec_args.data()), const_cast<char**>(env_args.data())) < 0) {
        perror("execvpe");
        exit(1);
    }

    return 0;
}
