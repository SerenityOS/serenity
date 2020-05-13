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

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>

namespace PaintBrush {

class Layer;

class Image : public RefCounted<Image> {
public:
    static RefPtr<Image> create_with_size(const Gfx::Size&);

    size_t layer_count() const { return m_layers.size(); }
    const Layer& layer(size_t index) const { return m_layers.at(index); }

    const Gfx::Size& size() const { return m_size; }
    Gfx::Rect rect() const { return { {}, m_size }; }

    void add_layer(NonnullRefPtr<Layer>);

    void paint_into(GUI::Painter&, const Gfx::Rect& dest_rect, const Gfx::Rect& src_rect);

    GUI::Model& layer_model();

    void move_layer_to_front(Layer&);
    void move_layer_to_back(Layer&);

private:
    explicit Image(const Gfx::Size&);

    size_t index_of(const Layer&) const;

    Gfx::Size m_size;
    NonnullRefPtrVector<Layer> m_layers;
    RefPtr<GUI::Model> m_layer_model;
};

}
