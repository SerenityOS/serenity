/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TodoList.h"

TodoList::TodoList()
    : GUI::Window()
{
    user_file_path = String::formatted("{}/todolist.json", Core::StandardPaths::config_directory());

    auto app_icon = GUI::Icon::default_icon("app-todolist");
    set_title("Todo List");
    resize(200, 350);
    set_icon(app_icon.bitmap_for_size(16));

    file_menu = add_menu("&File");
    help_menu = add_menu("&Help");
    help_menu->add_action(GUI::CommonActions::make_about_action("Todo List",
        app_icon,
        this));

    main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();

    list_view = main_widget->add<GUI::ListView>();
    list_view->set_alternating_row_colors(true);
    list_view->set_selection_mode(GUI::AbstractView::SelectionMode::SingleSelection);
    list_view->on_selection_change = [this]() {
        auto& selection = list_view->selection();
        if (!selection.is_empty())
            currently_selected = selection.first().row();
        else
            currently_selected = -1;
    };

    set_done = main_widget->add<GUI::Button>("Set selection as done");
    set_done->set_tooltip("Mark currently selected tats as DONE");
    set_done->on_click = [this](auto) {
        if (currently_selected != -1) {
            task_list_model->get_from_index(currently_selected).set_state(Task::State::done);
            list_view->update();
        }
    };

    delete_task = main_widget->add<GUI::Button>("Delete selected task");
    delete_task->set_tooltip("Delete the currenlty selected task");
    delete_task->on_click = [this](auto) {
        if (currently_selected != -1) {
            task_list_model->remove_task_from_index(currently_selected);
            list_view->update();
        }
    };

    add_task = main_widget->add<GUI::Button>("Add new task...");
    add_task->set_tooltip("Add a new task to the todo list");
    add_task->on_click = [this](auto) {
        String task_title;
        if (GUI::InputBox::show(this, task_title, "Title:", "Add new task") != GUI::InputBox::ExecCancel) {
            this->task_list_model->add_task(Task(task_title));
            this->list_view->update();
        }
    };

    list_view->set_model(TaskListModel::create());
    task_list_model = static_cast<TaskListModel*>(list_view->model());
    task_list_model->load_from_disk(user_file_path);

    on_close = [this]() {
        task_list_model->save_to_disk(user_file_path);
    };
}

GUI::Menu& TodoList::get_file_menu()
{
    return *file_menu;
}
