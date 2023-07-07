/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

enum FSETLOCKING {
  FSETLOCKING_QUERY = 0,
  FSETLOCKING_INTERNAL,
  FSETLOCKING_BYCALLER
};

#define FSETLOCKING_QUERY	FSETLOCKING_QUERY
#define FSETLOCKING_INTERNAL	FSETLOCKING_INTERNAL
#define FSETLOCKING_BYCALLER	FSETLOCKING_BYCALLER

__END_DECLS
