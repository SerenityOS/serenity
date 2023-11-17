/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/StructuredSerialize.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/NavigationTiming/EntryNames.h>
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

void Performance::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::PerformancePrototype>(realm, "Performance"));
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
        m_timing = heap().allocate<NavigationTiming::PerformanceTiming>(realm(), *m_window);
    return m_timing;
}

double Performance::time_origin() const
{
    return static_cast<double>(m_timer.origin_time().milliseconds());
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
    window_or_worker->queue_performance_entry(entry);

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

WebIDL::ExceptionOr<HighResolutionTime::DOMHighResTimeStamp> Performance::convert_name_to_timestamp(JS::Realm& realm, String const& name)
{
    auto& vm = realm.vm();

    // 1. If the global object is not a Window object, throw a TypeError.
    if (!is<HTML::Window>(realm.global_object()))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, TRY_OR_THROW_OOM(vm, String::formatted("'{}' is an attribute in the PerformanceTiming interface and thus can only be used in a Window context", name)) };

    // 2. If name is navigationStart, return 0.
    if (name == NavigationTiming::EntryNames::navigationStart)
        return 0.0;

    auto timing_interface = timing();
    VERIFY(timing_interface);

    // 3. Let startTime be the value of navigationStart in the PerformanceTiming interface.
    auto start_time = timing_interface->navigation_start();

    // 4. Let endTime be the value of name in the PerformanceTiming interface.
    u64 end_time { 0 };

#define __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(camel_case_name, snake_case_name) \
    if (name == NavigationTiming::EntryNames::camel_case_name)                     \
        end_time = timing_interface->snake_case_name();
    ENUMERATE_NAVIGATION_TIMING_ENTRY_NAMES
#undef __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME

    // 5. If endTime is 0, throw an InvalidAccessError.
    if (end_time == 0)
        return WebIDL::InvalidAccessError::create(realm, MUST(String::formatted("The '{}' entry in the PerformanceTiming interface is equal to 0, meaning it hasn't happened yet", name)));

    // 6. Return result of subtracting startTime from endTime.
    return static_cast<HighResolutionTime::DOMHighResTimeStamp>(end_time - start_time);
}

// https://w3c.github.io/user-timing/#dfn-convert-a-mark-to-a-timestamp
WebIDL::ExceptionOr<HighResolutionTime::DOMHighResTimeStamp> Performance::convert_mark_to_timestamp(JS::Realm& realm, Variant<String, HighResolutionTime::DOMHighResTimeStamp> mark)
{
    if (mark.has<String>()) {
        auto const& mark_string = mark.get<String>();

        // 1. If mark is a DOMString and it has the same name as a read only attribute in the PerformanceTiming interface, let end
        //    time be the value returned by running the convert a name to a timestamp algorithm with name set to the value of mark.
#define __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME(name, _)  \
    if (mark_string == NavigationTiming::EntryNames::name) \
        return convert_name_to_timestamp(realm, mark_string);
        ENUMERATE_NAVIGATION_TIMING_ENTRY_NAMES
#undef __ENUMERATE_NAVIGATION_TIMING_ENTRY_NAME

        // 2. Otherwise, if mark is a DOMString, let end time be the value of the startTime attribute from the most recent occurrence
        //    of a PerformanceMark object in the performance entry buffer whose name is mark. If no matching entry is found, throw a
        //    SyntaxError.
        auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
        VERIFY(window_or_worker);

        auto& tuple = window_or_worker->relevant_performance_entry_tuple(PerformanceTimeline::EntryTypes::mark);
        auto& performance_entry_buffer = tuple.performance_entry_buffer;

        auto maybe_entry = performance_entry_buffer.last_matching([&mark_string](JS::Handle<PerformanceTimeline::PerformanceEntry> const& entry) {
            return entry->name() == mark_string;
        });

        if (!maybe_entry.has_value())
            return WebIDL::SyntaxError::create(realm, MUST(String::formatted("No PerformanceMark object with name '{}' found in the performance timeline", mark_string)));

        return maybe_entry.value()->start_time();
    }

    // 3. Otherwise, if mark is a DOMHighResTimeStamp:
    auto mark_time_stamp = mark.get<HighResolutionTime::DOMHighResTimeStamp>();

    // 1. If mark is negative, throw a TypeError.
    if (mark_time_stamp < 0.0)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot have negative time values in PerformanceMark"sv };

    // 2. Otherwise, let end time be mark.
    return mark_time_stamp;
}

// https://w3c.github.io/user-timing/#dom-performance-measure
WebIDL::ExceptionOr<JS::NonnullGCPtr<UserTiming::PerformanceMeasure>> Performance::measure(String const& measure_name, Variant<String, UserTiming::PerformanceMeasureOptions> const& start_or_measure_options, Optional<String> end_mark)
{
    auto& realm = this->realm();
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);
    auto& vm = this->vm();

    // 1. If startOrMeasureOptions is a PerformanceMeasureOptions object and at least one of start, end, duration, and detail
    //    are present, run the following checks:
    auto const* start_or_measure_options_dictionary_object = start_or_measure_options.get_pointer<UserTiming::PerformanceMeasureOptions>();
    if (start_or_measure_options_dictionary_object
        && (start_or_measure_options_dictionary_object->start.has_value()
            || start_or_measure_options_dictionary_object->end.has_value()
            || start_or_measure_options_dictionary_object->duration.has_value()
            || !start_or_measure_options_dictionary_object->detail.is_undefined())) {
        // 1. If endMark is given, throw a TypeError.
        if (end_mark.has_value())
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot provide PerformanceMeasureOptions and endMark at the same time"sv };

        // 2. If startOrMeasureOptions's start and end members are both omitted, throw a TypeError.
        if (!start_or_measure_options_dictionary_object->start.has_value() && !start_or_measure_options_dictionary_object->end.has_value())
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "PerformanceMeasureOptions must contain one or both of 'start' and 'end'"sv };

        // 3. If startOrMeasureOptions's start, duration, and end members are all present, throw a TypeError.
        if (start_or_measure_options_dictionary_object->start.has_value() && start_or_measure_options_dictionary_object->end.has_value() && start_or_measure_options_dictionary_object->duration.has_value())
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "PerformanceMeasureOptions cannot contain 'start', 'duration' and 'end' properties all at once"sv };
    }

    // 2. Compute end time as follows:
    HighResolutionTime::DOMHighResTimeStamp end_time { 0.0 };

    // 1. If endMark is given, let end time be the value returned by running the convert a mark to a timestamp algorithm passing
    //    in endMark.
    if (end_mark.has_value()) {
        end_time = TRY(convert_mark_to_timestamp(realm, end_mark.value()));
    }
    // 2. Otherwise, if startOrMeasureOptions is a PerformanceMeasureOptions object, and if its end member is present, let end
    //    time be the value returned by running the convert a mark to a timestamp algorithm passing in startOrMeasureOptions's end.
    else if (start_or_measure_options_dictionary_object && start_or_measure_options_dictionary_object->end.has_value()) {
        end_time = TRY(convert_mark_to_timestamp(realm, start_or_measure_options_dictionary_object->end.value()));
    }
    // 3. Otherwise, if startOrMeasureOptions is a PerformanceMeasureOptions object, and if its start and duration members are
    //    both present:
    else if (start_or_measure_options_dictionary_object && start_or_measure_options_dictionary_object->start.has_value() && start_or_measure_options_dictionary_object->duration.has_value()) {
        // 1. Let start be the value returned by running the convert a mark to a timestamp algorithm passing in start.
        auto start = TRY(convert_mark_to_timestamp(realm, start_or_measure_options_dictionary_object->start.value()));

        // 2. Let duration be the value returned by running the convert a mark to a timestamp algorithm passing in duration.
        auto duration = TRY(convert_mark_to_timestamp(realm, start_or_measure_options_dictionary_object->duration.value()));

        // 3. Let end time be start plus duration.
        end_time = start + duration;
    }
    // 4. Otherwise, let end time be the value that would be returned by the Performance object's now() method.
    else {
        // FIXME: Performance#now doesn't currently use TimeOrigin's functions, update this and Performance#now to match Performance#now's specification.
        end_time = HighResolutionTime::unsafe_shared_current_time();
    }

    // 3. Compute start time as follows:
    HighResolutionTime::DOMHighResTimeStamp start_time { 0.0 };

    // 1. If startOrMeasureOptions is a PerformanceMeasureOptions object, and if its start member is present, let start time be
    //    the value returned by running the convert a mark to a timestamp algorithm passing in startOrMeasureOptions's start.
    if (start_or_measure_options_dictionary_object && start_or_measure_options_dictionary_object->start.has_value()) {
        start_time = TRY(convert_mark_to_timestamp(realm, start_or_measure_options_dictionary_object->start.value()));
    }
    // 2. Otherwise, if startOrMeasureOptions is a PerformanceMeasureOptions object, and if its duration and end members are
    //    both present:
    else if (start_or_measure_options_dictionary_object && start_or_measure_options_dictionary_object->duration.has_value() && start_or_measure_options_dictionary_object->end.has_value()) {
        // 1. Let duration be the value returned by running the convert a mark to a timestamp algorithm passing in duration.
        auto duration = TRY(convert_mark_to_timestamp(realm, start_or_measure_options_dictionary_object->duration.value()));

        // 2. Let end be the value returned by running the convert a mark to a timestamp algorithm passing in end.
        auto end = TRY(convert_mark_to_timestamp(realm, start_or_measure_options_dictionary_object->end.value()));

        // 3. Let start time be end minus duration.
        start_time = end - duration;
    }
    // 3. Otherwise, if startOrMeasureOptions is a DOMString, let start time be the value returned by running the convert a mark
    //    to a timestamp algorithm passing in startOrMeasureOptions.
    else if (start_or_measure_options.has<String>()) {
        start_time = TRY(convert_mark_to_timestamp(realm, start_or_measure_options.get<String>()));
    }
    // 4. Otherwise, let start time be 0.
    else {
        start_time = 0.0;
    }

    // NOTE: Step 4 (creating the entry) is done after determining values, as we set the values once during creation and never
    //       change them after.

    // 5. Set entry's name attribute to measureName.
    // NOTE: Will be done during construction.

    // 6. Set entry's entryType attribute to DOMString "measure".
    // NOTE: Already done via the `entry_type` virtual function.

    // 7. Set entry's startTime attribute to start time.
    // NOTE: Will be done during construction.

    // 8. Set entry's duration attribute to the duration from start time to end time. The resulting duration value MAY be negative.
    auto duration = end_time - start_time;

    // 9. Set entry's detail attribute as follows:
    JS::Value detail { JS::js_null() };

    // 1. If startOrMeasureOptions is a PerformanceMeasureOptions object and startOrMeasureOptions's detail member is present:
    if (start_or_measure_options_dictionary_object && !start_or_measure_options_dictionary_object->detail.is_undefined()) {
        // 1. Let record be the result of calling the StructuredSerialize algorithm on startOrMeasureOptions's detail.
        auto record = TRY(HTML::structured_serialize(vm, start_or_measure_options_dictionary_object->detail));

        // 2. Set entry's detail to the result of calling the StructuredDeserialize algorithm on record and the current realm.
        detail = TRY(HTML::structured_deserialize(vm, record, realm, Optional<HTML::DeserializationMemory> {}));
    }

    // 2. Otherwise, set it to null.
    // NOTE: Already the default value of `detail`.

    // 4. Create a new PerformanceMeasure object (entry) with this's relevant realm.
    auto entry = realm.heap().allocate<UserTiming::PerformanceMeasure>(realm, realm, measure_name, start_time, duration, detail);

    // 10. Queue entry.
    window_or_worker->queue_performance_entry(entry);

    // 11. Add entry to the performance entry buffer.
    // FIXME: This seems to be a holdover from moving to the `queue` structure for PerformanceObserver, as this would cause a double append.

    // 12. Return entry.
    return entry;
}

// https://w3c.github.io/user-timing/#dom-performance-clearmeasures
void Performance::clear_measures(Optional<String> measure_name)
{
    auto& realm = this->realm();
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&realm.global_object());
    VERIFY(window_or_worker);

    // 1. If measureName is omitted, remove all PerformanceMeasure objects in the performance entry buffer.
    if (!measure_name.has_value()) {
        window_or_worker->clear_performance_entry_buffer({}, PerformanceTimeline::EntryTypes::measure);
        return;
    }

    // 2. Otherwise remove all PerformanceMeasure objects listed in the performance entry buffer whose name is measureName.
    window_or_worker->remove_entries_from_performance_entry_buffer({}, PerformanceTimeline::EntryTypes::measure, measure_name.value());

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
