/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibELF/DynamicObject.h>

namespace ELF {

using EntryPointFunction = int (*)(int, char**, char**);

class DynamicLinker {
public:
    static Optional<DynamicObject::SymbolLookupResult> lookup_global_symbol(StringView symbol);
    static EntryPointFunction linker_main(ByteString&& main_program_path, int fd, bool is_secure, char** envp);
    static int iterate_over_loaded_shared_objects(int (*callback)(struct dl_phdr_info* info, size_t size, void* data), void* data);

    static Optional<ByteString> resolve_library(ByteString const& name, DynamicObject const& parent_object);

private:
    DynamicLinker() = delete;
    ~DynamicLinker() = delete;
};

}
