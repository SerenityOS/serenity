/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/device.h>

#define makedev(major, minor) serenity_dev_makedev((major), (minor))
#define major(dev) serenity_dev_major(dev)
#define minor(dev) serenity_dev_minor(dev)
