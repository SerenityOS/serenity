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
    TODO_AARCH64();
}

ScopedCritical& ScopedCritical::operator=(ScopedCritical&& /*from*/)
{
    TODO_AARCH64();
    return *this;
}

void ScopedCritical::leave()
{
    TODO_AARCH64();
}

void ScopedCritical::enter()
{
    TODO_AARCH64();
}

}
