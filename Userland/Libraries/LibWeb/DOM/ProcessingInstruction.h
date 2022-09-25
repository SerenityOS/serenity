/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/DOM/CharacterData.h>

namespace Web::DOM {

class ProcessingInstruction final : public CharacterData {
    WEB_PLATFORM_OBJECT(ProcessingInstruction, CharacterData);

public:
    virtual ~ProcessingInstruction() override = default;

    virtual FlyString node_name() const override { return m_target; }

    String const& target() const { return m_target; }

private:
    ProcessingInstruction(Document&, String const& data, String const& target);

    String m_target;
};

template<>
inline bool Node::fast_is<ProcessingInstruction>() const { return node_type() == (u16)NodeType::PROCESSING_INSTRUCTION_NODE; }

}
