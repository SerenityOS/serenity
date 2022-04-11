/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/ScopedCritical.h>

#include <Kernel/Arch/Processor.h>

namespace Kernel {

ScopedCritical::ScopedCritical() = default;
ScopedCritical::~ScopedCritical() = default;

ScopedCritical::ScopedCritical(ScopedCritical&& /*from*/)
{
    VERIFY_NOT_REACHED();
}

ScopedCritical& ScopedCritical::operator=(ScopedCritical&& /*from*/)
{
    VERIFY_NOT_REACHED();
    return *this;
}

void ScopedCritical::leave()
{
    VERIFY_NOT_REACHED();
}

void ScopedCritical::enter()
{
    VERIFY_NOT_REACHED();
}

}
