/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define ENUMERATE_PLEDGE_PROMISES         \
    __ENUMERATE_PLEDGE_PROMISE(stdio)     \
    __ENUMERATE_PLEDGE_PROMISE(rpath)     \
    __ENUMERATE_PLEDGE_PROMISE(wpath)     \
    __ENUMERATE_PLEDGE_PROMISE(cpath)     \
    __ENUMERATE_PLEDGE_PROMISE(dpath)     \
    __ENUMERATE_PLEDGE_PROMISE(inet)      \
    __ENUMERATE_PLEDGE_PROMISE(id)        \
    __ENUMERATE_PLEDGE_PROMISE(proc)      \
    __ENUMERATE_PLEDGE_PROMISE(ptrace)    \
    __ENUMERATE_PLEDGE_PROMISE(exec)      \
    __ENUMERATE_PLEDGE_PROMISE(unix)      \
    __ENUMERATE_PLEDGE_PROMISE(recvfd)    \
    __ENUMERATE_PLEDGE_PROMISE(sendfd)    \
    __ENUMERATE_PLEDGE_PROMISE(fattr)     \
    __ENUMERATE_PLEDGE_PROMISE(tty)       \
    __ENUMERATE_PLEDGE_PROMISE(chown)     \
    __ENUMERATE_PLEDGE_PROMISE(thread)    \
    __ENUMERATE_PLEDGE_PROMISE(video)     \
    __ENUMERATE_PLEDGE_PROMISE(accept)    \
    __ENUMERATE_PLEDGE_PROMISE(settime)   \
    __ENUMERATE_PLEDGE_PROMISE(sigaction) \
    __ENUMERATE_PLEDGE_PROMISE(setkeymap) \
    __ENUMERATE_PLEDGE_PROMISE(prot_exec) \
    __ENUMERATE_PLEDGE_PROMISE(map_fixed) \
    __ENUMERATE_PLEDGE_PROMISE(getkeymap)

namespace Kernel {

// This is evil, but avoids manual counting
constexpr size_t pledge_promise_count =
#define __ENUMERATE_PLEDGE_PROMISE(x) 1 +
    ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
    0;

#ifdef __cplusplus
enum class PledgeMode : u8 {
    None = 0,
    Promises = 1,
    ExecPromises = 2,
    Both = Promises | ExecPromises,
};
#else
enum PledgeMode {
    PLEDGE_MODE_PROMISES = 1,
    PLEDGE_MODE_EXEC_PROMISES = 2,
    PLEDGE_MODE_BOTH = PLEDGE_MODE_EXEC_PROMISES | PLEDGE_MODE_PROMISES,
};
#endif

#ifdef __cplusplus
enum class
#else
enum
#endif
    Pledge : u32 {
#define __ENUMERATE_PLEDGE_PROMISE(x) x,
        ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
    };

namespace PledgeBits {
#define __ENUMERATE_PLEDGE_PROMISE(promise) constexpr u32 const promise = 1 << static_cast<u64>(Kernel::Pledge::promise);
ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
};

}
