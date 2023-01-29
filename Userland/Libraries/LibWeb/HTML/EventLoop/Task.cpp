/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

Task::Task(Source source, DOM::Document* document, JS::SafeFunction<void()> steps)
    : m_source(source)
    , m_steps(move(steps))
    , m_document(JS::make_handle(document))
{
}

Task::~Task() = default;

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

DOM::Document* Task::document()
{
    return m_document.ptr();
}

DOM::Document const* Task::document() const
{
    return m_document.ptr();
}

}
