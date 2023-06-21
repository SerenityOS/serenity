/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <AK/Random.h>
#include <Demos/Tubes/Shapes.h>
#include <Demos/Tubes/Tubes.h>
#include <LibGL/GLContext.h>
#include <LibGUI/Application.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>

constexpr size_t grid_resolution = 15;
constexpr int reset_every_ticks = 900;
constexpr double rotation_range = 35.;
constexpr u8 tube_maximum_count = 12;
constexpr u8 tube_minimum_count = 3;
constexpr double tube_movement_per_tick = .25;
constexpr double tube_relative_thickness = .6;
constexpr int tube_travel_max_stretch = 6;

static double random_double()
{
    return get_random<u32>() / static_cast<double>(NumericLimits<u32>::max());
}

static int random_int(int min, int max)
{
    return min + round_to<int>(random_double() * (max - min));
}

static IntVector4 tube_rotation_for_direction(Direction direction)
{
    switch (direction) {
    case Direction::XPositive:
        return { 0, 1, 0, -90 };
    case Direction::XNegative:
        return { 0, 1, 0, 90 };
    case Direction::YPositive:
        return { 1, 0, 0, 90 };
    case Direction::YNegative:
        return { 1, 0, 0, -90 };
    case Direction::ZPositive:
        return { 0, 1, 0, 180 };
    case Direction::ZNegative:
        return { 0, 0, 0, 0 };
    default:
        VERIFY_NOT_REACHED();
    }
}

static IntVector3 vector_for_direction(Direction direction)
{
    switch (direction) {
    case Direction::XPositive:
        return { 1, 0, 0 };
    case Direction::XNegative:
        return { -1, 0, 0 };
    case Direction::YPositive:
        return { 0, 1, 0 };
    case Direction::YNegative:
        return { 0, -1, 0 };
    case Direction::ZPositive:
        return { 0, 0, 1 };
    case Direction::ZNegative:
        return { 0, 0, -1 };
    default:
        VERIFY_NOT_REACHED();
    }
}

Tubes::Tubes(int interval)
    : m_grid(MUST(FixedArray<u8>::create(grid_resolution * grid_resolution * grid_resolution)))
{
    on_screensaver_exit = []() { GUI::Application::the()->quit(); };
    start_timer(interval);
}

void Tubes::choose_new_direction_for_tube(Tube& tube)
{
    // Find all possible directions
    Vector<Direction, 6> possible_directions;
    for (int i = 1; i <= 6; ++i) {
        auto direction = static_cast<Direction>(i);
        auto direction_vector = vector_for_direction(direction);
        auto check_position = tube.position + direction_vector;
        if (is_valid_grid_position(check_position) && get_grid(check_position) == 0)
            possible_directions.append(direction);
    }

    // If tube is stuck, kill it :^(
    if (possible_directions.is_empty()) {
        tube.direction = Direction::None;
        tube.active = false;
        return;
    }

    // Remove our old direction if we have other options available
    Direction const old_direction = tube.direction;
    if (possible_directions.size() >= 2 && possible_directions.contains_slow(old_direction))
        possible_directions.remove_all_matching([&old_direction](Direction const& item) { return item == old_direction; });

    // Select a random new direction
    tube.direction = possible_directions[random_int(0, static_cast<int>(possible_directions.size()) - 1)];

    // Determine how far we can go in this direction
    auto direction_vector = vector_for_direction(tube.direction);
    int max_stretch = random_int(1, tube_travel_max_stretch);
    IntVector3 new_target = tube.position;
    while (max_stretch-- > 0) {
        new_target += direction_vector;
        if (!is_valid_grid_position(new_target) || get_grid(new_target) != 0)
            break;
        set_grid(new_target, 1);
        tube.target_position = new_target;
    }
    tube.progress_to_target = 0.;
}

ErrorOr<void> Tubes::create_buffer(Gfx::IntSize size)
{
    m_bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, size));
    m_gl_context = TRY(GL::create_context(*m_bitmap));
    return {};
}

u8 Tubes::get_grid(IntVector3 position)
{
    return m_grid[position.z() * grid_resolution * grid_resolution + position.y() * grid_resolution + position.x()];
}

bool Tubes::is_valid_grid_position(Gfx::IntVector3 position)
{
    return position.x() >= 0
        && position.x() < static_cast<int>(grid_resolution)
        && position.y() >= 0
        && position.y() < static_cast<int>(grid_resolution)
        && position.z() >= 0
        && position.z() < static_cast<int>(grid_resolution);
}

void Tubes::set_grid(IntVector3 position, u8 value)
{
    m_grid[position.z() * grid_resolution * grid_resolution + position.y() * grid_resolution + position.x()] = value;
}

void Tubes::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.blit(rect().location(), *m_bitmap, m_bitmap->rect());
}

void Tubes::reset_tubes()
{
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Random rotation
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glPushMatrix();
    glRotated((random_double() - .5) * 2 * rotation_range, 0., 1., 0.);
    glMatrixMode(GL_MODELVIEW);

    // Clear grid
    m_grid.fill_with(0);

    // Create new set of tubes
    auto free_grid_position = [&]() {
        for (;;) {
            IntVector3 position = {
                random_int(0, grid_resolution - 1),
                random_int(0, grid_resolution - 1),
                random_int(0, grid_resolution - 1),
            };
            if (get_grid(position) != 0)
                continue;
            return position;
        }
    };
    m_tubes.clear_with_capacity();
    int tube_count = random_int(tube_minimum_count, tube_maximum_count);
    while (tube_count-- > 0) {
        Tube new_tube = {
            .color = {
                random_double(),
                random_double(),
                random_double(),
            },
            .position = free_grid_position(),
        };
        choose_new_direction_for_tube(new_tube);
        m_tubes.append(new_tube);
        set_grid(new_tube.position, 1);
    }
}

void Tubes::setup_view()
{
    glClearColor(0.f, 0.f, 0.f, 1.f);

    glMatrixMode(GL_PROJECTION);
    double const zoom = .25;
    auto const half_aspect_ratio = static_cast<double>(m_bitmap->width()) / m_bitmap->height() * zoom;
    glFrustum(-half_aspect_ratio, half_aspect_ratio, -zoom, zoom, .5, 10.);
    glTranslated(0., 0., -2.);
    glPushMatrix();
    glMatrixMode(GL_MODELVIEW);

    // Set up lighting
    glEnable(GL_LIGHTING);
    glEnable(GL_LIGHT0);
    GLfloat light_ambient[] { .0f, .0f, .0f, 1.f };
    glLightfv(GL_LIGHT0, GL_AMBIENT, light_ambient);
    GLfloat light_diffuse[] { 1.f, 1.f, 1.f, 1.f };
    glLightfv(GL_LIGHT0, GL_DIFFUSE, light_diffuse);
    GLfloat light_specular[] { 1.f, 1.f, 1.f, 1.f };
    glLightfv(GL_LIGHT0, GL_SPECULAR, light_specular);
    GLfloat light_position[] { .5f, 1.f, .5f, 0.f };
    glLightfv(GL_LIGHT0, GL_POSITION, light_position);

    GLfloat mat_specular[] { 1.f, 1.f, 1.f, 1.f };
    glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular);
    glMaterialf(GL_FRONT, GL_SHININESS, 8.f);

    // Adapt the vertex color as ambient and diffuse colors
    glEnable(GL_COLOR_MATERIAL);
    glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glEnable(GL_NORMALIZE);
    glShadeModel(GL_SMOOTH);
}

void Tubes::timer_event(Core::TimerEvent&)
{
    update_tubes();
    m_gl_context->present();
    repaint();
}

void Tubes::update_tubes()
{
    if (++m_ticks % reset_every_ticks == 0)
        reset_tubes();

    double const primitive_size = 2.; // our tubes and spheres are 1 in diameter, so object size is 2
    double const grid_width = 2.;
    double const grid_scale = 1. / grid_resolution;
    double const primitive_scale = 1. / primitive_size;
    double const tube_length_scale = tube_movement_per_tick * primitive_size;
    double const tube_thickness_scale = tube_relative_thickness * primitive_scale;
    for (auto& tube : m_tubes) {
        if (!tube.active)
            continue;

        glColor3d(tube.color.x(), tube.color.y(), tube.color.z());
        glPushMatrix();

        auto pos = tube.position;
        glTranslated(
            pos.x() * grid_scale * grid_width - (grid_width / 2.),
            pos.y() * grid_scale * grid_width - (grid_width / 2.),
            pos.z() * grid_scale * grid_width - (grid_width / 2.));
        glScaled(grid_scale, grid_scale, grid_scale);

        // Draw sphere if we're at the start or a corner
        if (tube.progress_to_target == 0.) {
            glPushMatrix();
            glScaled(tube_thickness_scale, tube_thickness_scale, tube_thickness_scale);
            draw_sphere();
            glPopMatrix();
        }

        // Draw tube at the current position
        glPushMatrix();
        auto direction_vector = vector_for_direction(tube.direction);
        auto distance_to_target = (tube.target_position - tube.position).length<double>();
        auto movement_magnitude = tube.progress_to_target * (distance_to_target - tube_movement_per_tick) / distance_to_target * grid_width;
        glTranslated(
            direction_vector.x() * movement_magnitude,
            direction_vector.y() * movement_magnitude,
            direction_vector.z() * movement_magnitude);
        auto tube_rotation = tube_rotation_for_direction(tube.direction);
        glRotated(tube_rotation.w(), tube_rotation.x(), tube_rotation.y(), tube_rotation.z());
        glScaled(tube_thickness_scale, tube_thickness_scale, primitive_scale * tube_length_scale);

        draw_tube();
        glPopMatrix();

        // Move towards target
        if (tube.progress_to_target >= distance_to_target) {
            tube.position = tube.target_position;
            choose_new_direction_for_tube(tube);
        } else {
            tube.progress_to_target = min(tube.progress_to_target + tube_movement_per_tick, distance_to_target);
        }

        glPopMatrix();
    }
}
