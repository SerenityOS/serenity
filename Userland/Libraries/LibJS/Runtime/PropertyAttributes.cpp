/*
 * Copyright (c) 2021, David Tuin <david.tuin@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PropertyAttributes.h"

namespace JS {

PropertyAttributes PropertyAttributes::overwrite(PropertyAttributes attr) const
{
    PropertyAttributes combined = m_bits;
    if (attr.has_configurable()) {
        if (attr.is_configurable()) {
            combined.set_configurable();
        } else {
            combined.m_bits &= ~Attribute::Configurable;
        }
        combined.set_has_configurable();
    }

    if (attr.has_enumerable()) {
        if (attr.is_enumerable()) {
            combined.set_enumerable();
        } else {
            combined.m_bits &= ~Attribute::Enumerable;
        }
        combined.set_has_configurable();
    }

    if (attr.has_writable()) {
        if (attr.is_writable()) {
            combined.set_writable();
        } else {
            combined.m_bits &= ~Attribute::Writable;
        }
        combined.set_has_writable();
    }

    if (attr.has_getter()) {
        combined.set_has_getter();
    }

    if (attr.has_setter()) {
        combined.set_has_setter();
    }
    return combined;
}

}
