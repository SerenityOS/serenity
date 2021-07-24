/*
 * Copyright (c) 2021, Arthur Brainville <ybalrid@ybalrid.info>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <LibGUI/Variant.h>

#include "Task.h"

#include <cstdio>

class TaskListModel final : public GUI::Model {
    AK::Vector<Task> task_list;

public:
    static AK::NonnullRefPtr<TaskListModel> create() { return adopt_ref(*new TaskListModel); }

    virtual ~TaskListModel() override { }

    virtual void update() override { }
    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return task_list.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const
    {
        if (role == GUI::ModelRole::Display)
            return from_index(index.row()).to_string();
        return {};
    }

    inline void add_task(const Task& new_task) { task_list.append(new_task); }
    inline void set_done(int i) { task_list[i].set_state(Task::State::done); }
    inline Task& get_from_index(int i) { return task_list[i]; }
    inline const Task& from_index(int i) const { return task_list[i]; }
    inline void remove_task_from_index(int i) { task_list.remove(i); }

    String serialize_to_json() const;

    void load_from_disk(StringView path);
    void save_to_disk(StringView path) const;
};
