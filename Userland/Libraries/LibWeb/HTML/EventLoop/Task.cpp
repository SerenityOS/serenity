/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IDAllocator.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(Task);

static IDAllocator s_unique_task_source_allocator { static_cast<int>(Task::Source::UniqueTaskSourceStart) };
static IDAllocator s_task_id_allocator;

JS::NonnullGCPtr<Task> Task::create(JS::VM& vm, Source source, JS::GCPtr<DOM::Document const> document, JS::NonnullGCPtr<JS::HeapFunction<void()>> steps)
{
    return vm.heap().allocate_without_realm<Task>(source, document, move(steps));
}

Task::Task(Source source, JS::GCPtr<DOM::Document const> document, JS::NonnullGCPtr<JS::HeapFunction<void()>> steps)
    : m_id(s_task_id_allocator.allocate())
    , m_source(source)
    , m_steps(steps)
    , m_document(document)
{
}

Task::~Task() = default;

void Task::finalize()
{
    s_unique_task_source_allocator.deallocate(m_id);
}

void Task::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_steps);
    visitor.visit(m_document);
}

void Task::execute()
{
    m_steps->function()();
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
