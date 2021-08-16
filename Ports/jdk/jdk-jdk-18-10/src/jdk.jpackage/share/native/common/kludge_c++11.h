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


#ifndef kludge_cxx11_h
#define kludge_cxx11_h

//
// This file contains kludge implementation of C++11 features needed to build
// jpackage until Open JDK moves forward from C++98 standard.
//

#ifdef __GNUG__
#ifndef __clang__
#if __cplusplus < 201103L
#define JP_WITH_KLUDGE_CXX11
#endif
#endif
#endif

#ifdef JP_WITH_KLUDGE_CXX11

#include <algorithm>


namespace std {

namespace impl {

template <typename Tp, typename Dp>
class unique_ptr_impl {
public:
    typedef typename Dp::pointer pointer;
    typedef Tp element_type;
    typedef Dp deleter_type;

    unique_ptr_impl(): value(0) {
    }

    unique_ptr_impl(pointer p): value(p) {
    }

    pointer release() {
        const pointer retValue = value;
        value = 0;
        return retValue;
    }

    void swap(unique_ptr_impl& other) {
        std::swap(value, other.value);
    }

    pointer get() const {
        return value;
    }

private:
    unique_ptr_impl(const unique_ptr_impl&);
    unique_ptr_impl& operator= (const unique_ptr_impl&);

private:
    pointer value;
};

} // namespace impl


template <typename Tp>
struct default_delete {
    typedef Tp* pointer;

    void operator()(Tp* ptr) const {
        delete ptr;
    }
};


template <typename Tp, typename Dp = default_delete<Tp> >
class unique_ptr {
    typedef impl::unique_ptr_impl<Tp, Dp> impl_type;
public:
    typedef typename impl_type::pointer pointer;
    typedef typename impl_type::element_type element_type;
    typedef typename impl_type::deleter_type deleter_type;

    unique_ptr() {
    }

    unique_ptr(pointer p): impl(p) {
    }

    ~unique_ptr() {
        if (get() != 0) {
            impl_type tmp;
            tmp.swap(impl);
            Dp()(tmp.get());
        }
    }

    pointer release() {
        return impl.release();
    }

    void swap(unique_ptr& other) {
        impl.swap(other.impl);
    }

    pointer get() const {
        return impl.get();
    }

    element_type& operator *() const {
        return *impl.get();
    }

    pointer operator ->() const {
        return impl.get();
    }

private:
    impl_type impl;
};

template <class Ctnr>
typename Ctnr::const_iterator begin(const Ctnr& ctnr) {
    return ctnr.begin();
}

template <class Ctnr>
typename Ctnr::iterator begin(Ctnr& ctnr) {
    return ctnr.begin();
}

template <class Ctnr>
typename Ctnr::const_iterator end(const Ctnr& ctnr) {
    return ctnr.end();
}

template <class Ctnr>
typename Ctnr::iterator end(Ctnr& ctnr) {
    return ctnr.end();
}

} // namespace std

#endif // #ifdef JP_WITH_KLUDGE_CXX11

#endif // #ifndef kludge_cxx11_h
