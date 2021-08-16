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

#include <memory>
#include "WinApp.h"
#include "Log.h"
#include "SysInfo.h"
#include "FileUtils.h"
#include "ErrorHandling.h"


// MessageBox
#pragma comment(lib, "user32")


namespace {

class LastErrorGuiLogAppender : public LogAppender {
public:
    virtual void append(const LogEvent& v) {
        JP_TRY;

        const std::wstring msg = (tstrings::any()
                << app::lastErrorMsg()).wstr();
        MessageBox(0, msg.c_str(),
            FileUtils::basename(SysInfo::getProcessModulePath()).c_str(),
            MB_ICONERROR | MB_OK);

        JP_CATCH_ALL;
    }
};


class Console {
public:
    Console() {
        if (!AttachConsole(ATTACH_PARENT_PROCESS)) {
            // Failed to connect to parent's console. Create our own.
            if (!AllocConsole()) {
                // We already have a console, no need to redirect std I/O.
                return;
            }
        }

        stdoutChannel = std::unique_ptr<Channel>(new Channel(stdout));
        stderrChannel = std::unique_ptr<Channel>(new Channel(stderr));
    }

    struct FileCloser {
        typedef FILE* pointer;

        void operator()(pointer h) {
            ::fclose(h);
        }
    };

    typedef std::unique_ptr<
        FileCloser::pointer,
        FileCloser
    > UniqueFILEHandle;

private:
    class Channel {
    public:
        Channel(FILE* stdFILEHandle): stdFILEHandle(stdFILEHandle) {
            const char* stdFileName = "CONOUT$";
            const char* openMode = "w";
            if (stdFILEHandle == stdin) {
                stdFileName = "CONIN$";
                openMode = "r";
            }

            FILE* fp = 0;
            freopen_s(&fp, stdFileName, openMode, stdFILEHandle);

            fileHandle = UniqueFILEHandle(fp);

            std::ios_base::sync_with_stdio();
        }

        virtual ~Channel() {
            JP_TRY;

            FILE* fp = 0;
            fileHandle = UniqueFILEHandle(fp);
            std::ios_base::sync_with_stdio();

            JP_CATCH_ALL;
        }

    private:
        UniqueFILEHandle fileHandle;
        FILE *stdFILEHandle;
    };

    std::unique_ptr<Channel> stdoutChannel;
    std::unique_ptr<Channel> stderrChannel;
};

} // namespace


namespace app {
int wlaunch(const std::nothrow_t&, LauncherFunc func) {
    std::unique_ptr<Console> console;
    JP_TRY;
    if (app::isWithLogging()) {
        console = std::unique_ptr<Console>(new Console());
    }
    JP_CATCH_ALL;

    LastErrorGuiLogAppender lastErrorLogAppender;
    TeeLogAppender logAppender(&app::defaultLastErrorLogAppender(),
            &lastErrorLogAppender);
    return app::launch(std::nothrow, func, &logAppender);
}
} // namespace app
