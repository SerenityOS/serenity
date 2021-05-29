/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KSyms.h>
#include <Kernel/Module.h>
#include <Kernel/Process.h>
#include <LibELF/Image.h>

namespace Kernel {

extern HashMap<String, OwnPtr<Module>>* g_modules;

KResultOr<int> Process::sys$module_load(Userspace<const char*> user_path, size_t path_length)
{
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto description_or_error = VFS::the().open(path.value()->view(), O_RDONLY, 0, current_directory());
    if (description_or_error.is_error())
        return description_or_error.error();
    auto& description = description_or_error.value();
    auto payload_or_error = description->read_entire_file();
    if (payload_or_error.is_error())
        return payload_or_error.error();

    auto& payload = *payload_or_error.value();
    auto storage = KBuffer::create_with_size(payload.size());
    memcpy(storage.data(), payload.data(), payload.size());

    auto elf_image = adopt_own_if_nonnull(new ELF::Image(storage.data(), storage.size()));
    if (!elf_image)
        return ENOMEM;
    if (!elf_image->parse())
        return ENOEXEC;

    HashMap<String, u8*> section_storage_by_name;

    auto module = adopt_own_if_nonnull(new Module());
    if (!module)
        return ENOMEM;

    elf_image->for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
        if (!section.size())
            return;
        auto section_storage = KBuffer::copy(section.raw_data(), section.size(), Region::Access::Read | Region::Access::Write | Region::Access::Execute);
        section_storage_by_name.set(section.name(), section_storage.data());
        module->sections.append(move(section_storage));
    });

    bool missing_symbols = false;

    elf_image->for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
        if (!section.size())
            return;

        auto* section_storage = section_storage_by_name.get(section.name()).value_or(nullptr);
        VERIFY(section_storage);
        auto relocations = section.relocations();
        VERIFY(relocations.has_value());
        relocations->for_each_relocation([&](const ELF::Image::Relocation& relocation) {
            auto& patch_ptr = *reinterpret_cast<ptrdiff_t*>(section_storage + relocation.offset());
            switch (relocation.type()) {
            case R_386_PC32: {
                // PC-relative relocation
                dbgln("PC-relative relocation: {}", relocation.symbol().name());
                auto symbol_address = address_for_kernel_symbol(relocation.symbol().name());
                if (symbol_address == 0)
                    missing_symbols = true;
                dbgln("   Symbol address: {:p}", symbol_address);
                ptrdiff_t relative_offset = (FlatPtr)symbol_address - ((FlatPtr)&patch_ptr + 4);
                patch_ptr = relative_offset;
                break;
            }
            case R_386_32: // Absolute relocation
                dbgln("Absolute relocation: '{}' value={}, index={}", relocation.symbol().name(), relocation.symbol().value(), relocation.symbol_index());

                if (relocation.symbol().bind() == STB_LOCAL) {
                    auto* section_storage_containing_symbol = section_storage_by_name.get(relocation.symbol().section().name()).value_or(nullptr);
                    VERIFY(section_storage_containing_symbol);
                    u32 symbol_address = (ptrdiff_t)(section_storage_containing_symbol + relocation.symbol().value());
                    if (symbol_address == 0)
                        missing_symbols = true;
                    dbgln("   Symbol address: {:p}", symbol_address);
                    patch_ptr += symbol_address;
                } else if (relocation.symbol().bind() == STB_GLOBAL) {
                    u32 symbol_address = address_for_kernel_symbol(relocation.symbol().name());
                    if (symbol_address == 0)
                        missing_symbols = true;
                    dbgln("   Symbol address: {:p}", symbol_address);
                    patch_ptr += symbol_address;
                } else {
                    VERIFY_NOT_REACHED();
                }
                break;
            }
        });
    });

    if (missing_symbols)
        return EINVAL;

    auto* text_base = section_storage_by_name.get(".text").value_or(nullptr);
    if (!text_base) {
        dbgln("No .text section found in module!");
        return EINVAL;
    }

    elf_image->for_each_symbol([&](const ELF::Image::Symbol& symbol) {
        dbgln(" - {} '{}' @ {:p}, size={}", symbol.type(), symbol.name(), symbol.value(), symbol.size());
        if (symbol.name() == "module_init") {
            module->module_init = (ModuleInitPtr)(text_base + symbol.value());
        } else if (symbol.name() == "module_fini") {
            module->module_fini = (ModuleFiniPtr)(text_base + symbol.value());
        } else if (symbol.name() == "module_name") {
            const u8* storage = section_storage_by_name.get(symbol.section().name()).value_or(nullptr);
            if (storage)
                module->name = String((const char*)(storage + symbol.value()));
        }
    });

    if (!module->module_init)
        return EINVAL;

    if (g_modules->contains(module->name)) {
        dbgln("a module with the name {} is already loaded; please unload it first", module->name);
        return EEXIST;
    }

    module->module_init();

    auto name = module->name;
    g_modules->set(name, move(module));

    return 0;
}

KResultOr<int> Process::sys$module_unload(Userspace<const char*> user_name, size_t name_length)
{
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    auto module_name = copy_string_from_user(user_name, name_length);
    if (module_name.is_null())
        return EFAULT;

    auto it = g_modules->find(module_name);
    if (it == g_modules->end())
        return ENOENT;

    if (it->value->module_fini)
        it->value->module_fini();

    g_modules->remove(it);
    return 0;
}

}
