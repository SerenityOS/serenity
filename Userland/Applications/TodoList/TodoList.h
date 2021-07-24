/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "TaskListModel.h"

#include <LibCore/StandardPaths.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>

#include <unistd.h>

class TodoList : public GUI::Window {
    C_OBJECT(TodoList);

    RefPtr<GUI::Menu> file_menu;
    RefPtr<GUI::Menu> help_menu;
    RefPtr<GUI::Widget> main_widget;
    RefPtr<GUI::ListView> list_view;
    RefPtr<GUI::Button> add_task;
    RefPtr<GUI::Button> set_done;
    RefPtr<GUI::Label> todo_title;
    RefPtr<GUI::Button> delete_task;

    TaskListModel* task_list_model = nullptr;

    int currently_selected = -1;

    String user_file_path;

public:
    TodoList();

    virtual ~TodoList() override
    {
    }

    GUI::Menu& get_file_menu();
};
