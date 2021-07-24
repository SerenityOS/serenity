/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskListModel.h"

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/StringView.h>
#include <LibCore/File.h>

#include <cstdio>

void TaskListModel::load_from_disk(StringView path)
{
    dbgln("Openning {} for reading todo items...", path);
    if (!Core::File::exists(path))
        return;

    auto todofile = Core::File::construct(path);

    if (!todofile->open(Core::OpenMode::ReadOnly)) {
        dbgln("Couldn't open todolist file {}", todofile->error_string());
        return;
    }

    auto todoJson = JsonValue::from_string(todofile->read_all());

    if (!todoJson->is_array()) {
        dbgln("Error, json task list is not in the valid format");
        return;
    }
    auto list = todoJson->as_array();

    for (size_t i = 0; i < list.size(); ++i) {
        auto const& task = list[i].as_object();

        String const title = task.get("title").to_string();
        String const description = task.get("description").to_string();
        Task::State const state = task.get("state").to_string() == "DONE" ? Task::State::done : Task::State::todo;

        Task new_task_object(title, description);
        new_task_object.set_state(state);
        add_task(new_task_object);
    }
}

String TaskListModel::serialize_to_json() const
{
    AK::JsonArray json_task_array;

    for (size_t i = 0; i < task_list.size(); ++i) {
        AK::JsonObject task_object;

        task_object.set("title", task_list[i].title());
        task_object.set("description", task_list[i].description());
        task_object.set("state", Task::state_to_string(task_list[i].state()));

        json_task_array.append(task_object);
    }

    return json_task_array.to_string();
}

void TaskListModel::save_to_disk(StringView path) const
{
    dbgln("Openning {} for writing todo items...", path);
    dbgln("Saving task list size : {}", task_list.size());

    auto output = serialize_to_json();
    if (output.is_empty())
        return;

    auto todofile = Core::File::construct(path);
    if (todofile->open(Core::OpenMode::WriteOnly))
        todofile->write(output);
}
