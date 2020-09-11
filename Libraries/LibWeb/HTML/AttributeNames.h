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

#define ENUMERATE_HTML_ATTRIBUTES               \
    __ENUMERATE_HTML_ATTRIBUTE(abbr)            \
    __ENUMERATE_HTML_ATTRIBUTE(accept)          \
    __ENUMERATE_HTML_ATTRIBUTE(accept_charset)  \
    __ENUMERATE_HTML_ATTRIBUTE(action)          \
    __ENUMERATE_HTML_ATTRIBUTE(align)           \
    __ENUMERATE_HTML_ATTRIBUTE(allow)           \
    __ENUMERATE_HTML_ATTRIBUTE(alt)             \
    __ENUMERATE_HTML_ATTRIBUTE(async)           \
    __ENUMERATE_HTML_ATTRIBUTE(behaviour)       \
    __ENUMERATE_HTML_ATTRIBUTE(bgcolor)         \
    __ENUMERATE_HTML_ATTRIBUTE(checked)         \
    __ENUMERATE_HTML_ATTRIBUTE(cite)            \
    __ENUMERATE_HTML_ATTRIBUTE(class_)          \
    __ENUMERATE_HTML_ATTRIBUTE(cols)            \
    __ENUMERATE_HTML_ATTRIBUTE(colspan)         \
    __ENUMERATE_HTML_ATTRIBUTE(content)         \
    __ENUMERATE_HTML_ATTRIBUTE(contenteditable) \
    __ENUMERATE_HTML_ATTRIBUTE(data)            \
    __ENUMERATE_HTML_ATTRIBUTE(datetime)        \
    __ENUMERATE_HTML_ATTRIBUTE(disabled)        \
    __ENUMERATE_HTML_ATTRIBUTE(download)        \
    __ENUMERATE_HTML_ATTRIBUTE(defer)           \
    __ENUMERATE_HTML_ATTRIBUTE(direction)       \
    __ENUMERATE_HTML_ATTRIBUTE(dirname)         \
    __ENUMERATE_HTML_ATTRIBUTE(for_)            \
    __ENUMERATE_HTML_ATTRIBUTE(frameborder)     \
    __ENUMERATE_HTML_ATTRIBUTE(headers)         \
    __ENUMERATE_HTML_ATTRIBUTE(height)          \
    __ENUMERATE_HTML_ATTRIBUTE(href)            \
    __ENUMERATE_HTML_ATTRIBUTE(hreflang)        \
    __ENUMERATE_HTML_ATTRIBUTE(http_equiv)      \
    __ENUMERATE_HTML_ATTRIBUTE(id)              \
    __ENUMERATE_HTML_ATTRIBUTE(imagesizes)      \
    __ENUMERATE_HTML_ATTRIBUTE(imagesrcset)     \
    __ENUMERATE_HTML_ATTRIBUTE(integrity)       \
    __ENUMERATE_HTML_ATTRIBUTE(label)           \
    __ENUMERATE_HTML_ATTRIBUTE(lang)            \
    __ENUMERATE_HTML_ATTRIBUTE(longdesc)        \
    __ENUMERATE_HTML_ATTRIBUTE(max)             \
    __ENUMERATE_HTML_ATTRIBUTE(media)           \
    __ENUMERATE_HTML_ATTRIBUTE(method)          \
    __ENUMERATE_HTML_ATTRIBUTE(min)             \
    __ENUMERATE_HTML_ATTRIBUTE(name)            \
    __ENUMERATE_HTML_ATTRIBUTE(pattern)         \
    __ENUMERATE_HTML_ATTRIBUTE(ping)            \
    __ENUMERATE_HTML_ATTRIBUTE(placeholder)     \
    __ENUMERATE_HTML_ATTRIBUTE(poster)          \
    __ENUMERATE_HTML_ATTRIBUTE(rel)             \
    __ENUMERATE_HTML_ATTRIBUTE(rows)            \
    __ENUMERATE_HTML_ATTRIBUTE(scrolling)       \
    __ENUMERATE_HTML_ATTRIBUTE(size)            \
    __ENUMERATE_HTML_ATTRIBUTE(sizes)           \
    __ENUMERATE_HTML_ATTRIBUTE(src)             \
    __ENUMERATE_HTML_ATTRIBUTE(srcdoc)          \
    __ENUMERATE_HTML_ATTRIBUTE(srclang)         \
    __ENUMERATE_HTML_ATTRIBUTE(srcset)          \
    __ENUMERATE_HTML_ATTRIBUTE(step)            \
    __ENUMERATE_HTML_ATTRIBUTE(style)           \
    __ENUMERATE_HTML_ATTRIBUTE(target)          \
    __ENUMERATE_HTML_ATTRIBUTE(title)           \
    __ENUMERATE_HTML_ATTRIBUTE(type)            \
    __ENUMERATE_HTML_ATTRIBUTE(usemap)          \
    __ENUMERATE_HTML_ATTRIBUTE(value)           \
    __ENUMERATE_HTML_ATTRIBUTE(width)           \
    __ENUMERATE_HTML_ATTRIBUTE(wrap)

#define __ENUMERATE_HTML_ATTRIBUTE(name) extern FlyString name;
ENUMERATE_HTML_ATTRIBUTES
#undef __ENUMERATE_HTML_ATTRIBUTE

}
}
}
