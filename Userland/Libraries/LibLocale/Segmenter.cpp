/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <AK/Utf32View.h>
#include <LibLocale/Locale.h>
#include <LibLocale/Segmenter.h>
#include <LibUnicode/Segmentation.h>

namespace Locale {

SegmenterGranularity segmenter_granularity_from_string(StringView segmenter_granularity)
{
    if (segmenter_granularity == "grapheme"sv)
        return SegmenterGranularity::Grapheme;
    if (segmenter_granularity == "sentence"sv)
        return SegmenterGranularity::Sentence;
    if (segmenter_granularity == "word"sv)
        return SegmenterGranularity::Word;
    VERIFY_NOT_REACHED();
}

StringView segmenter_granularity_to_string(SegmenterGranularity segmenter_granularity)
{
    switch (segmenter_granularity) {
    case SegmenterGranularity::Grapheme:
        return "grapheme"sv;
    case SegmenterGranularity::Sentence:
        return "sentence"sv;
    case SegmenterGranularity::Word:
        return "word"sv;
    }
    VERIFY_NOT_REACHED();
}

class SegmenterImpl : public Segmenter {
public:
    SegmenterImpl(SegmenterGranularity segmenter_granularity)
        : Segmenter(segmenter_granularity)
    {
    }

    virtual ~SegmenterImpl() override = default;

    virtual NonnullOwnPtr<Segmenter> clone() const override
    {
        return make<SegmenterImpl>(m_segmenter_granularity);
    }

    virtual void set_segmented_text(String text) override
    {
        m_string_storage = move(text);
        set_text(m_string_storage.code_points());
    }

    virtual void set_segmented_text(Utf16View const& text) override
    {
        set_text(text);
    }

    void set_segmented_text(Utf32View const& text)
    {
        set_text(text);
    }

    virtual size_t current_boundary() override
    {
        return m_current_boundary;
    }

    virtual Optional<size_t> previous_boundary(size_t boundary, Inclusive inclusive) override
    {
        recompute_boundaries_if_necessary();

        if (inclusive == Inclusive::Yes)
            ++boundary;

        // FIXME: Add AK::lower_bound, use
        Optional<size_t> new_boundary;
        for (auto segment_boundary : m_boundaries) {
            if (segment_boundary < boundary) {
                new_boundary = segment_boundary;
                continue;
            }
            break;
        }

        if (new_boundary.has_value())
            m_current_boundary = new_boundary.value();
        return new_boundary;
    }

    virtual Optional<size_t> next_boundary(size_t boundary, Inclusive inclusive) override
    {
        recompute_boundaries_if_necessary();

        if (inclusive == Inclusive::Yes)
            --boundary;

        // FIXME: Add AK::upper_bound, use
        Optional<size_t> new_boundary;
        for (auto segment_boundary : m_boundaries) {
            if (segment_boundary > boundary) {
                new_boundary = segment_boundary;
                break;
            }
        }

        if (new_boundary.has_value())
            m_current_boundary = new_boundary.value();
        return new_boundary;
    }

    virtual void for_each_boundary(String text, SegmentationCallback callback) override
    {
        for_each_segmentation_boundary(text.code_points(), move(callback));
    }

    virtual void for_each_boundary(Utf16View const& text, SegmentationCallback callback) override
    {
        for_each_segmentation_boundary(text, move(callback));
    }

    virtual void for_each_boundary(Utf32View const& text, SegmentationCallback callback) override
    {
        for_each_segmentation_boundary(text, move(callback));
    }

    virtual bool is_current_boundary_word_like() const override
    {
        // FIXME: Implement one day.
        return false;
    }

private:
    void set_text(Variant<Utf8View, Utf16View, Utf32View> text)
    {
        m_segmented_text = text;
        m_must_recompute_boundaries = true;
    }

    void recompute_boundaries_if_necessary()
    {
        if (!m_must_recompute_boundaries)
            return;

        m_boundaries.clear();
        auto callback = [&](size_t boundary) {
            m_boundaries.append(boundary);
            return IterationDecision::Continue;
        };
        m_segmented_text.visit([&](auto const& text) { return for_each_segmentation_boundary(text, move(callback)); });
        m_must_recompute_boundaries = false;
    }

    template<class T>
    void for_each_segmentation_boundary(T const& text, SegmentationCallback callback)
    {
        switch (segmenter_granularity()) {
        case SegmenterGranularity::Grapheme:
            Unicode::for_each_grapheme_segmentation_boundary(text, move(callback));
            break;
        case SegmenterGranularity::Sentence:
            Unicode::for_each_sentence_segmentation_boundary(text, move(callback));
            break;
        case SegmenterGranularity::Word:
            Unicode::for_each_word_segmentation_boundary(text, move(callback));
            break;
        }
    }

    bool m_must_recompute_boundaries { true };
    Vector<size_t> m_boundaries;
    size_t m_current_boundary { 0 };
    String m_string_storage;
    Variant<Utf8View, Utf16View, Utf32View> m_segmented_text { Utf8View {} };
};

NonnullOwnPtr<Segmenter> Segmenter::create(SegmenterGranularity segmenter_granularity)
{
    return Segmenter::create(default_locale(), segmenter_granularity);
}

NonnullOwnPtr<Segmenter> Segmenter::create(StringView locale, SegmenterGranularity segmenter_granularity)
{
    // FIXME: Implement locale-specific segmentation.
    (void)locale;
    return make<SegmenterImpl>(segmenter_granularity);
}

}
