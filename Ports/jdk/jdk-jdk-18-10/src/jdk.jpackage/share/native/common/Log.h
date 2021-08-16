/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __LOG_H_INCLUDED_
#define __LOG_H_INCLUDED_

#include "PlatformLogEvent.h"
#include "tstrings.h"


/* Default logger (Logger::defaultLogger()) writes log messages to
 * the default log file.
 * Common scenario:
 *   - main() function configures default logger:
 *       FileLogAppender appender(_T("my_log_filename.log"));
 *       Logger::defaultLogger().setAppender(appender);
 *       Logger::defaultLogger().setLogLevel(LOG_INFO);
 * If the default file name and log level are not set,
 *  _T("jusched.log")/LOG_TRACE are used.
 *
 * Logger fileName specifies only file name,
 * full path for the log file depends on the platform
 * (usually value of the TMP env. var)
 */

class Logger;
class StreamLogAppender;

struct LogEvent: public PlatformLogEvent {
    tstring logLevel;
    tstring fileName;
    int lineNum;
    tstring funcName;
    tstring message;

    LogEvent();

    friend class Logger;
    friend class StreamLogAppender;

private:
    static void init(PlatformLogEvent& logEvent);
    static void appendFormatted(const PlatformLogEvent& logEvent,
            tstring& buffer);
};


class LogAppender {
public:
    virtual ~LogAppender() {
    }
    virtual void append(const LogEvent& v) = 0;
};


class NopLogAppender: public LogAppender {
public:
    virtual void append(const LogEvent& v) {};
};


class TeeLogAppender: public LogAppender {
public:
    TeeLogAppender(LogAppender* first, LogAppender* second):
            first(first), second(second) {
    }
    virtual ~TeeLogAppender() {
    }
    virtual void append(const LogEvent& v) {
        if (first) {
            first->append(v);
        }
        if (second) {
            second->append(v);
        }
    }
private:
    LogAppender* first;
    LogAppender* second;
};


/**
 * Writes log events to the given std::ostream.
 * Supposed to be used with std::cout or std::cerr
 */
class StreamLogAppender: public LogAppender {
public:
    explicit StreamLogAppender(std::ostream& consumer) : consumer(&consumer) {
    }

    virtual void append(const LogEvent& v);

private:
    std::ostream* consumer;
};


class Logger {
public:
    enum LogLevel {
        LOG_TRACE,
        LOG_INFO,
        LOG_WARNING,
        LOG_ERROR
    };

    static Logger& defaultLogger();

    explicit Logger(LogAppender& appender, LogLevel logLevel = LOG_TRACE);
    ~Logger();

    LogAppender& setAppender(LogAppender& v) {
        LogAppender& oldAppender = *appender;
        appender = &v;
        return oldAppender;
    }

    LogAppender& getAppender() const {
        return *appender;
    }

    void setLogLevel(LogLevel logLevel);

    bool isLoggable(LogLevel logLevel) const ;
    void log(LogLevel logLevel, LPCTSTR fileName, int lineNum,
            LPCTSTR funcName, const tstring& message) const;
    void log(LogLevel logLevel, LPCTSTR fileName, int lineNum,
            LPCTSTR funcName, const tstrings::any& message) const {
        return log(logLevel, fileName, lineNum, funcName, message.tstr());
    }
    void log(LogLevel logLevel, LPCTSTR fileName, int lineNum,
            LPCTSTR funcName, tstring::const_pointer message) const {
        return log(logLevel, fileName, lineNum, funcName, tstring(message));
    }

    // internal class for scope tracing
    class ScopeTracer {
    public:
        ScopeTracer(Logger &logger, LogLevel logLevel, LPCTSTR fileName,
                int lineNum, LPCTSTR funcName, const tstring& scopeName);
        ~ScopeTracer();

    private:
        const Logger &log;
        const LogLevel level;
        const tstring file;
        const int line;
        const tstring func;
        const tstring scope;
        const bool needLog;
    };

private:
    static void initializingLogging();
    static void initializeLogging();

private:
    LogLevel level;
    LogAppender* appender;
};


class WithExtraLogAppender {
public:
    WithExtraLogAppender(LogAppender& logAppender):
            oldLogAppender(Logger::defaultLogger().getAppender()),
            newLogAppender(&Logger::defaultLogger().getAppender(),
                    &logAppender) {
        Logger::defaultLogger().setAppender(newLogAppender);
    }

    virtual ~WithExtraLogAppender() {
        Logger::defaultLogger().setAppender(oldLogAppender);
    }

private:
    LogAppender& oldLogAppender;
    TeeLogAppender newLogAppender;
};


// base logging macro
#define LOGGER_LOG(logger, logLevel, message) \
    do { \
        if (logger.isLoggable(logLevel)) { \
            logger.log(logLevel, _T(__FILE__), __LINE__, _T(__FUNCTION__), message); \
        } \
    } while(false)


// custom logger macros
#define LOGGER_TRACE(logger, message)   LOGGER_LOG(logger, Logger::LOG_TRACE, message)
#define LOGGER_INFO(logger, message)    LOGGER_LOG(logger, Logger::LOG_INFO, message)
#define LOGGER_WARNING(logger, message) LOGGER_LOG(logger, Logger::LOG_WARNING, message)
#define LOGGER_ERROR(logger, message)   LOGGER_LOG(logger, Logger::LOG_ERROR, message)
// scope tracing macros
#define LOGGER_TRACE_SCOPE(logger, scopeName) \
    Logger::ScopeTracer tracer__COUNTER__(logger, Logger::LOG_TRACE, _T(__FILE__), __LINE__, _T(__FUNCTION__), scopeName)
#define LOGGER_TRACE_FUNCTION(logger)   LOGGER_TRACE_SCOPE(logger, _T(__FUNCTION__))


// default logger macros
#define LOG_TRACE(message)              LOGGER_LOG(Logger::defaultLogger(), Logger::LOG_TRACE, message)
#define LOG_INFO(message)               LOGGER_LOG(Logger::defaultLogger(), Logger::LOG_INFO, message)
#define LOG_WARNING(message)            LOGGER_LOG(Logger::defaultLogger(), Logger::LOG_WARNING, message)
#define LOG_ERROR(message)              LOGGER_LOG(Logger::defaultLogger(), Logger::LOG_ERROR, message)
// scope tracing macros
// logs (_T("Entering ") + scopeName) at the beging, (_T("Exiting ") + scopeName) at the end of scope
#define LOG_TRACE_SCOPE(scopeName)      LOGGER_TRACE_SCOPE(Logger::defaultLogger(), scopeName)
// logs (_T("Entering ") + functionName) at the beging, (_T("Exiting ") + __FUNCTION__) at the end of scope
#define LOG_TRACE_FUNCTION()            LOGGER_TRACE_FUNCTION(Logger::defaultLogger())


#endif // __LOG_H_INCLUDED_
