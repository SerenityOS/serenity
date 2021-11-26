/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

Task::Task(Source source, DOM::Document* document, Function<void()> steps)
    : m_source(source)
    , m_steps(move(steps))
    , m_document(document)
{
}

Task::~Task()
{
}

void Task::execute()
{
    m_steps();
}

// https://html.spec.whatwg.org/#concept-task-runnable
bool Task::is_runnable() const
{
    // A task is runnable if its document is either null or fully active.
    return !m_document || m_document->is_fully_active();
}

}
