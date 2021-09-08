/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

Task::Task(DOM::Document* document, Function<void()> steps)
    : m_steps(move(steps))
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

}
