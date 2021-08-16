/*
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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

#ifndef JAVA_MD_AIX_H
#define JAVA_MD_AIX_H

/*
 * Very limited AIX port of dladdr() for libjli.so.
 *
 * We try to mimick dladdr(3) on Linux (see http://linux.die.net/man/3/dladdr)
 * dladdr(3) is not POSIX but a GNU extension, and is not available on AIX.
 *
 * We only support Dl_info.dli_fname here as this is the only thing that is
 * used of it by libjli.so. A more comprehensive port of dladdr can be found
 * in the hotspot implementation which is not available at this place, though.
 */

typedef struct {
  const char *dli_fname; /* file path of loaded library */
  void *dli_fbase;       /* unsupported */
  const char *dli_sname; /* unsupported */
  void *dli_saddr;       /* unsupported */
} Dl_info;

int dladdr(void *addr, Dl_info *info);

#endif /* JAVA_MD_AIX_H */
