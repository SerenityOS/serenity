/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<ProcFSComponentRegistry> s_the;

ProcFSComponentRegistry& ProcFSComponentRegistry::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT void ProcFSComponentRegistry::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
}

UNMAP_AFTER_INIT ProcFSComponentRegistry::ProcFSComponentRegistry()
    : m_root_directory(ProcFSRootDirectory::must_create())
{
}

}
