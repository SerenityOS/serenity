/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/CSS/CSSStyleSheet.h>

namespace Web::CSS {

class StyleSheetList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(StyleSheetList, Bindings::LegacyPlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<StyleSheetList>> create(DOM::Document& document);

    void add_sheet(CSSStyleSheet&);
    void remove_sheet(CSSStyleSheet&);

    Vector<JS::NonnullGCPtr<CSSStyleSheet>> const& sheets() const { return m_sheets; }
    Vector<JS::NonnullGCPtr<CSSStyleSheet>>& sheets() { return m_sheets; }

    CSSStyleSheet* item(size_t index) const
    {
        if (index >= m_sheets.size())
            return {};
        return const_cast<CSSStyleSheet*>(m_sheets[index].ptr());
    }

    size_t length() const { return m_sheets.size(); }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

private:
    explicit StyleSheetList(DOM::Document&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // ^Bindings::LegacyPlatformObject
    virtual bool supports_indexed_properties() const override { return true; }
    virtual bool supports_named_properties() const override { return false; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return false; }
    virtual bool has_named_property_deleter() const override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }

    void sort_sheets();

    JS::NonnullGCPtr<DOM::Document> m_document;
    Vector<JS::NonnullGCPtr<CSSStyleSheet>> m_sheets;
};

}
