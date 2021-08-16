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

#include "kludge_c++11.h"

#include <memory>
#include "app.h"
#include "Log.h"
#include "SysInfo.h"
#include "ErrorHandling.h"


namespace {
const std::string* theLastErrorMsg = 0;

NopLogAppender nopLogAppender;

class StandardLogAppender : public LogAppender {
public:
    virtual void append(const LogEvent& v) {
        std::cerr << "[" << v.logLevel << "] "
            << v.fileName
            << ":" << v.lineNum
            << ": " << v.message
            << std::endl;
    }
} standardLogAppender;

class LastErrorLogAppender : public LogAppender {
public:
    virtual void append(const LogEvent& v) {
        std::cerr << app::lastErrorMsg() << std::endl;
    }
} lastErrorLogAppender;


class ResetLastErrorMsgAtEndOfScope {
public:
    ResetLastErrorMsgAtEndOfScope() {
    }
    ~ResetLastErrorMsgAtEndOfScope() {
        JP_NO_THROW(theLastErrorMsg = 0);
    }
};

class SetLoggerAtEndOfScope {
public:
    SetLoggerAtEndOfScope(
            std::unique_ptr<WithExtraLogAppender>& withLogAppender,
            LogAppender* lastErrorLogAppender):
                withLogAppender(withLogAppender),
                lastErrorLogAppender(lastErrorLogAppender) {
    }

    ~SetLoggerAtEndOfScope() {
        JP_TRY;
        std::unique_ptr<WithExtraLogAppender> other(
                new WithExtraLogAppender(*lastErrorLogAppender));
        withLogAppender.swap(other);
        JP_CATCH_ALL;
    }

private:
    std::unique_ptr<WithExtraLogAppender>& withLogAppender;
    LogAppender* lastErrorLogAppender;
};

} // namespace


namespace app {
LogAppender& defaultLastErrorLogAppender() {
    return lastErrorLogAppender;
}


std::string lastErrorMsg() {
    if (theLastErrorMsg) {
        return *theLastErrorMsg;
    }
    return "";
}


bool isWithLogging() {
    // If JPACKAGE_DEBUG environment variable is set to "true"
    // logging is enabled.
    return SysInfo::getEnvVariable(
            std::nothrow, _T("JPACKAGE_DEBUG")) == _T("true");
}


int launch(const std::nothrow_t&,
        LauncherFunc func, LogAppender* lastErrorLogAppender) {
    if (isWithLogging()) {
        Logger::defaultLogger().setAppender(standardLogAppender);
    } else {
        Logger::defaultLogger().setAppender(nopLogAppender);
    }

    LOG_TRACE_FUNCTION();

    if (!lastErrorLogAppender) {
        lastErrorLogAppender = &defaultLastErrorLogAppender();
    }
    std::unique_ptr<WithExtraLogAppender> withLogAppender;
    std::string errorMsg;
    const ResetLastErrorMsgAtEndOfScope resetLastErrorMsg;

    JP_TRY;

    // This will temporary change log appenders of the default logger
    // to save log messages in the default and additional log appenders.
    // Log appenders config of the default logger will be restored to
    // the original state at function exit automatically.
    const SetLoggerAtEndOfScope setLogger(withLogAppender, lastErrorLogAppender);
    func();
    return 0;

    // The point of all these redefines is to save the last raw error message in
    // 'theLastErrorMsg' variable.
    // By default error messages are saved in exception instances with the details
    // of error origin (source file, function name, line number).
    // We don't want these details in user error messages. However we still want to
    // save full information about the last error in the default log appender.
#undef JP_HANDLE_ERROR
#undef JP_HANDLE_UNKNOWN_ERROR
#undef JP_CATCH_EXCEPTIONS
#define JP_HANDLE_ERROR(e) \
    do { \
        errorMsg = (tstrings::any() << e.what()).str(); \
        theLastErrorMsg = &errorMsg; \
        reportError(JP_SOURCE_CODE_POS, e); \
    } while(0)
#define JP_HANDLE_UNKNOWN_ERROR \
    do { \
        errorMsg = "Unknown error"; \
        theLastErrorMsg = &errorMsg; \
        reportUnknownError(JP_SOURCE_CODE_POS); \
    } while(0)
#define JP_CATCH_EXCEPTIONS \
    catch (const JpErrorBase& e) { \
        errorMsg = (tstrings::any() << e.rawMessage()).str(); \
        theLastErrorMsg = &errorMsg; \
        try { \
            throw; \
        } catch (const std::runtime_error& e) { \
            reportError(JP_SOURCE_CODE_POS, e); \
        } \
    } catch (const std::runtime_error& e) { \
        errorMsg = lastCRTError(); \
        theLastErrorMsg = &errorMsg; \
        reportError(JP_SOURCE_CODE_POS, e); \
    } \
    JP_CATCH_UNKNOWN_EXCEPTION

    JP_CATCH_ALL;

#undef JP_HANDLE_ERROR
#undef JP_HANDLE_UNKNOWN_ERROR
#undef JP_CATCH_EXCEPTIONS
#define JP_HANDLE_ERROR(e)      JP_REPORT_ERROR(e)
#define JP_HANDLE_UNKNOWN_ERROR JP_REPORT_UNKNOWN_ERROR
#define JP_CATCH_EXCEPTIONS     JP_DEFAULT_CATCH_EXCEPTIONS

    return 1;
}
} // namespace app
