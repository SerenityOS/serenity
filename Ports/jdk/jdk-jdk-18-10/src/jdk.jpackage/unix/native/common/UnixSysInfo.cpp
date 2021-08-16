/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include "SysInfo.h"
#include "UnixSysInfo.h"
#include "ErrorHandling.h"

namespace SysInfo {

tstring getEnvVariable(const tstring& name) {
    char *value = ::getenv(name.c_str());
    if (!value) {
        JP_THROW(tstrings::any()    << "getenv("
                                    << name
                                    << ") failed. Variable not set");
    }
    return tstring(value);
}


tstring getEnvVariable(const std::nothrow_t&, const tstring& name,
                                                    const tstring& defValue) {
    char *value = ::getenv(name.c_str());
    if (value) {
        return tstring(value);
    }
    return defValue;
}


bool isEnvVariableSet(const tstring& name) {
    return ::getenv(name.c_str()) != 0;
}

void setEnvVariable(const tstring& name, const tstring& value) {
    ::setenv(name.c_str(), value.c_str(), 1);
}


int argc = 0;
char** argv = 0;

} // end of namespace SysInfo
