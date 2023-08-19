/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class BackgroundStyleValue final : public StyleValueWithDefaultOperators<BackgroundStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BackgroundStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue const> color,
        ValueComparingNonnullRefPtr<StyleValue const> image,
        ValueComparingNonnullRefPtr<StyleValue const> position,
        ValueComparingNonnullRefPtr<StyleValue const> size,
        ValueComparingNonnullRefPtr<StyleValue const> repeat,
        ValueComparingNonnullRefPtr<StyleValue const> attachment,
        ValueComparingNonnullRefPtr<StyleValue const> origin,
        ValueComparingNonnullRefPtr<StyleValue const> clip)
    {
        return adopt_ref(*new (nothrow) BackgroundStyleValue(move(color), move(image), move(position), move(size), move(repeat), move(attachment), move(origin), move(clip)));
    }
    virtual ~BackgroundStyleValue() override;

    size_t layer_count() const { return m_properties.layer_count; }

    auto attachment() const { return m_properties.attachment; }
    auto clip() const { return m_properties.clip; }
    auto color() const { return m_properties.color; }
    auto image() const { return m_properties.image; }
    auto origin() const { return m_properties.origin; }
    auto position() const { return m_properties.position; }
    auto repeat() const { return m_properties.repeat; }
    auto size() const { return m_properties.size; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BackgroundStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BackgroundStyleValue(
        ValueComparingNonnullRefPtr<StyleValue const> color,
        ValueComparingNonnullRefPtr<StyleValue const> image,
        ValueComparingNonnullRefPtr<StyleValue const> position,
        ValueComparingNonnullRefPtr<StyleValue const> size,
        ValueComparingNonnullRefPtr<StyleValue const> repeat,
        ValueComparingNonnullRefPtr<StyleValue const> attachment,
        ValueComparingNonnullRefPtr<StyleValue const> origin,
        ValueComparingNonnullRefPtr<StyleValue const> clip);

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue const> color;
        ValueComparingNonnullRefPtr<StyleValue const> image;
        ValueComparingNonnullRefPtr<StyleValue const> position;
        ValueComparingNonnullRefPtr<StyleValue const> size;
        ValueComparingNonnullRefPtr<StyleValue const> repeat;
        ValueComparingNonnullRefPtr<StyleValue const> attachment;
        ValueComparingNonnullRefPtr<StyleValue const> origin;
        ValueComparingNonnullRefPtr<StyleValue const> clip;
        size_t layer_count;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
