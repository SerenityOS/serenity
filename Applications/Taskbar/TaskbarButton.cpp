/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "TaskbarButton.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GWindowServerConnection.h>

TaskbarButton::TaskbarButton(const WindowIdentifier& identifier, GUI::Widget* parent)
    : GUI::Button(parent)
    , m_identifier(identifier)
{
}

TaskbarButton::~TaskbarButton()
{
}

void TaskbarButton::context_menu_event(GUI::ContextMenuEvent&)
{
    GUI::WindowServerConnection::the().post_message(Messages::WindowServer::WM_PopupWindowMenu(m_identifier.client_id(), m_identifier.window_id(), screen_relative_rect().location()));
}

void TaskbarButton::resize_event(GUI::ResizeEvent& event)
{
    GUI::WindowServerConnection::the().post_message(
        Messages::WindowServer::WM_SetWindowTaskbarRect(
            m_identifier.client_id(),
            m_identifier.window_id(),
            screen_relative_rect()));
    return GUI::Button::resize_event(event);
}
