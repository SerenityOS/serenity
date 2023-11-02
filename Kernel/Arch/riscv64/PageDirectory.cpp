/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Singleton.h>

#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>

namespace Kernel::Memory {

void PageDirectory::register_page_directory(PageDirectory*)
{
    TODO_RISCV64();
}

void PageDirectory::deregister_page_directory(PageDirectory*)
{
    TODO_RISCV64();
}

ErrorOr<NonnullLockRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace(Process&)
{
    TODO_RISCV64();
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    TODO_RISCV64();
}

void activate_kernel_page_directory(PageDirectory const&)
{
    TODO_RISCV64();
}

void activate_page_directory(PageDirectory const&, Thread*)
{
    TODO_RISCV64();
}

UNMAP_AFTER_INIT NonnullLockRefPtr<PageDirectory> PageDirectory::must_create_kernel_page_directory()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) PageDirectory).release_nonnull();
}

PageDirectory::PageDirectory() = default;

UNMAP_AFTER_INIT void PageDirectory::allocate_kernel_directory()
{
    TODO_RISCV64();
}

PageDirectory::~PageDirectory()
{
    if (is_root_table_initialized()) {
        deregister_page_directory(this);
    }
}

}
