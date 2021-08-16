/*
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include <stdio.h>
#include <string.h>
#include <sys/ldr.h>

#include "java_md_aix.h"

static unsigned char dladdr_buffer[0x8000];

static int fill_dll_info(void) {
    return loadquery(L_GETINFO, dladdr_buffer, sizeof(dladdr_buffer));
}

static int dladdr_dont_reload(void *addr, Dl_info *info) {
    const struct ld_info *p = (struct ld_info *)dladdr_buffer;
    memset((void *)info, 0, sizeof(Dl_info));
    for (;;) {
        if (addr >= p->ldinfo_textorg &&
            addr < p->ldinfo_textorg + p->ldinfo_textsize) {
            info->dli_fname = p->ldinfo_filename;
            return 1;
        }
        if (!p->ldinfo_next) {
            break;
        }
        p = (struct ld_info *)(((char *)p) + p->ldinfo_next);
    }
    return 0;
}

int dladdr(void *addr, Dl_info *info) {
    static int loaded = 0;
    int rc = 0;
    void *addr0;
    if (!addr) {
        return rc;
    }
    if (!loaded) {
        if (fill_dll_info() == -1)
            return rc;
        loaded = 1;
    }

    // first try with addr on cached data
    rc = dladdr_dont_reload(addr, info);

    // addr could be an AIX function descriptor, so try dereferenced version
    if (rc == 0) {
        addr0 = *((void **)addr);
        rc = dladdr_dont_reload(addr0, info);
    }

    // if we had no success until now, maybe loadquery info is outdated.
    // refresh and retry
    if (rc == 0) {
        if (fill_dll_info() == -1)
            return rc;
        rc = dladdr_dont_reload(addr, info);
        if (rc == 0) {
            rc = dladdr_dont_reload(addr0, info);
        }
    }
    return rc;
}
