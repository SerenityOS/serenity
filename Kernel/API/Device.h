/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

static ALWAYS_INLINE dev_t serenity_dev_makedev(unsigned major, unsigned minor)
{
    return (minor & 0xffu) | (major << 8u) | ((minor & ~0xffu) << 12u);
}

static ALWAYS_INLINE unsigned int serenity_dev_major(dev_t dev)
{
    return (dev & 0xfff00u) >> 8u;
}

static ALWAYS_INLINE unsigned int serenity_dev_minor(dev_t dev)
{
    return (dev & 0xffu) | ((dev >> 12u) & 0xfff00u);
}

__END_DECLS
