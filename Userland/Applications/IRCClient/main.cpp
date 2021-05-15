/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IRCAppWindow.h"
#include "IRCClient.h"
#include <LibCore/StandardPaths.h>
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio inet unix recvfd sendfd cpath rpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (getuid() == 0) {
        warnln("Refusing to run as root");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (unveil("/tmp/portal/lookup", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/notify", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(Core::StandardPaths::home_directory().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    URL url = "";
    if (app->args().size() >= 1) {
        url = URL::create_with_url_or_path(app->args()[0]);

        if (url.protocol().to_lowercase() == "ircs") {
            warnln("Secure IRC over SSL/TLS (ircs) is not supported");
            return 1;
        }

        if (url.protocol().to_lowercase() != "irc") {
            warnln("Unsupported protocol");
            return 1;
        }

        if (url.host().is_empty()) {
            warnln("Invalid URL");
            return 1;
        }

        if (!url.port() || url.port() == 80)
            url.set_port(6667);
    }

    auto app_window = IRCAppWindow::construct(url.host(), url.port());
    app_window->show();
    return app->exec();
}
