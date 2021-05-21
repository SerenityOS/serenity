/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/Types.h>

namespace Wasm {

Optional<FunctionAddress> Store::allocate(ModuleInstance& module, const Module::Function& function)
{
    FunctionAddress address { m_functions.size() };
    if (function.type().value() > module.types().size())
        return {};

    auto& type = module.types()[function.type().value()];
    m_functions.empend(WasmFunction { type, module, function });
    return address;
}

Optional<FunctionAddress> Store::allocate(HostFunction&& function)
{
    FunctionAddress address { m_functions.size() };
    m_functions.empend(HostFunction { move(function) });
    return address;
}

Optional<TableAddress> Store::allocate(const TableType& type)
{
    TableAddress address { m_tables.size() };
    Vector<Optional<Reference>> elements;
    elements.resize(type.limits().min());
    m_tables.empend(TableInstance { type, move(elements) });
    return address;
}

Optional<MemoryAddress> Store::allocate(const MemoryType& type)
{
    MemoryAddress address { m_memories.size() };
    m_memories.empend(MemoryInstance { type });
    return address;
}

Optional<GlobalAddress> Store::allocate(const GlobalType& type, Value value)
{
    GlobalAddress address { m_globals.size() };
    m_globals.append(GlobalInstance { move(value), type.is_mutable() });
    return address;
}

FunctionInstance* Store::get(FunctionAddress address)
{
    auto value = address.value();
    if (m_functions.size() <= value)
        return nullptr;
    return &m_functions[value];
}

TableInstance* Store::get(TableAddress address)
{
    auto value = address.value();
    if (m_tables.size() <= value)
        return nullptr;
    return &m_tables[value];
}

MemoryInstance* Store::get(MemoryAddress address)
{
    auto value = address.value();
    if (m_memories.size() <= value)
        return nullptr;
    return &m_memories[value];
}

GlobalInstance* Store::get(GlobalAddress address)
{
    auto value = address.value();
    if (m_globals.size() <= value)
        return nullptr;
    return &m_globals[value];
}

InstantiationResult AbstractMachine::instantiate(const Module& module, Vector<ExternValue> externs)
{
    auto main_module_instance_pointer = make<ModuleInstance>();
    auto& main_module_instance = *main_module_instance_pointer;
    Optional<InstantiationResult> instantiation_result;

    module.for_each_section_of_type<TypeSection>([&](const TypeSection& section) {
        main_module_instance.types() = section.types();
    });

    // FIXME: Validate stuff

    Vector<Value> global_values;
    ModuleInstance auxiliary_instance;

    // FIXME: Check that imports/extern match

    for (auto& entry : externs) {
        if (auto* ptr = entry.get_pointer<GlobalAddress>())
            auxiliary_instance.globals().append(*ptr);
    }

    module.for_each_section_of_type<GlobalSection>([&](auto& global_section) {
        for (auto& entry : global_section.entries()) {
            auto frame = make<Frame>(
                auxiliary_instance,
                Vector<Value> {},
                entry.expression(),
                1);
            Configuration config { m_store };
            config.pre_interpret_hook = &pre_interpret_hook;
            config.post_interpret_hook = &post_interpret_hook;
            config.set_frame(move(frame));
            auto result = config.execute();
            // What if this traps?
            if (result.is_trap())
                instantiation_result = InstantiationError { "Global value construction trapped" };
            else
                global_values.append(result.values().first());
        }
    });

    if (auto result = allocate_all(module, main_module_instance, externs, global_values); result.has_value()) {
        return result.release_value();
    }

    module.for_each_section_of_type<ElementSection>([&](const ElementSection&) {
        // FIXME: Implement me
        // https://webassembly.github.io/spec/core/bikeshed/#element-segments%E2%91%A0
        // https://webassembly.github.io/spec/core/bikeshed/#instantiation%E2%91%A1 step 9
    });

    module.for_each_section_of_type<DataSection>([&](const DataSection& data_section) {
        for (auto& segment : data_section.data()) {
            segment.value().visit(
                [&](const DataSection::Data::Active& data) {
                    auto frame = make<Frame>(
                        main_module_instance,
                        Vector<Value> {},
                        data.offset,
                        1);
                    Configuration config { m_store };
                    config.pre_interpret_hook = &pre_interpret_hook;
                    config.post_interpret_hook = &post_interpret_hook;
                    config.set_frame(move(frame));
                    auto result = config.execute();
                    size_t offset = 0;
                    result.values().first().value().visit(
                        [&](const auto& value) { offset = value; },
                        [&](const FunctionAddress&) { instantiation_result = InstantiationError { "Data segment offset returned an address" }; },
                        [&](const ExternAddress&) { instantiation_result = InstantiationError { "Data segment offset returned an address" }; });
                    if (instantiation_result.has_value() && instantiation_result->is_error())
                        return;
                    if (main_module_instance.memories().size() <= data.index.value()) {
                        instantiation_result = InstantiationError { String::formatted("Data segment referenced out-of-bounds memory ({}) of max {} entries", data.index.value(), main_module_instance.memories().size()) };
                        return;
                    }
                    auto address = main_module_instance.memories()[data.index.value()];
                    if (auto instance = m_store.get(address)) {
                        if (instance->type().limits().max().value_or(data.init.size() + offset + 1) <= data.init.size() + offset) {
                            instantiation_result = InstantiationError { String::formatted("Data segment attempted to write to out-of-bounds memory ({}) of max {} bytes", data.init.size() + offset, instance->type().limits().max().value()) };
                            return;
                        }
                        if (instance->size() < data.init.size() + offset)
                            instance->grow(data.init.size() + offset - instance->size());
                        instance->data().overwrite(offset, data.init.data(), data.init.size());
                    }
                },
                [&](const DataSection::Data::Passive&) {
                    // FIXME: What do we do here?
                });
        }
    });

    module.for_each_section_of_type<StartSection>([&](const StartSection& section) {
        auto& functions = main_module_instance.functions();
        auto index = section.function().index();
        if (functions.size() <= index.value()) {
            instantiation_result = InstantiationError { String::formatted("Start section function referenced invalid index {} of max {} entries", index.value(), functions.size()) };
            return;
        }
        invoke(functions[index.value()], {});
    });

    if (instantiation_result.has_value())
        return instantiation_result.release_value();

    return InstantiationResult { move(main_module_instance_pointer) };
}

Optional<InstantiationError> AbstractMachine::allocate_all(const Module& module, ModuleInstance& module_instance, Vector<ExternValue>& externs, Vector<Value>& global_values)
{
    Optional<InstantiationError> result;

    for (auto& entry : externs) {
        entry.visit(
            [&](const FunctionAddress& address) { module_instance.functions().append(address); },
            [&](const TableAddress& address) { module_instance.tables().append(address); },
            [&](const MemoryAddress& address) { module_instance.memories().append(address); },
            [&](const GlobalAddress& address) { module_instance.globals().append(address); });
    }

    // FIXME: What if this fails?

    for (auto& func : module.functions()) {
        auto address = m_store.allocate(module_instance, func);
        VERIFY(address.has_value());
        module_instance.functions().append(*address);
    }

    module.for_each_section_of_type<TableSection>([&](const TableSection& section) {
        for (auto& table : section.tables()) {
            auto table_address = m_store.allocate(table.type());
            VERIFY(table_address.has_value());
            module_instance.tables().append(*table_address);
        }
    });

    module.for_each_section_of_type<MemorySection>([&](const MemorySection& section) {
        for (auto& memory : section.memories()) {
            auto memory_address = m_store.allocate(memory.type());
            VERIFY(memory_address.has_value());
            module_instance.memories().append(*memory_address);
        }
    });

    module.for_each_section_of_type<GlobalSection>([&](const GlobalSection& section) {
        size_t index = 0;
        for (auto& entry : section.entries()) {
            auto address = m_store.allocate(entry.type(), global_values[index]);
            VERIFY(address.has_value());
            module_instance.globals().append(*address);
            index++;
        }
    });

    module.for_each_section_of_type<ExportSection>([&](const ExportSection& section) {
        for (auto& entry : section.entries()) {
            Variant<FunctionAddress, TableAddress, MemoryAddress, GlobalAddress, Empty> address { Empty {} };
            entry.description().visit(
                [&](const FunctionIndex& index) {
                    if (module_instance.functions().size() > index.value())
                        address = FunctionAddress { module_instance.functions()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.functions().size());
                },
                [&](const TableIndex& index) {
                    if (module_instance.tables().size() > index.value())
                        address = TableAddress { module_instance.tables()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.tables().size());
                },
                [&](const MemoryIndex& index) {
                    if (module_instance.memories().size() > index.value())
                        address = MemoryAddress { module_instance.memories()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.memories().size());
                },
                [&](const GlobalIndex& index) {
                    if (module_instance.globals().size() > index.value())
                        address = GlobalAddress { module_instance.globals()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.globals().size());
                });

            if (address.has<Empty>()) {
                result = InstantiationError { "An export could not be resolved" };
                continue;
            }

            module_instance.exports().append(ExportInstance {
                entry.name(),
                move(address).downcast<FunctionAddress, TableAddress, MemoryAddress, GlobalAddress>(),
            });
        }
    });

    return result;
}

Result AbstractMachine::invoke(FunctionAddress address, Vector<Value> arguments)
{
    Configuration configuration { m_store };
    configuration.pre_interpret_hook = &pre_interpret_hook;
    configuration.post_interpret_hook = &post_interpret_hook;
    return configuration.call(address, move(arguments));
}

void Linker::link(const ModuleInstance& instance)
{
    populate();
    if (m_unresolved_imports.is_empty())
        return;

    HashTable<Name> resolved_imports;
    for (auto& import_ : m_unresolved_imports) {
        auto it = instance.exports().find_if([&](auto& export_) { return export_.name() == import_.name; });
        if (!it.is_end()) {
            resolved_imports.set(import_);
            m_resolved_imports.set(import_, it->value());
        }
    }

    for (auto& entry : resolved_imports)
        m_unresolved_imports.remove(entry);
}

void Linker::link(const HashMap<Linker::Name, ExternValue>& exports)
{
    populate();
    if (m_unresolved_imports.is_empty())
        return;

    HashTable<Name> resolved_imports;
    for (auto& import_ : m_unresolved_imports) {
        auto export_ = exports.get(import_);
        if (export_.has_value()) {
            resolved_imports.set(import_);
            m_resolved_imports.set(import_, export_.value());
        }
    }

    for (auto& entry : resolved_imports)
        m_unresolved_imports.remove(entry);
}

AK::Result<Vector<ExternValue>, LinkError> Linker::finish()
{
    populate();
    if (!m_unresolved_imports.is_empty()) {
        if (!m_error.has_value())
            m_error = LinkError {};
        for (auto& entry : m_unresolved_imports)
            m_error->missing_imports.append(entry.name);
        return *m_error;
    }

    if (m_error.has_value())
        return *m_error;

    // Result must be in the same order as the module imports
    Vector<ExternValue> exports;
    exports.ensure_capacity(m_ordered_imports.size());
    for (auto& import_ : m_ordered_imports)
        exports.unchecked_append(*m_resolved_imports.get(import_));
    return exports;
}

void Linker::populate()
{
    if (!m_ordered_imports.is_empty())
        return;

    // There better be at most one import section!
    bool already_seen_an_import_section = false;
    m_module.for_each_section_of_type<ImportSection>([&](const ImportSection& section) {
        if (already_seen_an_import_section) {
            if (!m_error.has_value())
                m_error = LinkError {};
            m_error->other_errors.append(LinkError::InvalidImportedModule);
            return;
        }
        already_seen_an_import_section = true;
        for (auto& import_ : section.imports()) {
            m_ordered_imports.append({ import_.module(), import_.name(), import_.description() });
            m_unresolved_imports.set(m_ordered_imports.last());
        }
    });
}

}
