/*
 * Copyright (c) 2022, Ashley N. <dev-serenity@ne0ndrag0n.com>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EscalatorWindow.h"
#include "MainWidget.h"
#include <AK/Assertions.h>
#include <LibCore/File.h>
#include <LibCore/SecretString.h>
#include <LibCore/System.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Widget.h>
#include <fcntl.h>
#include <unistd.h>

namespace Escalator {
EscalatorWindow::EscalatorWindow(StringView executable, Vector<StringView> arguments, EscalatorWindow::Options const& options)
    : m_arguments(move(arguments))
    , m_executable(executable)
    , m_current_user(options.current_user)
    , m_preserve_env(options.preserve_env)
    , m_forward_stdin(options.forward_stdin)
    , m_forward_stdout(options.forward_stdout)
{
    auto app_icon = GUI::FileIconProvider::icon_for_executable(m_executable);

    set_title("Run as Root");
    set_icon(app_icon.bitmap_for_size(16));
    resize(345, 100);
    set_resizable(false);
    set_minimizable(false);

    auto main_widget = Escalator::MainWidget::try_create().release_value_but_fixme_should_propagate_errors();
    set_main_widget(main_widget);

    RefPtr<GUI::Label> app_label = *main_widget->find_descendant_of_type_named<GUI::Label>("description");

    String prompt;
    if (options.description.is_empty())
        prompt = String::formatted("{} requires root access. Please enter password for user \"{}\".", m_arguments[0], m_current_user.username()).release_value_but_fixme_should_propagate_errors();
    else
        prompt = String::from_utf8(options.description).release_value_but_fixme_should_propagate_errors();

    app_label->set_text(move(prompt));

    m_icon_image_widget = *main_widget->find_descendant_of_type_named<GUI::ImageWidget>("icon");
    m_icon_image_widget->set_bitmap(app_icon.bitmap_for_size(32));

    m_ok_button = *main_widget->find_descendant_of_type_named<GUI::DialogButton>("ok_button");
    size_t password_attempts = 0;
    m_ok_button->on_click = [this, password_attempts](auto) mutable {
        if (check_password()) {
            auto result = execute_command();
            if (result.is_error()) {
                GUI::MessageBox::show_error(this, ByteString::formatted("Failed to execute command: {}", result.error()));
            }
            close();
        } else {
            if (++password_attempts >= 3) {
                GUI::MessageBox::show_error(this, ByteString::formatted("Too many failed attempts"));
                close();
            }
        }
    };
    m_ok_button->set_default(true);

    m_cancel_button = *main_widget->find_descendant_of_type_named<GUI::DialogButton>("cancel_button");
    m_cancel_button->on_click = [this](auto) {
        close();
    };

    m_password_input = *main_widget->find_descendant_of_type_named<GUI::PasswordBox>("password");
    m_password_input->set_focus(true);
}

bool EscalatorWindow::check_password()
{
    ByteString password = m_password_input->text();
    if (password.is_empty()) {
        GUI::MessageBox::show_error(this, "Please enter a password."sv);
        return false;
    }

    // FIXME: PasswordBox really should store its input directly as a SecretString.
    Core::SecretString password_secret = Core::SecretString::take_ownership(password.to_byte_buffer());
    if (!m_current_user.authenticate(password_secret)) {
        GUI::MessageBox::show_error(this, "Incorrect or disabled password."sv);
        m_password_input->select_all();
        return false;
    }

    return true;
}

ErrorOr<void> EscalatorWindow::execute_command()
{
    char const* envp[] = { nullptr };
    Vector<char const*, 4> argv;
    for (auto& arg : m_arguments)
        argv.append(arg.characters_without_null_termination());
    argv.append(nullptr);

    // Escalate process privilege to root user.
    TRY(Core::System::seteuid(0));
    auto root_user = TRY(Core::Account::from_uid(0));
    TRY(root_user.login());

    if (m_forward_stdin || m_forward_stdout) {
        auto in_pipefds = TRY(Core::System::pipe2(O_CLOEXEC));
        auto out_pipefds = TRY(Core::System::pipe2(O_CLOEXEC));
        ScopeGuard guard_fds { [&] {
            ::close(in_pipefds[1]);
            ::close(out_pipefds[0]);
        } };
        {
            posix_spawn_file_actions_t file_actions;
            posix_spawn_file_actions_init(&file_actions);
            posix_spawn_file_actions_adddup2(&file_actions, in_pipefds[0], STDIN_FILENO);
            posix_spawn_file_actions_adddup2(&file_actions, out_pipefds[1], STDOUT_FILENO);

            ScopeGuard guard_fds_and_file_actions { [&]() {
                posix_spawn_file_actions_destroy(&file_actions);
                ::close(in_pipefds[0]);
                ::close(out_pipefds[1]);
            } };

            TRY(Core::System::pledge("stdio sendfd rpath proc exec"));
            (void)TRY(Core::System::posix_spawn(m_executable, &file_actions, nullptr, const_cast<char* const*>(argv.data()), m_preserve_env ? environ : const_cast<char**>(envp)));

            if (m_forward_stdin) {
                auto in_outfile = TRY(Core::File::adopt_fd(in_pipefds[1], Core::File::OpenMode::Write, Core::File::ShouldCloseFileDescriptor::No));
                auto in_infile = TRY(Core::File::standard_input());
                TRY(in_outfile->write_until_depleted(TRY(in_infile->read_until_eof())));
            }
            if (m_forward_stdout) {
                auto out_outfile = TRY(Core::File::standard_output());
                auto out_infile = TRY(Core::File::adopt_fd(out_pipefds[0], Core::File::OpenMode::Read, Core::File::ShouldCloseFileDescriptor::No));
                TRY(out_outfile->write_until_depleted(TRY(out_infile->read_until_eof())));
            }
        }
    } else {
        TRY(Core::System::pledge("stdio sendfd rpath proc exec"));
        (void)TRY(Core::System::posix_spawn(m_executable, nullptr, nullptr, const_cast<char* const*>(argv.data()), m_preserve_env ? environ : const_cast<char**>(envp)));
    }

    return {};
}
}
