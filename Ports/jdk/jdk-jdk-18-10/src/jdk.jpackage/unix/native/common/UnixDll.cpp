/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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


#include <dlfcn.h>
#include "Dll.h"
#include "FileUtils.h"
#include "ErrorHandling.h"


namespace {

HMODULE loadLibrary(const tstring& path) {
    HMODULE h = dlopen(path.c_str(), RTLD_LAZY);
    if (!h) {
        JP_THROW(tstrings::any() << "dlopen(" << path
                << ") failed. Error: " << dlerror());
    }
    return h;
}

} // namesace

Dll::Dll(const tstrings::any &libPath): thePath(libPath.tstr()),
                                        handle(loadLibrary(thePath)) {
}

Dll::Dll(const Dll& other): thePath(other.thePath),
                            handle(loadLibrary(thePath)) {
}

void* Dll::getFunction(const std::string &name, bool throwIfNotFound) const {
    void *ptr = dlsym(handle.get(), name.c_str());
    if (throwIfNotFound && !ptr) {
        JP_THROW(tstrings::any() << "dlsym(" << thePath
                << ", " << name << ") failed. Error: " << dlerror());
    }
    return ptr;
}

/*static*/
void Dll::freeLibrary(HMODULE h) {
    if (h) {
        dlclose(h);
    }
}
