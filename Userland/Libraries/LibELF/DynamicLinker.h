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

class DynamicLinker {
public:
    static Optional<DynamicObject::SymbolLookupResult> lookup_global_symbol(StringView symbol);
    [[noreturn]] static void linker_main(String&& main_program_name, int fd, bool is_secure, int argc, char** argv, char** envp);

private:
    DynamicLinker() = delete;
    ~DynamicLinker() = delete;
};

}
