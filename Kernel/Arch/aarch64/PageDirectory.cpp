/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Memory/PageDirectory.h>

namespace Kernel::Memory {

void PageDirectory::register_page_directory(PageDirectory*)
{
    TODO_AARCH64();
}

void PageDirectory::deregister_page_directory(PageDirectory*)
{
    TODO_AARCH64();
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    TODO_AARCH64();
    return nullptr;
}

void activate_kernel_page_directory(PageDirectory const&)
{
    // FIXME: Implement this
}

void activate_page_directory(PageDirectory const&, Thread*)
{
    TODO_AARCH64();
}

}
