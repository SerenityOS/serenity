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

#include "IRCAppWindow.h"
#include "IRCClient.h"
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio inet dns unix shared_buffer cpath rpath fattr wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (getuid() == 0) {
        warnln("Refusing to run as root");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio inet dns unix shared_buffer rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
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
