/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef EXECUTOR_H
#define EXECUTOR_H

#include "tstrings.h"
#include "UniqueHandle.h"


class Executor {
public:
    explicit Executor(const std::wstring& appPath=std::wstring()) {
        app(appPath).visible(false);
    }

    /**
     * Returns command line configured with arg() calls so far.
     */
    std::wstring args() const;

    /**
     * Set path to application to execute.
     */
    Executor& app(const std::wstring& v) {
        appPath = v;
        return *this;
    }

    /**
     * Adds another command line argument.
     */
    Executor& arg(const std::wstring& v) {
        argsArray.push_back(v);
        return *this;
    }

    /**
     * Controls if application window should be visible.
     */
    Executor& visible(bool v) {
        theVisible = v;
        return *this;
    }

    /**
     * Starts application process and blocks waiting when the started
     * process terminates.
     * Returns process exit code.
     * Throws exception if process start failed.
     */
    int execAndWaitForExit() const;

private:
    UniqueHandle startProcess() const;

    bool theVisible;
    tstring_array argsArray;
    std::wstring appPath;
};

#endif // #ifndef EXECUTOR_H
