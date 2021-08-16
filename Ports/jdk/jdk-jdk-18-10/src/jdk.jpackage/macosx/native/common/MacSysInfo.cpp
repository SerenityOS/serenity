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

#include <sys/stat.h>
#include <unistd.h>
#include <mach-o/dyld.h>
#include "UnixSysInfo.h"
#include "FileUtils.h"
#include "ErrorHandling.h"

namespace SysInfo {

tstring getRealPath(const std::vector<char>& in) {
    std::vector<char> out(PATH_MAX);

    struct stat sb;
    if (lstat(in.data(), &sb) == -1) {
        JP_THROW(tstrings::any() << "lstat(" << in.data()
                << ") failed. Error: " << lastCRTError());
    }

    // readlink() will fail if called on real path, so if we have real path, then just
    // use it
    if (!S_ISLNK(sb.st_mode)) {
        return tstring(in.data(), in.size() - 1 /* don't count trailing '0' */);
    }

    // Get real path, since _NSGetExecutablePath can return symbolic link
    ssize_t len = readlink(in.data(), out.data(), PATH_MAX);
    if (len < 0) {
        JP_THROW(tstrings::any() << "readlink(" << in.data()
                << ") failed. Error: " << lastCRTError());
    }

    return tstring(out.data(), len);
}

tstring getProcessModulePath() {
    std::vector<char> buffer;
    uint32_t bufferSize = 0;
    do {
        int len = _NSGetExecutablePath(buffer.data(), &bufferSize);
        if (len == 0) {
            break;
        }

        if (len > 0) {
            JP_THROW(tstrings::any() << "_NSGetExecutablePath() failed");
        }

        buffer.resize(bufferSize);
    } while (true);

    tstring reply = getRealPath(buffer);

    return FileUtils::toAbsolutePath(reply);
}

tstring_array getCommandArgs(CommandArgProgramNameMode progNameMode) {
    tstring_array result;
    const tstring psnArgPrefix = "-psn_";
    for (int i = progNameMode == ExcludeProgramName ? 1 : 0; i < argc; i++) {
        const tstring arg = argv[i];
        if (!tstrings::startsWith(arg, psnArgPrefix)) {
            result.push_back(arg);
        }
    }
    return result;
}

} // end of namespace SysInfo
