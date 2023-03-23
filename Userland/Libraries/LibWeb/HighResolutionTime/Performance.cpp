/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/NavigationTiming/PerformanceTiming.h>
#include <LibWeb/PerformanceTimeline/EntryTypes.h>

namespace Web::HighResolutionTime {

Performance::Performance(HTML::Window& window)
    : DOM::EventTarget(window.realm())
    , m_window(window)
{
    m_timer.start();
}

Performance::~Performance() = default;

JS::ThrowCompletionOr<void> Performance::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::PerformancePrototype>(realm, "Performance"));

    return {};
}

void Performance::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
    visitor.visit(m_timing.ptr());
}

JS::GCPtr<NavigationTiming::PerformanceTiming> Performance::timing()
{
    if (!m_timing)
        m_timing = heap().allocate<NavigationTiming::PerformanceTiming>(realm(), *m_window).release_allocated_value_but_fixme_should_propagate_errors();
    return m_timing;
}

double Performance::time_origin() const
{
    return static_cast<double>(m_timer.origin_time().to_milliseconds());
}

// https://w3c.github.io/user-timing/#mark-method
WebIDL::ExceptionOr<JS::NonnullGCPtr<UserTiming::PerformanceMark>> Performance::mark(String const& mark_name, UserTiming::PerformanceMarkOptions const& mark_options)
{
    auto& realm = this->realm();

    // 1. Run the PerformanceMark constructor and let entry be the newly created object.
    auto entry = TRY(UserTiming::PerformanceMark::construct_impl(realm, mark_name, mark_options));

    // 2. Queue entry.
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);
    TRY(window_or_worker->queue_performance_entry(entry));

    // 3. Add entry to the performance entry buffer.
    // FIXME: This seems to be a holdover from moving to the `queue` structure for PerformanceObserver, as this would cause a double append.

    // 4. Return entry.
    return entry;
}

void Performance::clear_marks(Optional<String> mark_name)
{
    auto& realm = this->realm();
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);

    // 1. If markName is omitted, remove all PerformanceMark objects from the performance entry buffer.
    if (!mark_name.has_value()) {
        window_or_worker->clear_performance_entry_buffer({}, PerformanceTimeline::EntryTypes::mark);
        return;
    }

    // 2. Otherwise, remove all PerformanceMark objects listed in the performance entry buffer whose name is markName.
    window_or_worker->remove_entries_from_performance_entry_buffer({}, PerformanceTimeline::EntryTypes::mark, mark_name.value());

    // 3. Return undefined.
}

// https://www.w3.org/TR/performance-timeline/#getentries-method
WebIDL::ExceptionOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> Performance::get_entries() const
{
    auto& realm = this->realm();
    auto& vm = this->vm();
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);

    // Returns a PerformanceEntryList object returned by the filter buffer map by name and type algorithm with name and
    // type set to null.
    return TRY_OR_THROW_OOM(vm, window_or_worker->filter_buffer_map_by_name_and_type(/* name= */ Optional<String> {}, /* type= */ Optional<String> {}));
}

// https://www.w3.org/TR/performance-timeline/#dom-performance-getentriesbytype
WebIDL::ExceptionOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> Performance::get_entries_by_type(String const& type) const
{
    auto& realm = this->realm();
    auto& vm = this->vm();
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);

    // Returns a PerformanceEntryList object returned by filter buffer map by name and type algorithm with name set to null,
    // and type set to the method's input type parameter.
    return TRY_OR_THROW_OOM(vm, window_or_worker->filter_buffer_map_by_name_and_type(/* name= */ Optional<String> {}, type));
}

// https://www.w3.org/TR/performance-timeline/#dom-performance-getentriesbyname
WebIDL::ExceptionOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> Performance::get_entries_by_name(String const& name, Optional<String> type) const
{
    auto& realm = this->realm();
    auto& vm = this->vm();
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);

    // Returns a PerformanceEntryList object returned by filter buffer map by name and type algorithm with name set to the
    // method input name parameter, and type set to null if optional entryType is omitted, or set to the method's input type
    // parameter otherwise.
    return TRY_OR_THROW_OOM(vm, window_or_worker->filter_buffer_map_by_name_and_type(name, type));
}

}
