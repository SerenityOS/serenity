/*
 * Copyright (c) 2012, 2018 SAP SE. All rights reserved.
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
 *
 */

/*
 * Header file to contain porting-relevant code which does not have a
 * home anywhere else.
 * This is intially based on hotspot/src/os/aix/vm/{loadlib,porting}_aix.{hpp,cpp}
 */

/*
 * Aix' own version of dladdr().
 * This function tries to mimick dladdr(3) on Linux
 * (see http://linux.die.net/man/3/dladdr)
 * dladdr(3) is not POSIX but a GNU extension, and is not available on AIX.
 *
 * Differences between AIX dladdr and Linux dladdr:
 *
 * 1) Dl_info.dli_fbase: can never work, is disabled.
 *   A loaded image on AIX is divided in multiple segments, at least two
 *   (text and data) but potentially also far more. This is because the loader may
 *   load each member into an own segment, as for instance happens with the libC.a
 * 2) Dl_info.dli_sname: This only works for code symbols (functions); for data, a
 *   zero-length string is returned ("").
 * 3) Dl_info.dli_saddr: For code, this will return the entry point of the function,
 *   not the function descriptor.
 */

typedef struct {
  const char *dli_fname; /* file path of loaded library */
  void *dli_fbase;       /* doesn't make sence on AIX */
  const char *dli_sname; /* symbol name; "" if not known */
  void *dli_saddr;       /* address of *entry* of function; not function descriptor; */
} Dl_info;

#ifdef __cplusplus
extern "C"
#endif
int dladdr(void *addr, Dl_info *info);
