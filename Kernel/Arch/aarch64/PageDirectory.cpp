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
    VERIFY_NOT_REACHED();
}

void PageDirectory::deregister_page_directory(PageDirectory*)
{
    VERIFY_NOT_REACHED();
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    VERIFY_NOT_REACHED();
    return nullptr;
}

void activate_kernel_page_directory(PageDirectory const&)
{
    // FIXME: Implement this
}

void activate_page_directory(PageDirectory const&, Thread*)
{
    VERIFY_NOT_REACHED();
}

}
