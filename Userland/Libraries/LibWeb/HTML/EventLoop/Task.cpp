/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IDAllocator.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

static IDAllocator s_unique_task_source_allocator { static_cast<int>(Task::Source::UniqueTaskSourceStart) };
static IDAllocator s_task_id_allocator;

Task::Task(Source source, DOM::Document const* document, JS::SafeFunction<void()> steps)
    : m_id(s_task_id_allocator.allocate())
    , m_source(source)
    , m_steps(move(steps))
    , m_document(JS::make_handle(document))
{
}

Task::~Task()
{
    s_unique_task_source_allocator.deallocate(m_id);
}

void Task::execute()
{
    m_steps();
}

// https://html.spec.whatwg.org/#concept-task-runnable
bool Task::is_runnable() const
{
    // A task is runnable if its document is either null or fully active.
    return !m_document.ptr() || m_document->is_fully_active();
}

DOM::Document const* Task::document() const
{
    return m_document.ptr();
}

UniqueTaskSource::UniqueTaskSource()
    : source(static_cast<Task::Source>(s_unique_task_source_allocator.allocate()))
{
}

UniqueTaskSource::~UniqueTaskSource()
{
    s_unique_task_source_allocator.deallocate(static_cast<int>(source));
}

}
