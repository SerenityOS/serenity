/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/API/POSIX/sys/auxv.h>

static_assert(sizeof(auxv_t) % sizeof(FlatPtr) == 0);

namespace ELF {

struct AuxiliaryValue {
    enum Type {
        Null = AT_NULL,
        Ignore = AT_IGNORE,
        ExecFileDescriptor = AT_EXECFD,
        Phdr = AT_PHDR,
        Phent = AT_PHENT,
        Phnum = AT_PHNUM,
        PageSize = AT_PAGESZ,
        BaseAddress = AT_BASE,
        Flags = AT_FLAGS,
        Entry = AT_ENTRY,
        NotELF = AT_NOTELF,
        Uid = AT_UID,
        EUid = AT_EUID,
        Gid = AT_GID,
        EGid = AT_EGID,
        Platform = AT_PLATFORM,
        HwCap = AT_HWCAP,
        ClockTick = AT_CLKTCK,
        Secure = AT_SECURE,
        BasePlatform = AT_BASE_PLATFORM,
        Random = AT_RANDOM,
        HwCap2 = AT_HWCAP2,
        ExecFilename = AT_EXECFN,
        ExeBaseAddress = AT_EXE_BASE,
        ExeSize = AT_EXE_SIZE
    };

    AuxiliaryValue(Type type, long val)
    {
        auxv.a_type = type;
        auxv.a_un.a_val = val;
    }
    AuxiliaryValue(Type type, void* ptr)
    {
        auxv.a_type = type;
        auxv.a_un.a_ptr = (void*)ptr;
    }
    AuxiliaryValue(Type type, StringView string)
        : optional_string(string)
    {
        auxv.a_type = type;
        auxv.a_un.a_ptr = nullptr;
    }

    auxv_t auxv {};
    StringView optional_string;
};

}
