/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_OLE_H
#define AWT_OLE_H

#include <ole2.h>
#include <comdef.h>
#include <comutil.h>
#include "awt.h"

#ifdef _DEBUG
    #define _SUN_DEBUG
#endif


#ifndef SUN_DBG_NS
  #ifdef _LIB
    #define SUN_DBG_NS SUN_dbg_lib
  #else
    #define SUN_DBG_NS SUN_dbg_glb
  #endif //_LIB
#endif //SUN_DBG_NS


#ifndef  TRACE_SUFFIX
  #define TRACE_SUFFIX
#endif

namespace SUN_DBG_NS{
  LPCTSTR CreateTimeStamp(LPTSTR lpBuffer, size_t iBufferSize);
  inline void snTraceEmp(LPCTSTR, ...) { }
  void snvTrace(LPCTSTR lpszFormat, va_list argList);
  void snTrace(LPCTSTR lpszFormat, ... );
}//SUN_DBG_NS namespace end

#define STRACE1       SUN_DBG_NS::snTrace
#ifdef _SUN_DEBUG
  #define STRACE      SUN_DBG_NS::snTrace
#else
  #define STRACE      SUN_DBG_NS::snTraceEmp
#endif
#define STRACE0       SUN_DBG_NS::snTraceEmp

struct CLogEntryPoint1 {
    LPCTSTR m_lpTitle;
    CLogEntryPoint1(LPCTSTR lpTitle):m_lpTitle(lpTitle) { STRACE(_T("{%s"), m_lpTitle); }
    ~CLogEntryPoint1(){ STRACE(_T("}%s"), m_lpTitle); }
};
struct CLogEntryPoint0 {
    LPCTSTR m_lpTitle;
    CLogEntryPoint0(LPCTSTR lpTitle):m_lpTitle(lpTitle) { STRACE0(_T("{%s"), m_lpTitle); }
    ~CLogEntryPoint0(){ STRACE0(_T("}%s"), m_lpTitle); }
};

#define SEP1(msg)    CLogEntryPoint1 _ep1_(msg);
#define SEP0(msg)    CLogEntryPoint0 _ep0_(msg);
#ifdef  _SUN_DEBUG
  #define SEP(msg)   CLogEntryPoint1 _ep1_(msg);
#else
  #define SEP(msg)   CLogEntryPoint0 _ep0_(msg);
#endif


#define OLE_BAD_COOKIE ((DWORD)-1)

#define OLE_TRACENOTIMPL(msg)\
        STRACE(_T("Warning:%s"), msg);\
        return E_NOTIMPL;

#define OLE_TRACEOK(msg)\
        STRACE0(_T("Info:%s"), msg);\
        return S_OK;


#define OLE_DECL\
        HRESULT _hr_ = S_OK;

#define OLE_NEXT_TRY\
        try {

#define OLE_TRY\
        OLE_DECL\
        try {

#define OLE_HRT(fnc)\
        _hr_ = fnc;\
        if (FAILED(_hr_)) {\
            STRACE1(_T("Error:%08x in ") _T(#fnc),  _hr_);\
            _com_raise_error(_hr_);\
        }

#define OLE_WINERROR2HR(msg, erCode)\
        _hr_ = erCode;\
        STRACE1(_T("OSError:%d in ") msg,  _hr_);\
        _hr_ = HRESULT_FROM_WIN32(_hr_);

#define OLE_THROW_LASTERROR(msg)\
        OLE_WINERROR2HR(msg, ::GetLastError())\
        _com_raise_error(_hr_);

#define OLE_CHECK_NOTNULL(x)\
        if (!(x)) {\
            STRACE1(_T("Null pointer:") _T(#x));\
            _com_raise_error(_hr_ = E_POINTER);\
        }

#define OLE_CHECK_NOTNULLSP(x)\
        if (!bool(x)) {\
            STRACE1(_T("Null pointer:") _T(#x));\
            _com_raise_error(_hr_ = E_POINTER);\
        }

#define OLE_HRW32(fnc)\
        _hr_ = fnc;\
        if (ERROR_SUCCESS != _hr_) {\
            STRACE1(_T("OSError:%d in ") _T(#fnc),  _hr_);\
            _com_raise_error(_hr_ = HRESULT_FROM_WIN32(_hr_));\
        }

#define OLE_HRW32_BOOL(fnc)\
        if (!fnc) {\
            OLE_THROW_LASTERROR(_T(#fnc))\
        }

#define OLE_CATCH\
        } catch (_com_error &e) {\
            _hr_ = e.Error();\
            STRACE1(_T("COM Error:%08x %s"), _hr_, e.ErrorMessage());\
        }

#define OLE_CATCH_BAD_ALLOC\
        } catch (_com_error &e) {\
            _hr_ = e.Error();\
            STRACE1(_T("COM Error:%08x %s"), _hr_, e.ErrorMessage());\
        } catch (std::bad_alloc&) {\
            _hr_ = E_OUTOFMEMORY;\
            STRACE1(_T("Error: Out of Memory"));\
        }

#define OLE_CATCH_ALL\
        } catch (_com_error &e) {\
            _hr_ = e.Error();\
            STRACE1(_T("COM Error:%08x %s"), _hr_, e.ErrorMessage());\
        } catch(...) {\
            _hr_ = E_FAIL;\
            STRACE1(_T("Error: General Pritection Failor"));\
        }

#define OLE_RETURN_SUCCESS return SUCCEEDED(_hr_);
#define OLE_RETURN_HR      return _hr_;
#define OLE_HR             _hr_

#define _B(x)    _bstr_t(x)
#define _BT(x)    (LPCTSTR)_bstr_t(x)
#define _V(x)    _variant_t(x)
#define _VV(vrt) _variant_t(vrt, false)
#define _VE      _variant_t()
#define _VB(b)   _variant_t(bool(b))

struct OLEHolder
{
    OLEHolder()
    : m_hr(::OleInitialize(NULL))
    {}

    ~OLEHolder(){}
    operator bool() const { return S_OK==SUCCEEDED(m_hr); }
    HRESULT m_hr;
};

#endif//AWT_OLE_H
