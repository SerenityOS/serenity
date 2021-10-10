/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <Services/LoginServer/LoginWindow.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto window = LoginWindow::construct();
    window->show();

    return app->exec();
}
