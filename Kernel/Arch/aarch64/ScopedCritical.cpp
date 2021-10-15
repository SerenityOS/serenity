/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/ScopedCritical.h>

#include <Kernel/Arch/Processor.h>

namespace Kernel {

ScopedCritical::ScopedCritical()
{
}

ScopedCritical::~ScopedCritical()
{
}

ScopedCritical::ScopedCritical(ScopedCritical&& /*from*/)
{
}

ScopedCritical& ScopedCritical::operator=(ScopedCritical&& /*from*/)
{
    return *this;
}

void ScopedCritical::leave() { }

void ScopedCritical::enter() { }

}
