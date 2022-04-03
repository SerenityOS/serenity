/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Memory/PageDirectory.h>

namespace Kernel::Memory {

void PageDirectory::register_page_directory(PageDirectory*)
{
}

void PageDirectory::deregister_page_directory(PageDirectory*)
{
}

RefPtr<PageDirectory> PageDirectory::find_current()
{
    return nullptr;
}

void activate_kernel_page_directory(PageDirectory const&)
{
}

void activate_page_directory(PageDirectory const&, Thread*)
{
}

}
