/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDl/dlfcn_integration.h>

// These are used by libdl and are filled in by the dynamic loader.
DlCloseFunction __dlclose;
DlOpenFunction __dlopen;
DlSymFunction __dlsym;
DlAddrFunction __dladdr;
