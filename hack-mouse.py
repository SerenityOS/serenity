#!/usr/bin/env python3

# flake8: noqa

from enum import IntFlag

import os
os.environ['PYGAME_HIDE_SUPPORT_PROMPT'] = 'hide'

import pygame
import sys


class MouseButton(IntFlag):
    NONE = 0x00
    LEFT_BUTTON = 0x01
    RIGHT_BUTTON = 0x02
    MIDDLE_BUTTON = 0x04
    BACKWARD_BUTTON = 0x08
    FORWARD_BUTTON = 0x10


PYGAME_TO_SCAN_CODE_MAP = {
    0: 0,
    pygame.K_ESCAPE: 0x1,
    pygame.K_1: 0x2,
    pygame.K_2: 0x3,
    pygame.K_3: 0x4,
    pygame.K_4: 0x5,
    pygame.K_5: 0x6,
    pygame.K_6: 0x7,
    pygame.K_7: 0x8,
    pygame.K_8: 0x9,
    pygame.K_9: 0xa,
    pygame.K_0: 0xb,
    'ß'.encode('latin-1')[0x0]: 0xc,
    '´'.encode('latin-1')[0x0]: 0xd,
    pygame.K_BACKSPACE: 0xe,
    pygame.K_TAB: 0xf,
    pygame.K_q: 0x10,
    pygame.K_w: 0x11,
    pygame.K_e: 0x12,
    pygame.K_r: 0x13,
    pygame.K_t: 0x14,
    pygame.K_z: 0x15,
    pygame.K_u: 0x16,
    pygame.K_i: 0x17,
    pygame.K_o: 0x18,
    pygame.K_p: 0x19,
    'ü'.encode('latin-1')[0x0]: 0x1a,
    pygame.K_PLUS: 0x1b,
    pygame.K_RETURN: 0x1c,
    pygame.K_LCTRL: 0x1d,
    pygame.K_a: 0x1e,
    pygame.K_s: 0x1f,
    pygame.K_d: 0x20,
    pygame.K_f: 0x21,
    pygame.K_g: 0x22,
    pygame.K_h: 0x23,
    pygame.K_j: 0x24,
    pygame.K_k: 0x25,
    pygame.K_l: 0x26,
    'ö'.encode('latin-1')[0x0]: 0x27,
    'ä'.encode('latin-1')[0x0]: 0x28,
    pygame.K_CARET: 0x29,
    pygame.K_LSHIFT: 0x2a,
    pygame.K_HASH: 0x2b,
    pygame.K_y: 0x2c,
    pygame.K_x: 0x2d,
    pygame.K_c: 0x2e,
    pygame.K_v: 0x2f,
    pygame.K_b: 0x30,
    pygame.K_n: 0x31,
    pygame.K_m: 0x32,
    pygame.K_COMMA: 0x33,
    pygame.K_PERIOD: 0x34,
    pygame.K_MINUS: 0x35,
    pygame.K_RSHIFT: 0x36,
    # Key_Asterisk: 0x37,
    pygame.K_LALT: 0x38,
    pygame.K_SPACE: 0x39,
    pygame.K_CAPSLOCK: 0x3a,
    pygame.K_F1: 0x3b,
    pygame.K_F2: 0x3c,
    pygame.K_F3: 0x3d,
    pygame.K_F4: 0x3e,
    pygame.K_F5: 0x3f,
    pygame.K_F6: 0x40,
    pygame.K_F7: 0x41,
    pygame.K_F8: 0x42,
    pygame.K_F9: 0x43,
    pygame.K_F10: 0x44,
    pygame.K_NUMLOCK: [0xe0, 0x45],
    # Key_Invalid: 0x46,
    pygame.K_HOME: [0xe0, 0x47],
    pygame.K_UP: [0xe0, 0x48],
    pygame.K_PAGEUP: [0xe0, 0x49],
    # Key_Minus: 0x4a,
    pygame.K_LEFT: [0xe0, 0x4b],
    # Key_Invalid: 0x4c,
    pygame.K_RIGHT: [0xe0, 0x4d],
    # Key_Plus: 0x4e,
    pygame.K_END: [0xe0, 0x4f],
    pygame.K_DOWN: [0xe0, 0x50],
    pygame.K_PAGEDOWN: [0xe0, 0x51],
    pygame.K_INSERT: [0xe0, 0x52],
    pygame.K_DELETE: [0xe0, 0x53],
    # Key_Invalid: 0x54,
    # Key_Invalid: 0x55,
    pygame.K_LESS: 0x56,
    pygame.K_F11: 0x57,
    pygame.K_F12: 0x58,
    # Key_Invalid: 0x59,
    # Key_Invalid: 0x5a,
    pygame.K_LSUPER: [0xe0, 0x5b],
    # Key_Invalid: 0x5c,
    pygame.K_MENU: [0xe0, 0x5d],

    pygame.K_RALT: [0xe0, 0x38],
}


pygame.init()
screen = pygame.display.set_mode((320, 240))
pygame.display.set_caption("Hack Mouse")

clock = pygame.time.Clock()

running = True
mouse_grabbed = False

screen.fill((10, 0, 40))
pygame.display.flip()

scan_codes = []

while running:
    relative_mouse_movement = [0, 0]

    mouse_up = False

    for event in pygame.event.get():
        match event.type:
            case pygame.QUIT:
                running = False
            case pygame.KEYDOWN:
                if event.key == pygame.K_g and event.mod & pygame.KMOD_CTRL and event.mod & pygame.KMOD_ALT and mouse_grabbed:
                    mouse_grabbed = False
                    pygame.mouse.set_visible(True)
                    pygame.event.set_grab(False)
                    scan_codes.append(0x80 | PYGAME_TO_SCAN_CODE_MAP[pygame.K_LCTRL])
                    scan_codes.append(0x80 | PYGAME_TO_SCAN_CODE_MAP[pygame.K_LALT])
                elif mouse_grabbed and event.key in PYGAME_TO_SCAN_CODE_MAP:
                    scan_code = PYGAME_TO_SCAN_CODE_MAP[event.key]
                    if isinstance(scan_code, list):
                        scan_codes += scan_code
                    else:
                        scan_codes.append(scan_code)
            case pygame.KEYUP if mouse_grabbed and event.key in PYGAME_TO_SCAN_CODE_MAP:
                scan_code = PYGAME_TO_SCAN_CODE_MAP[event.key]
                if isinstance(scan_code, list):
                    assert len(scan_code) == 2
                    scan_codes.append(scan_code[0])
                    scan_codes.append(0x80 | scan_code[1])
                else:
                    scan_codes.append(0x80 | scan_code)
            case pygame.MOUSEMOTION if mouse_grabbed:
                relative_mouse_movement[0] += event.rel[0]
                relative_mouse_movement[1] += -event.rel[1]
            case pygame.MOUSEBUTTONDOWN if not mouse_grabbed and event.button == pygame.BUTTON_LEFT:
                mouse_grabbed = True
                pygame.mouse.set_visible(False)
                pygame.event.set_grab(True)
            case pygame.MOUSEBUTTONUP:
                mouse_up = True

    pygame_mouse_buttons_pressed = pygame.mouse.get_pressed(num_buttons=3)
    mouse_buttons_pressed = MouseButton.NONE

    if pygame_mouse_buttons_pressed[0]:
        mouse_buttons_pressed |= MouseButton.LEFT_BUTTON
    if pygame_mouse_buttons_pressed[1]:
        mouse_buttons_pressed |= MouseButton.MIDDLE_BUTTON
    if pygame_mouse_buttons_pressed[2]:
        mouse_buttons_pressed |= MouseButton.RIGHT_BUTTON

    scan_code = 0
    if len(scan_codes) > 0:
        scan_code = scan_codes.pop(0)

    packet = relative_mouse_movement + [int(mouse_buttons_pressed)] + [scan_code]
    if packet != [0, 0, 0, 0] or mouse_up:
        print(",".join(map(str, packet)) + "F", end='')
        sys.stdout.flush()

    clock.tick(60)

pygame.quit()
