/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibMaps/MapWidget.h>

namespace Maps {

class UsersMapWidget final : public MapWidget {
    C_OBJECT(UsersMapWidget);

public:
    bool show_users() const { return m_show_users; }
    void set_show_users(bool show_users)
    {
        m_show_users = show_users;
        if (m_show_users) {
            if (!m_users.has_value()) {
                get_users();
            } else {
                add_users_to_map();
            }
        } else {
            remove_markers_with_name("users"sv);
            remove_panels_with_name("users"sv);
        }
    }

private:
    UsersMapWidget(Options const&);

    void get_users();

    void add_users_to_map();

    RefPtr<Gfx::Bitmap> m_marker_gray_image;
    RefPtr<Protocol::Request> m_request;
    bool m_show_users { false };

    struct User {
        String nick;
        LatLng coordinates;
        bool contributor;
    };
    Optional<Vector<User>> m_users;
};

}
