/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <Services/LoginServer/LoginWindow.h>
#include <errno.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

static void child_process(Core::Account const& account)
{
    if (!account.login()) {
        dbgln("failed to switch users: {}", strerror(errno));
        exit(1);
    }

    setenv("HOME", account.home_directory().characters(), true);
    pid_t rc = setsid();
    if (rc == -1) {
        dbgln("failed to setsid: {}", strerror(errno));
        exit(1);
    }
    dbgln("login with sid={}", rc);

    execlp("/bin/SystemServer", "SystemServer", "--user", nullptr);
    dbgln("failed to exec SystemServer --user: {}", strerror(errno));
    exit(127);
}

static void login(Core::Account const& account, LoginWindow& window)
{
    pid_t pid = fork();
    if (pid == 0)
        child_process(account);

    int wstatus;
    pid_t rc = waitpid(pid, &wstatus, 0);
    if (rc == -1)
        dbgln("waitpid failed: {}", strerror(errno));
    if (rc != 0)
        dbgln("SystemServer exited with non-zero status: {}", rc);

    window.show();
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath exec proc id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/home", "r") < 0) {
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

    if (unveil("/bin/SystemServer", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto window = LoginWindow::construct();
    window->on_submit = [&]() {
        auto username = window->username();
        auto password = Core::SecretString::take_ownership(window->password().to_byte_buffer());

        window->set_password("");

        auto account = Core::Account::from_name(username.characters());
        if (account.is_error()) {
            dbgln("failed graphical login for user {}: {}", username, account.error());
            return;
        }

        if (!account.value().authenticate(password)) {
            dbgln("failed graphical login for user {}: invalid password", username);
            return;
        }

        window->set_username("");
        window->hide();

        login(account.value(), *window);
    };

    char const* auto_login = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(auto_login, "automatically log in with no prompt", "auto-login", 'a', "username");
    args_parser.parse(argc, argv);

    if (!auto_login) {
        window->show();
    } else {
        auto account = Core::Account::from_name(auto_login);
        if (account.is_error()) {
            dbgln("failed auto-login for user {}: {}", auto_login, account.error());
            return 1;
        }

        login(account.value(), *window);
    }

    return app->exec();
}
