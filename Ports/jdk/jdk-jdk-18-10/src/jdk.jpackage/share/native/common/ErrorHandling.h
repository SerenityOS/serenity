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

#ifndef ErrorHandling_h
#define ErrorHandling_h


#include <stdexcept>

#include "SourceCodePos.h"
#include "tstrings.h"


//
// Exception handling helpers. Allow transparent exception logging.
// Use as follows:
//
//  void foo () {
//      JP_TRY;
//
//      if (!do_something()) {
//          JP_THROW("do_something() failed");
//      }
//
//      JP_CATCH_ALL;
//  }
//


// Logs std::exception caught at 'pos'.
void reportError(const SourceCodePos& pos, const std::runtime_error& e);
// Logs unknown exception caught at 'pos'.
// Assumed to be called from catch (...) {}
void reportUnknownError(const SourceCodePos& pos);

std::string makeMessage(const std::runtime_error& e, const SourceCodePos& pos);

std::string joinErrorMessages(const std::string& a, const std::string& b);

std::string lastCRTError();


class JpErrorBase {
public:
    virtual const char* rawMessage() const throw() = 0;
};


template <class Base>
class JpError: public JpErrorBase, public Base {
public:
    JpError(const Base& e, const SourceCodePos& pos):
                                        Base(e), msg(::makeMessage(e, pos)) {
    }

    ~JpError() throw() {
    }

    // override Base::what()
    const char* what() const throw() {
        return msg.c_str();
    }

    // override JpErrorBase
    const char* rawMessage() const throw() {
        return Base::what();
    }
private:
    // Assert Base is derived from std::runtime_error
    enum { isDerivedFromStdException =
                        sizeof(static_cast<std::runtime_error*>((Base*)0)) };

    std::string msg;
};

template <class T>
inline JpError<T> makeException(const T& obj, const SourceCodePos& p) {
    return JpError<T>(obj, p);
}

inline JpError<std::runtime_error> makeException(
                            const std::string& msg, const SourceCodePos& p) {
    return JpError<std::runtime_error>(std::runtime_error(msg), p);
}

inline JpError<std::runtime_error> makeException(
                        const tstrings::any& msg, const SourceCodePos& p) {
    return makeException(msg.str(), p);
}

inline JpError<std::runtime_error> makeException(
                    std::string::const_pointer msg, const SourceCodePos& p) {
    return makeException(std::string(msg), p);
}


#define JP_REPORT_ERROR(e)          reportError(JP_SOURCE_CODE_POS, e)
#define JP_REPORT_UNKNOWN_ERROR     reportUnknownError(JP_SOURCE_CODE_POS)

// Redefine locally in cpp file(s) if need more handling than just reporting
#define JP_HANDLE_ERROR(e)          JP_REPORT_ERROR(e)
#define JP_HANDLE_UNKNOWN_ERROR     JP_REPORT_UNKNOWN_ERROR


#define JP_TRY                              \
        try                                 \
        {                                   \
            do {} while(0)

#define JP_DEFAULT_CATCH_EXCEPTIONS         \
        JP_CATCH_STD_EXCEPTION              \
        JP_CATCH_UNKNOWN_EXCEPTION

#define JP_CATCH_EXCEPTIONS                 \
        JP_DEFAULT_CATCH_EXCEPTIONS

#define JP_CATCH_ALL                        \
        }                                   \
        JP_CATCH_EXCEPTIONS                 \
        do {} while(0)

#define JP_CATCH_STD_EXCEPTION              \
        catch (const std::runtime_error& e) \
        {                                   \
            JP_HANDLE_ERROR(e);             \
        }

#define JP_CATCH_UNKNOWN_EXCEPTION          \
        catch (...)                         \
        {                                   \
            JP_HANDLE_UNKNOWN_ERROR;        \
        }


#define JP_THROW(e) throw makeException((e), JP_SOURCE_CODE_POS)

#define JP_NO_THROW(expr) \
    JP_TRY; \
    expr; \
    JP_CATCH_ALL

#endif // #ifndef ErrorHandling_h
