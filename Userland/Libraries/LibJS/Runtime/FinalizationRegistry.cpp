/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FinalizationRegistry.h>

namespace JS {

FinalizationRegistry::FinalizationRegistry(Realm& realm, JS::JobCallback cleanup_callback, Object& prototype)
    : Object(prototype)
    , WeakContainer(heap())
    , m_realm(JS::make_handle(realm))
    , m_cleanup_callback(move(cleanup_callback))
{
}

FinalizationRegistry::~FinalizationRegistry()
{
}

void FinalizationRegistry::add_finalization_record(Cell& target, Value held_value, Object* unregister_token)
{
    VERIFY(!held_value.is_empty());
    m_records.append({ &target, held_value, unregister_token });
}

bool FinalizationRegistry::remove_by_token(Object& unregister_token)
{
    auto removed = false;
    for (auto it = m_records.begin(); it != m_records.end(); ++it) {
        if (it->unregister_token == &unregister_token) {
            it.remove(m_records);
            removed = true;
        }
    }
    return removed;
}

void FinalizationRegistry::remove_dead_cells(Badge<Heap>)
{
    auto any_cells_were_removed = false;
    for (auto& record : m_records) {
        if (!record.target || record.target->state() == Cell::State::Live)
            continue;
        record.target = nullptr;
        any_cells_were_removed = true;
        break;
    }
    if (any_cells_were_removed)
        vm().host_enqueue_finalization_registry_cleanup_job(*this);
}

// 9.13 CleanupFinalizationRegistry ( finalizationRegistry ), https://tc39.es/ecma262/#sec-cleanup-finalization-registry
ThrowCompletionOr<void> FinalizationRegistry::cleanup(Optional<JobCallback> callback)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: finalizationRegistry has [[Cells]] and [[CleanupCallback]] internal slots.
    // Note: Ensured by type.

    // 2. Let callback be finalizationRegistry.[[CleanupCallback]].
    auto& cleanup_callback = callback.has_value() ? callback.value() : m_cleanup_callback;

    // 3. While finalizationRegistry.[[Cells]] contains a Record cell such that cell.[[WeakRefTarget]] is empty, an implementation may perform the following steps:
    for (auto it = m_records.begin(); it != m_records.end(); ++it) {
        // a. Choose any such cell.
        if (it->target != nullptr)
            continue;

        // b. Remove cell from finalizationRegistry.[[Cells]].
        MarkedVector<Value> arguments(vm.heap());
        arguments.append(it->held_value);
        it.remove(m_records);

        // c. Perform ? HostCallJobCallback(callback, undefined, « cell.[[HeldValue]] »).
        TRY(vm.host_call_job_callback(global_object, cleanup_callback, js_undefined(), move(arguments)));
    }

    // 4. Return NormalCompletion(empty).
    return {};
}

void FinalizationRegistry::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& record : m_records) {
        visitor.visit(record.held_value);
        visitor.visit(record.unregister_token);
    }
}

}
