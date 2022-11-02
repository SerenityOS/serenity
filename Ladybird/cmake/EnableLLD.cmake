# Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
#
# SPDX-License-Identifier: BSD-2-Clause
#
option(LADYBIRD_USE_LLD "Use llvm lld to link application" ON)
if (LADYBIRD_USE_LLD AND NOT APPLE)
    find_program(LLD_LINKER NAMES "ld.lld")
    if (NOT LLD_LINKER)
        message(INFO "LLD not found, cannot use to link. Disabling option...")
        set(LADYBIRD_USE_LLD OFF CACHE BOOL "" FORCE)
    endif()
endif()
if (LADYBIRD_USE_LLD AND NOT APPLE)
    add_link_options(-fuse-ld=lld)
    add_compile_options(-ggnu-pubnames)
    add_link_options(LINKER:--gdb-index)
endif()
