# Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
# Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
#
# SPDX-License-Identifier: BSD-2-Clause
#

if (NOT APPLE AND "${LAGOM_USE_LINKER}" STREQUAL "")
    find_program(LLD_LINKER NAMES "ld.lld")
    if (LLD_LINKER)
        message("Using LLD to link Lagom.")
        set(LAGOM_USE_LINKER "lld" CACHE STRING "")
    else()
        find_program(MOLD_LINKER NAMES "ld.mold")
        if (MOLD_LINKER)
            message("Using mold to link Lagom.")
            set(LAGOM_USE_LINKER "mold" CACHE STRING "")
        endif()
    endif()
endif()

if (NOT "${LAGOM_USE_LINKER}" STREQUAL "")
    set(LINKER_FLAG "-fuse-ld=${LAGOM_USE_LINKER}")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${LINKER_FLAG}")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAG}")
    set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAG}")
endif()
