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
    resize(300, 350);
    set_icon(app_icon.bitmap_for_size(16));

    file_menu = add_menu("&File");
    help_menu = add_menu("&Help");
    help_menu->add_action(GUI::CommonActions::make_about_action("Todo List",
        app_icon,
        this));

    main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::HorizontalBoxLayout>();
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto& button_group = main_widget->add<GUI::Widget>();
    button_group.set_fixed_width(110);
    button_group.set_layout<GUI::VerticalBoxLayout>();
    button_group.layout()->set_margins({ 8, 16, 8, 8 });

    auto& todo_frame = main_widget->add<GUI::GroupBox>();
    todo_frame.set_title("Tasks:");
    todo_frame.set_layout<GUI::VerticalBoxLayout>();
    todo_frame.layout()->set_margins({ 8, 16, 8, 8 });

    set_done = button_group.add<GUI::Button>("Check off List");
    set_done->set_tooltip("Mark currently selected tats as DONE");
    set_done->set_icon(GUI::Icon::default_icon("checkmark").bitmap_for_size(15));

    delete_task = button_group.add<GUI::Button>("Remove");
    delete_task->set_tooltip("Delete the currenlty selected task");
    delete_task->set_icon(GUI::Icon::default_icon("delete").bitmap_for_size(15));

    list_view = todo_frame.add<GUI::ListView>();
    list_view->set_should_hide_unnecessary_scrollbars(true);
    list_view->set_alternating_row_colors(true);
    list_view->set_selection_mode(GUI::AbstractView::SelectionMode::SingleSelection);
    list_view->on_selection_change = [this]() {
        auto& selection = list_view->selection();
        if (!selection.is_empty())
            currently_selected = selection.first().row();
        else
            currently_selected = -1;
    };

#if 0
    add_task = todo_frame.add<GUI::Button>("Add ...");
    add_task->set_tooltip("Add a new task to the todo list");
    add_task->set_icon(GUI::Icon::default_icon("plus").bitmap_for_size(15));

    add_task->on_click = [this](auto) {
        String task_title;
        if (GUI::InputBox::show(this, task_title, "Title:", "Add new task") != GUI::InputBox::ExecCancel) {
            task_list_model->add_task(Task(task_title));
            list_view->update();
            update();
        }
    };
#endif

    auto& input_frame = todo_frame.add<GUI::Widget>();
    input_frame.set_fixed_height(24);
    input_frame.set_layout<GUI::HorizontalBoxLayout>();
    new_task_box = input_frame.add<GUI::TextBox>();
    auto& new_task_button = input_frame.add<GUI::Button>();
    new_task_button.set_fixed_width(24);
    new_task_button.set_icon(GUI::Icon::default_icon("plus").bitmap_for_size(16));

    delete_task->on_click = [this](auto) {
        if (currently_selected != -1) {
            task_list_model->remove_task_from_index(currently_selected);
            list_view->update();
        }
    };

    set_done->on_click = [this](auto) {
        if (currently_selected != -1) {
            task_list_model->get_from_index(currently_selected).set_state(Task::State::done);
            list_view->update();
        }
    };

    new_task_button.on_click = [this](auto) {
        perform_add_new_task();
    };

    new_task_box->on_return_pressed = [this] {
        perform_add_new_task();
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

void TodoList::perform_add_new_task()
{
    String task_title = new_task_box->document().text();
    if (task_title.is_empty())
        return;

    new_task_box->set_document(GUI::TextDocument::create());
    task_list_model->add_task(Task(task_title));
    list_view->update();
    update();
}
