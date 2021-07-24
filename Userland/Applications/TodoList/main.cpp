/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TodoList.h"
#include <LibCore/LockFile.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <WindowServer/WindowManager.h>

int main(int argc, char** argv)
{
    const auto user_file_path = String::formatted("{}/todolist.json", Core::StandardPaths::config_directory());
    const auto personalLockPath = String::formatted("/tmp/lock/todolist-{}.lock", getuid());

    dbgln("Starting TodoList");
    dbgln("todo_file_path {}", user_file_path);
    dbgln("lock_file_path {}", personalLockPath);

    Core::LockFile lockfile(personalLockPath.characters());

    if (!lockfile.is_held()) {
        dbgln("There's another instance of todolist");
        return 0;
    }

    dbgln("this is the primary instance of todolist");

    if (unveil(user_file_path.characters(), "rwc") < 0) {
        perror("unveil $HOME/.todolist.json");
        return -1;
    }

    if (unveil(personalLockPath.characters(), "rwc") < 0) {
        perror("unveil $HOME/.todolist.lock");
        return -1;
    }

    if (unveil("/res/", "r") < 0) {
        perror("unveil /res");
        return -1;
    }

    if (unveil("/tmp/", "rwcb") < 0) {
        perror("unveil /tmp");
        return -1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil nullptr");
        return -1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto window = TodoList::construct();
    auto& file_menu = window->get_file_menu();

    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        window->close();
        app->quit();
    }));

    window->show();
    return app->exec();
}
