/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web {
namespace HTML {
namespace AttributeNames {

#define ENUMERATE_HTML_ATTRIBUTES                          \
    __ENUMERATE_HTML_ATTRIBUTE(abbr)                       \
    __ENUMERATE_HTML_ATTRIBUTE(accept)                     \
    __ENUMERATE_HTML_ATTRIBUTE(accept_charset)             \
    __ENUMERATE_HTML_ATTRIBUTE(action)                     \
    __ENUMERATE_HTML_ATTRIBUTE(align)                      \
    __ENUMERATE_HTML_ATTRIBUTE(alink)                      \
    __ENUMERATE_HTML_ATTRIBUTE(allow)                      \
    __ENUMERATE_HTML_ATTRIBUTE(allowfullscreen)            \
    __ENUMERATE_HTML_ATTRIBUTE(alt)                        \
    __ENUMERATE_HTML_ATTRIBUTE(archive)                    \
    __ENUMERATE_HTML_ATTRIBUTE(async)                      \
    __ENUMERATE_HTML_ATTRIBUTE(autoplay)                   \
    __ENUMERATE_HTML_ATTRIBUTE(axis)                       \
    __ENUMERATE_HTML_ATTRIBUTE(background)                 \
    __ENUMERATE_HTML_ATTRIBUTE(behavior)                   \
    __ENUMERATE_HTML_ATTRIBUTE(bgcolor)                    \
    __ENUMERATE_HTML_ATTRIBUTE(border)                     \
    __ENUMERATE_HTML_ATTRIBUTE(cellpadding)                \
    __ENUMERATE_HTML_ATTRIBUTE(cellspacing)                \
    __ENUMERATE_HTML_ATTRIBUTE(char_)                      \
    __ENUMERATE_HTML_ATTRIBUTE(charoff)                    \
    __ENUMERATE_HTML_ATTRIBUTE(charset)                    \
    __ENUMERATE_HTML_ATTRIBUTE(checked)                    \
    __ENUMERATE_HTML_ATTRIBUTE(cite)                       \
    __ENUMERATE_HTML_ATTRIBUTE(class_)                     \
    __ENUMERATE_HTML_ATTRIBUTE(clear)                      \
    __ENUMERATE_HTML_ATTRIBUTE(code)                       \
    __ENUMERATE_HTML_ATTRIBUTE(codetype)                   \
    __ENUMERATE_HTML_ATTRIBUTE(color)                      \
    __ENUMERATE_HTML_ATTRIBUTE(cols)                       \
    __ENUMERATE_HTML_ATTRIBUTE(colspan)                    \
    __ENUMERATE_HTML_ATTRIBUTE(compact)                    \
    __ENUMERATE_HTML_ATTRIBUTE(content)                    \
    __ENUMERATE_HTML_ATTRIBUTE(contenteditable)            \
    __ENUMERATE_HTML_ATTRIBUTE(controls)                   \
    __ENUMERATE_HTML_ATTRIBUTE(coords)                     \
    __ENUMERATE_HTML_ATTRIBUTE(data)                       \
    __ENUMERATE_HTML_ATTRIBUTE(datetime)                   \
    __ENUMERATE_HTML_ATTRIBUTE(declare)                    \
    __ENUMERATE_HTML_ATTRIBUTE(default_)                   \
    __ENUMERATE_HTML_ATTRIBUTE(defer)                      \
    __ENUMERATE_HTML_ATTRIBUTE(direction)                  \
    __ENUMERATE_HTML_ATTRIBUTE(dirname)                    \
    __ENUMERATE_HTML_ATTRIBUTE(disabled)                   \
    __ENUMERATE_HTML_ATTRIBUTE(download)                   \
    __ENUMERATE_HTML_ATTRIBUTE(event)                      \
    __ENUMERATE_HTML_ATTRIBUTE(face)                       \
    __ENUMERATE_HTML_ATTRIBUTE(for_)                       \
    __ENUMERATE_HTML_ATTRIBUTE(form)                       \
    __ENUMERATE_HTML_ATTRIBUTE(formnovalidate)             \
    __ENUMERATE_HTML_ATTRIBUTE(formtarget)                 \
    __ENUMERATE_HTML_ATTRIBUTE(frame)                      \
    __ENUMERATE_HTML_ATTRIBUTE(frameborder)                \
    __ENUMERATE_HTML_ATTRIBUTE(headers)                    \
    __ENUMERATE_HTML_ATTRIBUTE(height)                     \
    __ENUMERATE_HTML_ATTRIBUTE(hidden)                     \
    __ENUMERATE_HTML_ATTRIBUTE(href)                       \
    __ENUMERATE_HTML_ATTRIBUTE(hreflang)                   \
    __ENUMERATE_HTML_ATTRIBUTE(hspace)                     \
    __ENUMERATE_HTML_ATTRIBUTE(http_equiv)                 \
    __ENUMERATE_HTML_ATTRIBUTE(id)                         \
    __ENUMERATE_HTML_ATTRIBUTE(imagesizes)                 \
    __ENUMERATE_HTML_ATTRIBUTE(imagesrcset)                \
    __ENUMERATE_HTML_ATTRIBUTE(integrity)                  \
    __ENUMERATE_HTML_ATTRIBUTE(ismap)                      \
    __ENUMERATE_HTML_ATTRIBUTE(label)                      \
    __ENUMERATE_HTML_ATTRIBUTE(lang)                       \
    __ENUMERATE_HTML_ATTRIBUTE(language)                   \
    __ENUMERATE_HTML_ATTRIBUTE(link)                       \
    __ENUMERATE_HTML_ATTRIBUTE(longdesc)                   \
    __ENUMERATE_HTML_ATTRIBUTE(loop)                       \
    __ENUMERATE_HTML_ATTRIBUTE(marginheight)               \
    __ENUMERATE_HTML_ATTRIBUTE(marginwidth)                \
    __ENUMERATE_HTML_ATTRIBUTE(max)                        \
    __ENUMERATE_HTML_ATTRIBUTE(media)                      \
    __ENUMERATE_HTML_ATTRIBUTE(method)                     \
    __ENUMERATE_HTML_ATTRIBUTE(min)                        \
    __ENUMERATE_HTML_ATTRIBUTE(multiple)                   \
    __ENUMERATE_HTML_ATTRIBUTE(name)                       \
    __ENUMERATE_HTML_ATTRIBUTE(nohref)                     \
    __ENUMERATE_HTML_ATTRIBUTE(nomodule)                   \
    __ENUMERATE_HTML_ATTRIBUTE(noshade)                    \
    __ENUMERATE_HTML_ATTRIBUTE(novalidate)                 \
    __ENUMERATE_HTML_ATTRIBUTE(nowrap)                     \
    __ENUMERATE_HTML_ATTRIBUTE(onabort)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onauxclick)                 \
    __ENUMERATE_HTML_ATTRIBUTE(onblur)                     \
    __ENUMERATE_HTML_ATTRIBUTE(oncancel)                   \
    __ENUMERATE_HTML_ATTRIBUTE(oncanplay)                  \
    __ENUMERATE_HTML_ATTRIBUTE(oncanplaythrough)           \
    __ENUMERATE_HTML_ATTRIBUTE(onchange)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onclick)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onclose)                    \
    __ENUMERATE_HTML_ATTRIBUTE(oncontextmenu)              \
    __ENUMERATE_HTML_ATTRIBUTE(oncuechange)                \
    __ENUMERATE_HTML_ATTRIBUTE(ondblclick)                 \
    __ENUMERATE_HTML_ATTRIBUTE(ondrag)                     \
    __ENUMERATE_HTML_ATTRIBUTE(ondragend)                  \
    __ENUMERATE_HTML_ATTRIBUTE(ondragenter)                \
    __ENUMERATE_HTML_ATTRIBUTE(ondragleave)                \
    __ENUMERATE_HTML_ATTRIBUTE(ondragover)                 \
    __ENUMERATE_HTML_ATTRIBUTE(ondragstart)                \
    __ENUMERATE_HTML_ATTRIBUTE(ondrop)                     \
    __ENUMERATE_HTML_ATTRIBUTE(ondurationchange)           \
    __ENUMERATE_HTML_ATTRIBUTE(onemptied)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onended)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onerror)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onfocus)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onformdata)                 \
    __ENUMERATE_HTML_ATTRIBUTE(oninput)                    \
    __ENUMERATE_HTML_ATTRIBUTE(oninvalid)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onkeydown)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onkeypress)                 \
    __ENUMERATE_HTML_ATTRIBUTE(onkeyup)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onload)                     \
    __ENUMERATE_HTML_ATTRIBUTE(onloadeddata)               \
    __ENUMERATE_HTML_ATTRIBUTE(onloadedmetadata)           \
    __ENUMERATE_HTML_ATTRIBUTE(onloadstart)                \
    __ENUMERATE_HTML_ATTRIBUTE(onmousedown)                \
    __ENUMERATE_HTML_ATTRIBUTE(onmouseenter)               \
    __ENUMERATE_HTML_ATTRIBUTE(onmouseleave)               \
    __ENUMERATE_HTML_ATTRIBUTE(onmousemove)                \
    __ENUMERATE_HTML_ATTRIBUTE(onmouseout)                 \
    __ENUMERATE_HTML_ATTRIBUTE(onmouseover)                \
    __ENUMERATE_HTML_ATTRIBUTE(onmouseup)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onpause)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onplay)                     \
    __ENUMERATE_HTML_ATTRIBUTE(onplaying)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onprogress)                 \
    __ENUMERATE_HTML_ATTRIBUTE(onratechange)               \
    __ENUMERATE_HTML_ATTRIBUTE(onreset)                    \
    __ENUMERATE_HTML_ATTRIBUTE(onresize)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onscroll)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onsecuritypolicyviolation)  \
    __ENUMERATE_HTML_ATTRIBUTE(onseeked)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onseeking)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onselect)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onslotchange)               \
    __ENUMERATE_HTML_ATTRIBUTE(onstalled)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onsubmit)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onsuspend)                  \
    __ENUMERATE_HTML_ATTRIBUTE(ontimeupdate)               \
    __ENUMERATE_HTML_ATTRIBUTE(ontoggle)                   \
    __ENUMERATE_HTML_ATTRIBUTE(onvolumechange)             \
    __ENUMERATE_HTML_ATTRIBUTE(onwaiting)                  \
    __ENUMERATE_HTML_ATTRIBUTE(onwebkitanimationend)       \
    __ENUMERATE_HTML_ATTRIBUTE(onwebkitanimationiteration) \
    __ENUMERATE_HTML_ATTRIBUTE(onwebkitanimationstart)     \
    __ENUMERATE_HTML_ATTRIBUTE(onwebkittransitionend)      \
    __ENUMERATE_HTML_ATTRIBUTE(onwheel)                    \
    __ENUMERATE_HTML_ATTRIBUTE(open)                       \
    __ENUMERATE_HTML_ATTRIBUTE(pattern)                    \
    __ENUMERATE_HTML_ATTRIBUTE(ping)                       \
    __ENUMERATE_HTML_ATTRIBUTE(placeholder)                \
    __ENUMERATE_HTML_ATTRIBUTE(playsinline)                \
    __ENUMERATE_HTML_ATTRIBUTE(poster)                     \
    __ENUMERATE_HTML_ATTRIBUTE(preload)                    \
    __ENUMERATE_HTML_ATTRIBUTE(readonly)                   \
    __ENUMERATE_HTML_ATTRIBUTE(rel)                        \
    __ENUMERATE_HTML_ATTRIBUTE(required)                   \
    __ENUMERATE_HTML_ATTRIBUTE(rev)                        \
    __ENUMERATE_HTML_ATTRIBUTE(reversed)                   \
    __ENUMERATE_HTML_ATTRIBUTE(rows)                       \
    __ENUMERATE_HTML_ATTRIBUTE(rowspan)                    \
    __ENUMERATE_HTML_ATTRIBUTE(rules)                      \
    __ENUMERATE_HTML_ATTRIBUTE(scheme)                     \
    __ENUMERATE_HTML_ATTRIBUTE(scrolling)                  \
    __ENUMERATE_HTML_ATTRIBUTE(selected)                   \
    __ENUMERATE_HTML_ATTRIBUTE(shape)                      \
    __ENUMERATE_HTML_ATTRIBUTE(size)                       \
    __ENUMERATE_HTML_ATTRIBUTE(sizes)                      \
    __ENUMERATE_HTML_ATTRIBUTE(src)                        \
    __ENUMERATE_HTML_ATTRIBUTE(srcdoc)                     \
    __ENUMERATE_HTML_ATTRIBUTE(srclang)                    \
    __ENUMERATE_HTML_ATTRIBUTE(srcset)                     \
    __ENUMERATE_HTML_ATTRIBUTE(standby)                    \
    __ENUMERATE_HTML_ATTRIBUTE(step)                       \
    __ENUMERATE_HTML_ATTRIBUTE(style)                      \
    __ENUMERATE_HTML_ATTRIBUTE(summary)                    \
    __ENUMERATE_HTML_ATTRIBUTE(target)                     \
    __ENUMERATE_HTML_ATTRIBUTE(text)                       \
    __ENUMERATE_HTML_ATTRIBUTE(title)                      \
    __ENUMERATE_HTML_ATTRIBUTE(type)                       \
    __ENUMERATE_HTML_ATTRIBUTE(usemap)                     \
    __ENUMERATE_HTML_ATTRIBUTE(valign)                     \
    __ENUMERATE_HTML_ATTRIBUTE(value)                      \
    __ENUMERATE_HTML_ATTRIBUTE(valuetype)                  \
    __ENUMERATE_HTML_ATTRIBUTE(version)                    \
    __ENUMERATE_HTML_ATTRIBUTE(vlink)                      \
    __ENUMERATE_HTML_ATTRIBUTE(vspace)                     \
    __ENUMERATE_HTML_ATTRIBUTE(width)                      \
    __ENUMERATE_HTML_ATTRIBUTE(wrap)

#define __ENUMERATE_HTML_ATTRIBUTE(name) extern FlyString name;
ENUMERATE_HTML_ATTRIBUTES
#undef __ENUMERATE_HTML_ATTRIBUTE

}
}
}
