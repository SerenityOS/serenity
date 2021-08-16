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

#include <stdio.h>
#include <stdlib.h>
#include "Executor.h"
#include "Log.h"
#include "ErrorHandling.h"


int executeCommandLineAndReadStdout(const std::string& cmd,
        CommandOutputConsumer& consumer) {
    FILE * stream = popen(cmd.c_str(), "r");
    if (!stream) {
        JP_THROW(tstrings::any() << "popen(" << cmd
                << ") failed. Error: " << lastCRTError());
    }

    LOG_TRACE(tstrings::any() << "Reading output of [" << cmd << "] command");

    try {
        bool useConsumer = true;
        std::string buf;
        for (;;) {
            const int c = fgetc(stream);
            if(c == EOF) {
                if (useConsumer && !buf.empty()) {
                    LOG_TRACE(tstrings::any() << "Next line: [" << buf << "]");
                    consumer.accept(buf);
                }
                break;
            }

            if (c == '\n' && useConsumer) {
                LOG_TRACE(tstrings::any() << "Next line: [" << buf << "]");
                useConsumer = !consumer.accept(buf);
                buf.clear();
            } else {
                buf.push_back(static_cast<char>(c));
            }
        }
        return pclose(stream);
    } catch (...) {
        if (stream) {
            pclose(stream);
        }
        throw;
    }
}
