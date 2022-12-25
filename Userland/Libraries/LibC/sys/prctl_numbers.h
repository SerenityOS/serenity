/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define PR_SET_DUMPABLE 1
#define PR_GET_DUMPABLE 2
#define PR_SET_NO_NEW_PRIVS 3
#define PR_GET_NO_NEW_PRIVS 4

#define NO_NEW_PRIVS_MODE_DISABLED 0
#define NO_NEW_PRIVS_MODE_ENFORCED 1
#define NO_NEW_PRIVS_MODE_ENFORCED_QUIETLY 2
