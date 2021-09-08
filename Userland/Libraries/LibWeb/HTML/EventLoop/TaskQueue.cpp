/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventLoop/TaskQueue.h>

namespace Web::HTML {

TaskQueue::TaskQueue()
{
}

TaskQueue::~TaskQueue()
{
}

void TaskQueue::add(NonnullOwnPtr<Task> task)
{
    m_tasks.enqueue(move(task));
}

}
