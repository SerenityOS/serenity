/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Fetch/FetchMethod.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/StructuredSerialize.h>
#include <LibWeb/HTML/Timer.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowOrWorkerGlobalScope.h>
#include <LibWeb/Infra/Base64.h>
#include <LibWeb/PerformanceTimeline/EntryTypes.h>
#include <LibWeb/UserTiming/PerformanceMark.h>
#include <LibWeb/UserTiming/PerformanceMeasure.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

WindowOrWorkerGlobalScopeMixin::~WindowOrWorkerGlobalScopeMixin() = default;

// Please keep these in alphabetical order based on the entry type :^)
#define ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES                                                                   \
    __ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES(PerformanceTimeline::EntryTypes::mark, UserTiming::PerformanceMark) \
    __ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES(PerformanceTimeline::EntryTypes::measure, UserTiming::PerformanceMeasure)

void WindowOrWorkerGlobalScopeMixin::initialize(JS::Realm&)
{
#define __ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES(entry_type, cpp_class) \
    m_performance_entry_buffer_map.set(entry_type,                           \
        PerformanceTimeline::PerformanceEntryTuple {                         \
            .performance_entry_buffer = {},                                  \
            .max_buffer_size = cpp_class::max_buffer_size(),                 \
            .available_from_timeline = cpp_class::available_from_timeline(), \
            .dropped_entries_count = 0,                                      \
        });
    ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES
#undef __ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES
}

void WindowOrWorkerGlobalScopeMixin::visit_edges(JS::Cell::Visitor& visitor)
{
    for (auto& it : m_timers)
        visitor.visit(it.value);
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-origin
WebIDL::ExceptionOr<String> WindowOrWorkerGlobalScopeMixin::origin() const
{
    auto& vm = this_impl().vm();

    // The origin getter steps are to return this's relevant settings object's origin, serialized.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(relevant_settings_object(this_impl()).origin().serialize()));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-issecurecontext
bool WindowOrWorkerGlobalScopeMixin::is_secure_context() const
{
    // The isSecureContext getter steps are to return true if this's relevant settings object is a secure context, or false otherwise.
    return HTML::is_secure_context(relevant_settings_object(this_impl()));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-crossoriginisolated
bool WindowOrWorkerGlobalScopeMixin::cross_origin_isolated() const
{
    // The crossOriginIsolated getter steps are to return this's relevant settings object's cross-origin isolated capability.
    return relevant_settings_object(this_impl()).cross_origin_isolated_capability() == CanUseCrossOriginIsolatedAPIs::Yes;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-btoa
WebIDL::ExceptionOr<String> WindowOrWorkerGlobalScopeMixin::btoa(String const& data) const
{
    auto& vm = this_impl().vm();
    auto& realm = *vm.current_realm();

    // The btoa(data) method must throw an "InvalidCharacterError" DOMException if data contains any character whose code point is greater than U+00FF.
    Vector<u8> byte_string;
    byte_string.ensure_capacity(data.bytes().size());
    for (u32 code_point : Utf8View(data)) {
        if (code_point > 0xff)
            return WebIDL::InvalidCharacterError::create(realm, "Data contains characters outside the range U+0000 and U+00FF");
        byte_string.append(code_point);
    }

    // Otherwise, the user agent must convert data to a byte sequence whose nth byte is the eight-bit representation of the nth code point of data,
    // and then must apply forgiving-base64 encode to that byte sequence and return the result.
    return TRY_OR_THROW_OOM(vm, encode_base64(byte_string.span()));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-atob
WebIDL::ExceptionOr<String> WindowOrWorkerGlobalScopeMixin::atob(String const& data) const
{
    auto& vm = this_impl().vm();
    auto& realm = *vm.current_realm();

    // 1. Let decodedData be the result of running forgiving-base64 decode on data.
    auto decoded_data = Infra::decode_forgiving_base64(data.bytes_as_string_view());

    // 2. If decodedData is failure, then throw an "InvalidCharacterError" DOMException.
    if (decoded_data.is_error())
        return WebIDL::InvalidCharacterError::create(realm, "Input string is not valid base64 data");

    // 3. Return decodedData.
    // decode_base64() returns a byte string. LibJS uses UTF-8 for strings. Use Latin1Decoder to convert bytes 128-255 to UTF-8.
    auto decoder = TextCodec::decoder_for("windows-1252"sv);
    VERIFY(decoder.has_value());
    return TRY_OR_THROW_OOM(vm, decoder->to_utf8(decoded_data.value()));
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-queuemicrotask
void WindowOrWorkerGlobalScopeMixin::queue_microtask(WebIDL::CallbackType& callback)
{
    auto& vm = this_impl().vm();
    auto& realm = *vm.current_realm();

    JS::GCPtr<DOM::Document> document;
    if (is<Window>(this_impl()))
        document = &static_cast<Window&>(this_impl()).associated_document();

    // The queueMicrotask(callback) method must queue a microtask to invoke callback, and if callback throws an exception, report the exception.
    HTML::queue_a_microtask(document, [&callback, &realm] {
        auto result = WebIDL::invoke_callback(callback, {});
        if (result.is_error())
            HTML::report_exception(result, realm);
    });
}

// https://html.spec.whatwg.org/multipage/structured-data.html#dom-structuredclone
WebIDL::ExceptionOr<JS::Value> WindowOrWorkerGlobalScopeMixin::structured_clone(JS::Value value, StructuredSerializeOptions const& options) const
{
    auto& vm = this_impl().vm();
    (void)options;

    // 1. Let serialized be ? StructuredSerializeWithTransfer(value, options["transfer"]).
    // FIXME: Use WithTransfer variant of the AO
    auto serialized = TRY(structured_serialize(vm, value));

    // 2. Let deserializeRecord be ? StructuredDeserializeWithTransfer(serialized, this's relevant realm).
    // FIXME: Use WithTransfer variant of the AO
    auto deserialized = TRY(structured_deserialize(vm, serialized, relevant_realm(this_impl()), {}));

    // 3. Return deserializeRecord.[[Deserialized]].
    return deserialized;
}

JS::NonnullGCPtr<JS::Promise> WindowOrWorkerGlobalScopeMixin::fetch(Fetch::RequestInfo const& input, Fetch::RequestInit const& init) const
{
    auto& vm = this_impl().vm();
    return Fetch::fetch(vm, input, init);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-settimeout
i32 WindowOrWorkerGlobalScopeMixin::set_timeout(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments)
{
    return run_timer_initialization_steps(move(handler), timeout, move(arguments), Repeat::No);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-setinterval
i32 WindowOrWorkerGlobalScopeMixin::set_interval(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments)
{
    return run_timer_initialization_steps(move(handler), timeout, move(arguments), Repeat::Yes);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-cleartimeout
void WindowOrWorkerGlobalScopeMixin::clear_timeout(i32 id)
{
    m_timers.remove(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-clearinterval
void WindowOrWorkerGlobalScopeMixin::clear_interval(i32 id)
{
    m_timers.remove(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#timer-initialisation-steps
i32 WindowOrWorkerGlobalScopeMixin::run_timer_initialization_steps(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments, Repeat repeat, Optional<i32> previous_id, Optional<AK::URL> base_url)
{
    // 1. Let thisArg be global if that is a WorkerGlobalScope object; otherwise let thisArg be the WindowProxy that corresponds to global.

    // 2. If previousId was given, let id be previousId; otherwise, let id be an implementation-defined integer that is greater than zero and does not already exist in global's map of active timers.
    auto id = previous_id.has_value() ? previous_id.value() : m_timer_id_allocator.allocate();

    // FIXME: 3. If the surrounding agent's event loop's currently running task is a task that was created by this algorithm, then let nesting level be the task's timer nesting level. Otherwise, let nesting level be zero.

    // 4. If timeout is less than 0, then set timeout to 0.
    if (timeout < 0)
        timeout = 0;

    // FIXME: 5. If nesting level is greater than 5, and timeout is less than 4, then set timeout to 4.

    // 6. Let callerRealm be the current Realm Record, and calleeRealm be global's relevant Realm.
    // FIXME: Implement this when step 9.3.2 is implemented.

    // FIXME: The active script becomes null on repeated setInterval callbacks. In JS::VM::get_active_script_or_module,
    //        the execution context stack is empty on the repeated invocations, thus it returns null. We will need
    //        to figure out why it becomes empty. But all we need from the active script is the base URL, so we
    //        grab it on the first invocation an reuse it on repeated invocations.
    if (!base_url.has_value()) {
        // 7. Let initiating script be the active script.
        auto const* initiating_script = Web::Bindings::active_script();

        // 8. Assert: initiating script is not null, since this algorithm is always called from some script.
        VERIFY(initiating_script);

        base_url = initiating_script->base_url();
    }

    // 9. Let task be a task that runs the following substeps:
    JS::SafeFunction<void()> task = [this, handler = move(handler), timeout, arguments = move(arguments), repeat, id, base_url = move(base_url)]() mutable {
        // 1. If id does not exist in global's map of active timers, then abort these steps.
        if (!m_timers.contains(id))
            return;

        handler.visit(
            // 2. If handler is a Function, then invoke handler given arguments with the callback this value set to thisArg. If this throws an exception, catch it, and report the exception.
            [&](JS::Handle<WebIDL::CallbackType> const& callback) {
                if (auto result = WebIDL::invoke_callback(*callback, &this_impl(), arguments); result.is_error())
                    report_exception(result, this_impl().realm());
            },
            // 3. Otherwise:
            [&](String const& source) {
                // 1. Assert: handler is a string.
                // FIXME: 2. Perform HostEnsureCanCompileStrings(callerRealm, calleeRealm). If this throws an exception, catch it, report the exception, and abort these steps.

                // 3. Let settings object be global's relevant settings object.
                auto& settings_object = relevant_settings_object(this_impl());

                // 4. Let base URL be initiating script's base URL.
                // 5. Assert: base URL is not null, as initiating script is a classic script or a JavaScript module script.
                VERIFY(base_url.has_value());

                // 6. Let fetch options be a script fetch options whose cryptographic nonce is initiating script's fetch options's cryptographic nonce, integrity metadata is the empty string, parser metadata is "not-parser-inserted", credentials mode is initiating script's fetch options's credentials mode, and referrer policy is initiating script's fetch options's referrer policy.
                // 7. Let script be the result of creating a classic script given handler, settings object, base URL, and fetch options.
                auto script = ClassicScript::create(base_url->basename(), source, settings_object, *base_url);

                // 8. Run the classic script script.
                (void)script->run();
            });

        // 4. If id does not exist in global's map of active timers, then abort these steps.
        if (!m_timers.contains(id))
            return;

        switch (repeat) {
        // 5. If repeat is true, then perform the timer initialization steps again, given global, handler, timeout, arguments, true, and id.
        case Repeat::Yes:
            run_timer_initialization_steps(handler, timeout, move(arguments), repeat, id, move(base_url));
            break;

        // 6. Otherwise, remove global's map of active timers[id].
        case Repeat::No:
            m_timers.remove(id);
            break;
        }
    };

    // FIXME: 10. Increment nesting level by one.
    // FIXME: 11. Set task's timer nesting level to nesting level.

    // 12. Let completionStep be an algorithm step which queues a global task on the timer task source given global to run task.
    JS::SafeFunction<void()> completion_step = [this, task = move(task)]() mutable {
        queue_global_task(Task::Source::TimerTask, this_impl(), move(task));
    };

    // 13. Run steps after a timeout given global, "setTimeout/setInterval", timeout, completionStep, and id.
    auto timer = Timer::create(this_impl(), timeout, move(completion_step), id);
    m_timers.set(id, timer);
    timer->start();

    // 14. Return id.
    return id;
}

// 1. https://www.w3.org/TR/performance-timeline/#dfn-relevant-performance-entry-tuple
PerformanceTimeline::PerformanceEntryTuple& WindowOrWorkerGlobalScopeMixin::relevant_performance_entry_tuple(FlyString const& entry_type)
{
    // 1. Let map be the performance entry buffer map associated with globalObject.
    // 2. Return the result of getting the value of an entry from map, given entryType as the key.
    auto tuple = m_performance_entry_buffer_map.get(entry_type);

    // This shouldn't be called with entry types that aren't in `ENUMERATE_SUPPORTED_PERFORMANCE_ENTRY_TYPES`.
    VERIFY(tuple.has_value());
    return tuple.value();
}

// https://www.w3.org/TR/performance-timeline/#dfn-queue-a-performanceentry
WebIDL::ExceptionOr<void> WindowOrWorkerGlobalScopeMixin::queue_performance_entry(JS::NonnullGCPtr<PerformanceTimeline::PerformanceEntry> new_entry)
{
    auto& vm = new_entry->vm();

    // FIXME: 1. Let interested observers be an initially empty set of PerformanceObserver objects.

    // 2. Let entryType be newEntry’s entryType value.
    auto const& entry_type = new_entry->entry_type();

    // 3. Let relevantGlobal be newEntry's relevant global object.
    // NOTE: Already is `this`.

    // FIXME: 4. For each registered performance observer regObs in relevantGlobal's list of registered performance observer
    //           objects:
    //           1. If regObs's options list contains a PerformanceObserverInit options whose entryTypes member includes entryType
    //              or whose type member equals to entryType:
    //              1. If should add entry with newEntry and options returns true, append regObs's observer to interested observers.

    // FIXME: 5. For each observer in interested observers:
    //           1. Append newEntry to observer's observer buffer.

    // 6. Let tuple be the relevant performance entry tuple of entryType and relevantGlobal.
    auto& tuple = relevant_performance_entry_tuple(entry_type);

    // 7. Let isBufferFull be the return value of the determine if a performance entry buffer is full algorithm with tuple
    //    as input.
    bool is_buffer_full = tuple.is_full();

    // 8. Let shouldAdd be the result of should add entry with newEntry as input.
    auto should_add = new_entry->should_add_entry();

    // 9. If isBufferFull is false and shouldAdd is true, append newEntry to tuple's performance entry buffer.
    if (!is_buffer_full && should_add == PerformanceTimeline::ShouldAddEntry::Yes)
        TRY_OR_THROW_OOM(vm, tuple.performance_entry_buffer.try_append(JS::make_handle(new_entry)));

    // FIXME: 10. Queue the PerformanceObserver task with relevantGlobal as input.
    return {};
}

void WindowOrWorkerGlobalScopeMixin::clear_performance_entry_buffer(Badge<HighResolutionTime::Performance>, FlyString const& entry_type)
{
    auto& tuple = relevant_performance_entry_tuple(entry_type);
    tuple.performance_entry_buffer.clear();
}

void WindowOrWorkerGlobalScopeMixin::remove_entries_from_performance_entry_buffer(Badge<HighResolutionTime::Performance>, FlyString const& entry_type, String entry_name)
{
    auto& tuple = relevant_performance_entry_tuple(entry_type);
    tuple.performance_entry_buffer.remove_all_matching([&entry_name](JS::Handle<PerformanceTimeline::PerformanceEntry> const& entry) {
        return entry->name() == entry_name;
    });
}

// https://www.w3.org/TR/performance-timeline/#dfn-filter-buffer-by-name-and-type
static ErrorOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> filter_buffer_by_name_and_type(Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>> const& buffer, Optional<String> name, Optional<String> type)
{
    // 1. Let result be an initially empty list.
    Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>> result;

    // 2. For each PerformanceEntry entry in buffer, run the following steps:
    for (auto const& entry : buffer) {
        // 1. If type is not null and if type is not identical to entry's entryType attribute, continue to next entry.
        if (type.has_value() && type.value() != entry->entry_type())
            continue;

        // 2. If name is not null and if name is not identical to entry's name attribute, continue to next entry.
        if (name.has_value() && name.value() != entry->name())
            continue;

        // 3. append entry to result.
        TRY(result.try_append(entry));
    }

    // 3. Sort results's entries in chronological order with respect to startTime
    quick_sort(result, [](auto const& left_entry, auto const& right_entry) {
        return left_entry->start_time() < right_entry->start_time();
    });

    // 4. Return result.
    return result;
}

// https://www.w3.org/TR/performance-timeline/#dfn-filter-buffer-map-by-name-and-type
ErrorOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> WindowOrWorkerGlobalScopeMixin::filter_buffer_map_by_name_and_type(Optional<String> name, Optional<String> type) const
{
    // 1. Let result be an initially empty list.
    Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>> result;

    // 2. Let map be the performance entry buffer map associated with the relevant global object of this.
    auto const& map = m_performance_entry_buffer_map;

    // 3. Let tuple list be an empty list.
    Vector<PerformanceTimeline::PerformanceEntryTuple const&> tuple_list;

    // 4. If type is not null, append the result of getting the value of entry on map given type as key to tuple list.
    //    Otherwise, assign the result of get the values on map to tuple list.
    if (type.has_value()) {
        auto maybe_tuple = map.get(type.value());
        if (maybe_tuple.has_value())
            TRY(tuple_list.try_append(maybe_tuple.release_value()));
    } else {
        for (auto const& it : map)
            TRY(tuple_list.try_append(it.value));
    }

    // 5. For each tuple in tuple list, run the following steps:
    for (auto const& tuple : tuple_list) {
        // 1. Let buffer be tuple's performance entry buffer.
        auto const& buffer = tuple.performance_entry_buffer;

        // 2. If tuple's availableFromTimeline is false, continue to the next tuple.
        if (tuple.available_from_timeline == PerformanceTimeline::AvailableFromTimeline::No)
            continue;

        // 3. Let entries be the result of running filter buffer by name and type with buffer, name and type as inputs.
        auto entries = TRY(filter_buffer_by_name_and_type(buffer, name, type));

        // 4. For each entry in entries, append entry to result.
        TRY(result.try_extend(entries));
    }

    // 6. Sort results's entries in chronological order with respect to startTime
    quick_sort(result, [](auto const& left_entry, auto const& right_entry) {
        return left_entry->start_time() < right_entry->start_time();
    });

    // 7. Return result.
    return result;
}

}
