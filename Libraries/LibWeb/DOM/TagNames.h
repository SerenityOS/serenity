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
namespace TagNames {

#define ENUMERATE_HTML_TAGS          \
    __ENUMERATE_HTML_TAG(a)          \
    __ENUMERATE_HTML_TAG(abbr)       \
    __ENUMERATE_HTML_TAG(acronym)    \
    __ENUMERATE_HTML_TAG(address)    \
    __ENUMERATE_HTML_TAG(applet)     \
    __ENUMERATE_HTML_TAG(area)       \
    __ENUMERATE_HTML_TAG(article)    \
    __ENUMERATE_HTML_TAG(aside)      \
    __ENUMERATE_HTML_TAG(audio)      \
    __ENUMERATE_HTML_TAG(b)          \
    __ENUMERATE_HTML_TAG(base)       \
    __ENUMERATE_HTML_TAG(basefont)   \
    __ENUMERATE_HTML_TAG(bdi)        \
    __ENUMERATE_HTML_TAG(bdo)        \
    __ENUMERATE_HTML_TAG(bgsound)    \
    __ENUMERATE_HTML_TAG(big)        \
    __ENUMERATE_HTML_TAG(blink)      \
    __ENUMERATE_HTML_TAG(blockquote) \
    __ENUMERATE_HTML_TAG(body)       \
    __ENUMERATE_HTML_TAG(br)         \
    __ENUMERATE_HTML_TAG(button)     \
    __ENUMERATE_HTML_TAG(canvas)     \
    __ENUMERATE_HTML_TAG(caption)    \
    __ENUMERATE_HTML_TAG(center)     \
    __ENUMERATE_HTML_TAG(cite)       \
    __ENUMERATE_HTML_TAG(code)       \
    __ENUMERATE_HTML_TAG(col)        \
    __ENUMERATE_HTML_TAG(colgroup)   \
    __ENUMERATE_HTML_TAG(data)       \
    __ENUMERATE_HTML_TAG(datalist)   \
    __ENUMERATE_HTML_TAG(dd)         \
    __ENUMERATE_HTML_TAG(del)        \
    __ENUMERATE_HTML_TAG(details)    \
    __ENUMERATE_HTML_TAG(dfn)        \
    __ENUMERATE_HTML_TAG(dialog)     \
    __ENUMERATE_HTML_TAG(dir)        \
    __ENUMERATE_HTML_TAG(div)        \
    __ENUMERATE_HTML_TAG(dl)         \
    __ENUMERATE_HTML_TAG(dt)         \
    __ENUMERATE_HTML_TAG(em)         \
    __ENUMERATE_HTML_TAG(embed)      \
    __ENUMERATE_HTML_TAG(fieldset)   \
    __ENUMERATE_HTML_TAG(figcaption) \
    __ENUMERATE_HTML_TAG(figure)     \
    __ENUMERATE_HTML_TAG(font)       \
    __ENUMERATE_HTML_TAG(footer)     \
    __ENUMERATE_HTML_TAG(form)       \
    __ENUMERATE_HTML_TAG(frame)      \
    __ENUMERATE_HTML_TAG(frameset)   \
    __ENUMERATE_HTML_TAG(h1)         \
    __ENUMERATE_HTML_TAG(h2)         \
    __ENUMERATE_HTML_TAG(h3)         \
    __ENUMERATE_HTML_TAG(h4)         \
    __ENUMERATE_HTML_TAG(h5)         \
    __ENUMERATE_HTML_TAG(h6)         \
    __ENUMERATE_HTML_TAG(head)       \
    __ENUMERATE_HTML_TAG(header)     \
    __ENUMERATE_HTML_TAG(hgroup)     \
    __ENUMERATE_HTML_TAG(hr)         \
    __ENUMERATE_HTML_TAG(html)       \
    __ENUMERATE_HTML_TAG(i)          \
    __ENUMERATE_HTML_TAG(iframe)     \
    __ENUMERATE_HTML_TAG(image)      \
    __ENUMERATE_HTML_TAG(img)        \
    __ENUMERATE_HTML_TAG(input)      \
    __ENUMERATE_HTML_TAG(ins)        \
    __ENUMERATE_HTML_TAG(kbd)        \
    __ENUMERATE_HTML_TAG(keygen)     \
    __ENUMERATE_HTML_TAG(label)      \
    __ENUMERATE_HTML_TAG(legend)     \
    __ENUMERATE_HTML_TAG(li)         \
    __ENUMERATE_HTML_TAG(link)       \
    __ENUMERATE_HTML_TAG(listing)    \
    __ENUMERATE_HTML_TAG(main)       \
    __ENUMERATE_HTML_TAG(map)        \
    __ENUMERATE_HTML_TAG(mark)       \
    __ENUMERATE_HTML_TAG(marquee)    \
    __ENUMERATE_HTML_TAG(math)       \
    __ENUMERATE_HTML_TAG(menu)       \
    __ENUMERATE_HTML_TAG(meta)       \
    __ENUMERATE_HTML_TAG(meter)      \
    __ENUMERATE_HTML_TAG(nav)        \
    __ENUMERATE_HTML_TAG(nobr)       \
    __ENUMERATE_HTML_TAG(noembed)    \
    __ENUMERATE_HTML_TAG(noframes)   \
    __ENUMERATE_HTML_TAG(noscript)   \
    __ENUMERATE_HTML_TAG(object)     \
    __ENUMERATE_HTML_TAG(ol)         \
    __ENUMERATE_HTML_TAG(optgroup)   \
    __ENUMERATE_HTML_TAG(option)     \
    __ENUMERATE_HTML_TAG(output)     \
    __ENUMERATE_HTML_TAG(p)          \
    __ENUMERATE_HTML_TAG(param)      \
    __ENUMERATE_HTML_TAG(picture)    \
    __ENUMERATE_HTML_TAG(path)       \
    __ENUMERATE_HTML_TAG(plaintext)  \
    __ENUMERATE_HTML_TAG(pre)        \
    __ENUMERATE_HTML_TAG(progress)   \
    __ENUMERATE_HTML_TAG(q)          \
    __ENUMERATE_HTML_TAG(ruby)       \
    __ENUMERATE_HTML_TAG(rb)         \
    __ENUMERATE_HTML_TAG(rp)         \
    __ENUMERATE_HTML_TAG(rt)         \
    __ENUMERATE_HTML_TAG(rtc)        \
    __ENUMERATE_HTML_TAG(s)          \
    __ENUMERATE_HTML_TAG(samp)       \
    __ENUMERATE_HTML_TAG(script)     \
    __ENUMERATE_HTML_TAG(section)    \
    __ENUMERATE_HTML_TAG(select)     \
    __ENUMERATE_HTML_TAG(slot)       \
    __ENUMERATE_HTML_TAG(small)      \
    __ENUMERATE_HTML_TAG(source)     \
    __ENUMERATE_HTML_TAG(span)       \
    __ENUMERATE_HTML_TAG(strike)     \
    __ENUMERATE_HTML_TAG(strong)     \
    __ENUMERATE_HTML_TAG(style)      \
    __ENUMERATE_HTML_TAG(sub)        \
    __ENUMERATE_HTML_TAG(sup)        \
    __ENUMERATE_HTML_TAG(summary)    \
    __ENUMERATE_HTML_TAG(svg)        \
    __ENUMERATE_HTML_TAG(table)      \
    __ENUMERATE_HTML_TAG(tbody)      \
    __ENUMERATE_HTML_TAG(td)         \
    __ENUMERATE_HTML_TAG(template_)  \
    __ENUMERATE_HTML_TAG(textarea)   \
    __ENUMERATE_HTML_TAG(tfoot)      \
    __ENUMERATE_HTML_TAG(th)         \
    __ENUMERATE_HTML_TAG(thead)      \
    __ENUMERATE_HTML_TAG(time)       \
    __ENUMERATE_HTML_TAG(title)      \
    __ENUMERATE_HTML_TAG(tr)         \
    __ENUMERATE_HTML_TAG(track)      \
    __ENUMERATE_HTML_TAG(tt)         \
    __ENUMERATE_HTML_TAG(u)          \
    __ENUMERATE_HTML_TAG(ul)         \
    __ENUMERATE_HTML_TAG(var)        \
    __ENUMERATE_HTML_TAG(video)      \
    __ENUMERATE_HTML_TAG(wbr)        \
    __ENUMERATE_HTML_TAG(xmp)

#define __ENUMERATE_HTML_TAG(name) extern FlyString name;
ENUMERATE_HTML_TAGS
#undef __ENUMERATE_HTML_TAG

}
}
}
