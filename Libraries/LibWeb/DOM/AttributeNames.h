/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/FlyString.h>

namespace Web {
namespace HTML {
namespace AttributeNames {

void initialize();

#define ENUMERATE_HTML_ATTRIBUTES      \
    __ENUMERATE_HTML_ATTRIBUTE(id)     \
    __ENUMERATE_HTML_ATTRIBUTE(class_) \
    __ENUMERATE_HTML_ATTRIBUTE(type)   \
    __ENUMERATE_HTML_ATTRIBUTE(href)   \
    __ENUMERATE_HTML_ATTRIBUTE(style)  \
    __ENUMERATE_HTML_ATTRIBUTE(name)   \
    __ENUMERATE_HTML_ATTRIBUTE(target) \
    __ENUMERATE_HTML_ATTRIBUTE(width)  \
    __ENUMERATE_HTML_ATTRIBUTE(height) \
    __ENUMERATE_HTML_ATTRIBUTE(title)  \
    __ENUMERATE_HTML_ATTRIBUTE(action) \
    __ENUMERATE_HTML_ATTRIBUTE(method) \
    __ENUMERATE_HTML_ATTRIBUTE(alt)    \
    __ENUMERATE_HTML_ATTRIBUTE(src)    \
    __ENUMERATE_HTML_ATTRIBUTE(value)  \
    __ENUMERATE_HTML_ATTRIBUTE(rel)    \
    __ENUMERATE_HTML_ATTRIBUTE(async)  \
    __ENUMERATE_HTML_ATTRIBUTE(defer)  \
    __ENUMERATE_HTML_ATTRIBUTE(size)

#define __ENUMERATE_HTML_ATTRIBUTE(name) extern FlyString name;
ENUMERATE_HTML_ATTRIBUTES
#undef __ENUMERATE_HTML_ATTRIBUTE

}
}
}
