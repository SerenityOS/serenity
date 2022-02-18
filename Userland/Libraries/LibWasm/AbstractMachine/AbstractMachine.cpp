/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/BytecodeInterpreter.h>
#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWasm/AbstractMachine/Validator.h>
#include <LibWasm/Types.h>

namespace Wasm {

Optional<FunctionAddress> Store::allocate(ModuleInstance& module, Module::Function const& function)
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

Optional<TableAddress> Store::allocate(TableType const& type)
{
    TableAddress address { m_tables.size() };
    Vector<Optional<Reference>> elements;
    elements.resize(type.limits().min());
    m_tables.empend(TableInstance { type, move(elements) });
    return address;
}

Optional<MemoryAddress> Store::allocate(MemoryType const& type)
{
    MemoryAddress address { m_memories.size() };
    auto instance = MemoryInstance::create(type);
    if (instance.is_error())
        return {};

    m_memories.append(instance.release_value());
    return address;
}

Optional<GlobalAddress> Store::allocate(GlobalType const& type, Value value)
{
    GlobalAddress address { m_globals.size() };
    m_globals.append(GlobalInstance { move(value), type.is_mutable() });
    return address;
}

Optional<DataAddress> Store::allocate_data(Vector<u8> initializer)
{
    DataAddress address { m_datas.size() };
    m_datas.append(DataInstance { move(initializer) });
    return address;
}

Optional<ElementAddress> Store::allocate(ValueType const& type, Vector<Reference> references)
{
    ElementAddress address { m_elements.size() };
    m_elements.append(ElementInstance { type, move(references) });
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

ElementInstance* Store::get(ElementAddress address)
{
    auto value = address.value();
    if (m_elements.size() <= value)
        return nullptr;
    return &m_elements[value];
}

DataInstance* Store::get(DataAddress address)
{
    auto value = address.value();
    if (m_datas.size() <= value)
        return nullptr;
    return &m_datas[value];
}

ErrorOr<void, ValidationError> AbstractMachine::validate(Module& module)
{
    if (module.validation_status() != Module::ValidationStatus::Unchecked) {
        if (module.validation_status() == Module::ValidationStatus::Valid)
            return {};

        return ValidationError { module.validation_error() };
    }

    auto result = Validator {}.validate(module);
    if (result.is_error()) {
        module.set_validation_error(result.error().error_string);
        return result.release_error();
    }

    return {};
}

InstantiationResult AbstractMachine::instantiate(Module const& module, Vector<ExternValue> externs)
{
    if (auto result = validate(const_cast<Module&>(module)); result.is_error())
        return InstantiationError { String::formatted("Validation failed: {}", result.error()) };

    auto main_module_instance_pointer = make<ModuleInstance>();
    auto& main_module_instance = *main_module_instance_pointer;
    Optional<InstantiationResult> instantiation_result;

    module.for_each_section_of_type<TypeSection>([&](TypeSection const& section) {
        main_module_instance.types() = section.types();
    });

    Vector<Value> global_values;
    Vector<Vector<Reference>> elements;
    ModuleInstance auxiliary_instance;

    // FIXME: Check that imports/extern match

    for (auto& entry : externs) {
        if (auto* ptr = entry.get_pointer<GlobalAddress>())
            auxiliary_instance.globals().append(*ptr);
    }

    BytecodeInterpreter interpreter;

    module.for_each_section_of_type<GlobalSection>([&](auto& global_section) {
        for (auto& entry : global_section.entries()) {
            Configuration config { m_store };
            if (m_should_limit_instruction_count)
                config.enable_instruction_count_limit();
            config.set_frame(Frame {
                auxiliary_instance,
                Vector<Value> {},
                entry.expression(),
                1,
            });
            auto result = config.execute(interpreter);
            if (result.is_trap())
                instantiation_result = InstantiationError { String::formatted("Global value construction trapped: {}", result.trap().reason) };
            else
                global_values.append(result.values().first());
        }
    });

    if (instantiation_result.has_value())
        return instantiation_result.release_value();

    if (auto result = allocate_all_initial_phase(module, main_module_instance, externs, global_values); result.has_value())
        return result.release_value();

    module.for_each_section_of_type<ElementSection>([&](ElementSection const& section) {
        for (auto& segment : section.segments()) {
            Vector<Reference> references;
            for (auto& entry : segment.init) {
                Configuration config { m_store };
                if (m_should_limit_instruction_count)
                    config.enable_instruction_count_limit();
                config.set_frame(Frame {
                    main_module_instance,
                    Vector<Value> {},
                    entry,
                    entry.instructions().size(),
                });
                auto result = config.execute(interpreter);
                if (result.is_trap()) {
                    instantiation_result = InstantiationError { String::formatted("Element construction trapped: {}", result.trap().reason) };
                    return IterationDecision::Continue;
                }

                for (auto& value : result.values()) {
                    if (!value.type().is_reference()) {
                        instantiation_result = InstantiationError { "Evaluated element entry is not a reference" };
                        return IterationDecision::Continue;
                    }
                    auto reference = value.to<Reference>();
                    if (!reference.has_value()) {
                        instantiation_result = InstantiationError { "Evaluated element entry does not contain a reference" };
                        return IterationDecision::Continue;
                    }
                    // FIXME: type-check the reference.
                    references.prepend(reference.release_value());
                }
            }
            elements.append(move(references));
        }

        return IterationDecision::Continue;
    });

    if (instantiation_result.has_value())
        return instantiation_result.release_value();

    if (auto result = allocate_all_final_phase(module, main_module_instance, elements); result.has_value())
        return result.release_value();

    module.for_each_section_of_type<ElementSection>([&](ElementSection const& section) {
        size_t index = 0;
        for (auto& segment : section.segments()) {
            auto current_index = index;
            ++index;
            auto active_ptr = segment.mode.get_pointer<ElementSection::Active>();
            if (!active_ptr)
                continue;
            if (active_ptr->index.value() != 0) {
                instantiation_result = InstantiationError { "Non-zero table referenced by active element segment" };
                return IterationDecision::Break;
            }
            Configuration config { m_store };
            if (m_should_limit_instruction_count)
                config.enable_instruction_count_limit();
            config.set_frame(Frame {
                main_module_instance,
                Vector<Value> {},
                active_ptr->expression,
                1,
            });
            auto result = config.execute(interpreter);
            if (result.is_trap()) {
                instantiation_result = InstantiationError { String::formatted("Element section initialisation trapped: {}", result.trap().reason) };
                return IterationDecision::Break;
            }
            auto d = result.values().first().to<i32>();
            if (!d.has_value()) {
                instantiation_result = InstantiationError { "Element section initialisation returned invalid table initial offset" };
                return IterationDecision::Break;
            }
            if (main_module_instance.tables().size() < 1) {
                instantiation_result = InstantiationError { "Element section initialisation references nonexistent table" };
                return IterationDecision::Break;
            }
            auto table_instance = m_store.get(main_module_instance.tables()[0]);
            if (current_index >= main_module_instance.elements().size()) {
                instantiation_result = InstantiationError { "Invalid element referenced by active element segment" };
                return IterationDecision::Break;
            }
            auto elem_instance = m_store.get(main_module_instance.elements()[current_index]);
            if (!table_instance || !elem_instance) {
                instantiation_result = InstantiationError { "Invalid element referenced by active element segment" };
                return IterationDecision::Break;
            }

            auto total_required_size = elem_instance->references().size() + d.value();

            if (table_instance->type().limits().max().value_or(total_required_size) < total_required_size) {
                instantiation_result = InstantiationError { "Table limit overflow in active element segment" };
                return IterationDecision::Break;
            }

            if (table_instance->elements().size() < total_required_size)
                table_instance->elements().resize(total_required_size);

            size_t i = 0;
            for (auto it = elem_instance->references().begin(); it < elem_instance->references().end(); ++i, ++it) {
                table_instance->elements()[i + d.value()] = *it;
            }
        }

        return IterationDecision::Continue;
    });

    if (instantiation_result.has_value())
        return instantiation_result.release_value();

    module.for_each_section_of_type<DataSection>([&](DataSection const& data_section) {
        for (auto& segment : data_section.data()) {
            segment.value().visit(
                [&](DataSection::Data::Active const& data) {
                    Configuration config { m_store };
                    if (m_should_limit_instruction_count)
                        config.enable_instruction_count_limit();
                    config.set_frame(Frame {
                        main_module_instance,
                        Vector<Value> {},
                        data.offset,
                        1,
                    });
                    auto result = config.execute(interpreter);
                    if (result.is_trap()) {
                        instantiation_result = InstantiationError { String::formatted("Data section initialisation trapped: {}", result.trap().reason) };
                        return;
                    }
                    size_t offset = 0;
                    result.values().first().value().visit(
                        [&](auto const& value) { offset = value; },
                        [&](Reference const&) { instantiation_result = InstantiationError { "Data segment offset returned a reference"sv }; });
                    if (instantiation_result.has_value() && instantiation_result->is_error())
                        return;
                    if (main_module_instance.memories().size() <= data.index.value()) {
                        instantiation_result = InstantiationError {
                            String::formatted("Data segment referenced out-of-bounds memory ({}) of max {} entries",
                                data.index.value(), main_module_instance.memories().size())
                        };
                        return;
                    }
                    auto maybe_data_address = m_store.allocate_data(data.init);
                    if (!maybe_data_address.has_value()) {
                        instantiation_result = InstantiationError { "Failed to allocate a data instance for an active data segment"sv };
                        return;
                    }
                    main_module_instance.datas().append(*maybe_data_address);

                    if (data.init.is_empty())
                        return;
                    auto address = main_module_instance.memories()[data.index.value()];
                    if (auto instance = m_store.get(address)) {
                        if (auto max = instance->type().limits().max(); max.has_value()) {
                            if (*max < data.init.size() + offset) {
                                instantiation_result = InstantiationError {
                                    String::formatted("Data segment attempted to write to out-of-bounds memory ({}) of max {} bytes",
                                        data.init.size() + offset, instance->type().limits().max().value())
                                };
                                return;
                            }
                        }
                        if (instance->size() < data.init.size() + offset)
                            instance->grow(data.init.size() + offset - instance->size());
                        instance->data().overwrite(offset, data.init.data(), data.init.size());
                    }
                },
                [&](DataSection::Data::Passive const& passive) {
                    auto maybe_data_address = m_store.allocate_data(passive.init);
                    if (!maybe_data_address.has_value()) {
                        instantiation_result = InstantiationError { "Failed to allocate a data instance for a passive data segment"sv };
                        return;
                    }
                    main_module_instance.datas().append(*maybe_data_address);
                });
        }
    });

    module.for_each_section_of_type<StartSection>([&](StartSection const& section) {
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

Optional<InstantiationError> AbstractMachine::allocate_all_initial_phase(Module const& module, ModuleInstance& module_instance, Vector<ExternValue>& externs, Vector<Value>& global_values)
{
    Optional<InstantiationError> result;

    for (auto& entry : externs) {
        entry.visit(
            [&](FunctionAddress const& address) { module_instance.functions().append(address); },
            [&](TableAddress const& address) { module_instance.tables().append(address); },
            [&](MemoryAddress const& address) { module_instance.memories().append(address); },
            [&](GlobalAddress const& address) { module_instance.globals().append(address); });
    }

    // FIXME: What if this fails?

    for (auto& func : module.functions()) {
        auto address = m_store.allocate(module_instance, func);
        VERIFY(address.has_value());
        module_instance.functions().append(*address);
    }

    module.for_each_section_of_type<TableSection>([&](TableSection const& section) {
        for (auto& table : section.tables()) {
            auto table_address = m_store.allocate(table.type());
            VERIFY(table_address.has_value());
            module_instance.tables().append(*table_address);
        }
    });

    module.for_each_section_of_type<MemorySection>([&](MemorySection const& section) {
        for (auto& memory : section.memories()) {
            auto memory_address = m_store.allocate(memory.type());
            VERIFY(memory_address.has_value());
            module_instance.memories().append(*memory_address);
        }
    });

    module.for_each_section_of_type<GlobalSection>([&](GlobalSection const& section) {
        size_t index = 0;
        for (auto& entry : section.entries()) {
            auto address = m_store.allocate(entry.type(), move(global_values[index]));
            VERIFY(address.has_value());
            module_instance.globals().append(*address);
            index++;
        }
    });
    module.for_each_section_of_type<ExportSection>([&](ExportSection const& section) {
        for (auto& entry : section.entries()) {
            Variant<FunctionAddress, TableAddress, MemoryAddress, GlobalAddress, Empty> address {};
            entry.description().visit(
                [&](FunctionIndex const& index) {
                    if (module_instance.functions().size() > index.value())
                        address = FunctionAddress { module_instance.functions()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.functions().size());
                },
                [&](TableIndex const& index) {
                    if (module_instance.tables().size() > index.value())
                        address = TableAddress { module_instance.tables()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.tables().size());
                },
                [&](MemoryIndex const& index) {
                    if (module_instance.memories().size() > index.value())
                        address = MemoryAddress { module_instance.memories()[index.value()] };
                    else
                        dbgln("Failed to export '{}', the exported address ({}) was out of bounds (min: 0, max: {})", entry.name(), index.value(), module_instance.memories().size());
                },
                [&](GlobalIndex const& index) {
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

Optional<InstantiationError> AbstractMachine::allocate_all_final_phase(Module const& module, ModuleInstance& module_instance, Vector<Vector<Reference>>& elements)
{
    module.for_each_section_of_type<ElementSection>([&](ElementSection const& section) {
        size_t index = 0;
        for (auto& segment : section.segments()) {
            auto address = m_store.allocate(segment.type, move(elements[index]));
            VERIFY(address.has_value());
            module_instance.elements().append(*address);
            index++;
        }
    });

    return {};
}

Result AbstractMachine::invoke(FunctionAddress address, Vector<Value> arguments)
{
    BytecodeInterpreter interpreter;
    return invoke(interpreter, address, move(arguments));
}

Result AbstractMachine::invoke(Interpreter& interpreter, FunctionAddress address, Vector<Value> arguments)
{
    Configuration configuration { m_store };
    if (m_should_limit_instruction_count)
        configuration.enable_instruction_count_limit();
    return configuration.call(interpreter, address, move(arguments));
}

void Linker::link(ModuleInstance const& instance)
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

void Linker::link(HashMap<Linker::Name, ExternValue> const& exports)
{
    populate();
    if (m_unresolved_imports.is_empty())
        return;

    if (exports.is_empty())
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
    m_module.for_each_section_of_type<ImportSection>([&](ImportSection const& section) {
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
