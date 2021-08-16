/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Infocmp helper methods.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public final class InfoCmp {

    private static final Map<String, Object> CAPS = new HashMap<>();

    private InfoCmp() {
    }

    @SuppressWarnings("unused")
    public enum Capability {

        auto_left_margin,           // auto_left_margin, bw, bw
        auto_right_margin,          // auto_right_margin, am, am
        back_color_erase,           // back_color_erase, bce, ut
        can_change,                 // can_change, ccc, cc
        ceol_standout_glitch,       // ceol_standout_glitch, xhp, xs
        col_addr_glitch,            // col_addr_glitch, xhpa, YA
        cpi_changes_res,            // cpi_changes_res, cpix, YF
        cr_cancels_micro_mode,      // cr_cancels_micro_mode, crxm, YB
        dest_tabs_magic_smso,       // dest_tabs_magic_smso, xt, xt
        eat_newline_glitch,         // eat_newline_glitch, xenl, xn
        erase_overstrike,           // erase_overstrike, eo, eo
        generic_type,               // generic_type, gn, gn
        hard_copy,                  // hard_copy, hc, hc
        hard_cursor,                // hard_cursor, chts, HC
        has_meta_key,               // has_meta_key, km, km
        has_print_wheel,            // has_print_wheel, daisy, YC
        has_status_line,            // has_status_line, hs, hs
        hue_lightness_saturation,   // hue_lightness_saturation, hls, hl
        insert_null_glitch,         // insert_null_glitch, in, in
        lpi_changes_res,            // lpi_changes_res, lpix, YG
        memory_above,               // memory_above, da, da
        memory_below,               // memory_below, db, db
        move_insert_mode,           // move_insert_mode, mir, mi
        move_standout_mode,         // move_standout_mode, msgr, ms
        needs_xon_xoff,             // needs_xon_xoff, nxon, nx
        no_esc_ctlc,                // no_esc_ctlc, xsb, xb
        no_pad_char,                // no_pad_char, npc, NP
        non_dest_scroll_region,     // non_dest_scroll_region, ndscr, ND
        non_rev_rmcup,              // non_rev_rmcup, nrrmc, NR
        over_strike,                // over_strike, os, os
        prtr_silent,                // prtr_silent, mc5i, 5i
        row_addr_glitch,            // row_addr_glitch, xvpa, YD
        semi_auto_right_margin,     // semi_auto_right_margin, sam, YE
        status_line_esc_ok,         // status_line_esc_ok, eslok, es
        tilde_glitch,               // tilde_glitch, hz, hz
        transparent_underline,      // transparent_underline, ul, ul
        xon_xoff,                   // xon_xoff, xon, xo
        columns,                    // columns, cols, co
        init_tabs,                  // init_tabs, it, it
        label_height,               // label_height, lh, lh
        label_width,                // label_width, lw, lw
        lines,                      // lines, lines, li
        lines_of_memory,            // lines_of_memory, lm, lm
        magic_cookie_glitch,        // magic_cookie_glitch, xmc, sg
        max_attributes,             // max_attributes, ma, ma
        max_colors,                 // max_colors, colors, Co
        max_pairs,                  // max_pairs, pairs, pa
        maximum_windows,            // maximum_windows, wnum, MW
        no_color_video,             // no_color_video, ncv, NC
        num_labels,                 // num_labels, nlab, Nl
        padding_baud_rate,          // padding_baud_rate, pb, pb
        virtual_terminal,           // virtual_terminal, vt, vt
        width_status_line,          // width_status_line, wsl, ws
        bit_image_entwining,        // bit_image_entwining, bitwin, Yo
        bit_image_type,             // bit_image_type, bitype, Yp
        buffer_capacity,            // buffer_capacity, bufsz, Ya
        buttons,                    // buttons, btns, BT
        dot_horz_spacing,           // dot_horz_spacing, spinh, Yc
        dot_vert_spacing,           // dot_vert_spacing, spinv, Yb
        max_micro_address,          // max_micro_address, maddr, Yd
        max_micro_jump,             // max_micro_jump, mjump, Ye
        micro_col_size,             // micro_col_size, mcs, Yf
        micro_line_size,            // micro_line_size, mls, Yg
        number_of_pins,             // number_of_pins, npins, Yh
        output_res_char,            // output_res_char, orc, Yi
        output_res_horz_inch,       // output_res_horz_inch, orhi, Yk
        output_res_line,            // output_res_line, orl, Yj
        output_res_vert_inch,       // output_res_vert_inch, orvi, Yl
        print_rate,                 // print_rate, cps, Ym
        wide_char_size,             // wide_char_size, widcs, Yn
        acs_chars,                  // acs_chars, acsc, ac
        back_tab,                   // back_tab, cbt, bt
        bell,                       // bell, bel, bl
        carriage_return,            // carriage_return, cr, cr
        change_char_pitch,          // change_char_pitch, cpi, ZA
        change_line_pitch,          // change_line_pitch, lpi, ZB
        change_res_horz,            // change_res_horz, chr, ZC
        change_res_vert,            // change_res_vert, cvr, ZD
        change_scroll_region,       // change_scroll_region, csr, cs
        char_padding,               // char_padding, rmp, rP
        clear_all_tabs,             // clear_all_tabs, tbc, ct
        clear_margins,              // clear_margins, mgc, MC
        clear_screen,               // clear_screen, clear, cl
        clr_bol,                    // clr_bol, el1, cb
        clr_eol,                    // clr_eol, el, ce
        clr_eos,                    // clr_eos, ed, cd
        column_address,             // column_address, hpa, ch
        command_character,          // command_character, cmdch, CC
        create_window,              // create_window, cwin, CW
        cursor_address,             // cursor_address, cup, cm
        cursor_down,                // cursor_down, cud1, do
        cursor_home,                // cursor_home, home, ho
        cursor_invisible,           // cursor_invisible, civis, vi
        cursor_left,                // cursor_left, cub1, le
        cursor_mem_address,         // cursor_mem_address, mrcup, CM
        cursor_normal,              // cursor_normal, cnorm, ve
        cursor_right,               // cursor_right, cuf1, nd
        cursor_to_ll,               // cursor_to_ll, ll, ll
        cursor_up,                  // cursor_up, cuu1, up
        cursor_visible,             // cursor_visible, cvvis, vs
        define_char,                // define_char, defc, ZE
        delete_character,           // delete_character, dch1, dc
        delete_line,                // delete_line, dl1, dl
        dial_phone,                 // dial_phone, dial, DI
        dis_status_line,            // dis_status_line, dsl, ds
        display_clock,              // display_clock, dclk, DK
        down_half_line,             // down_half_line, hd, hd
        ena_acs,                    // ena_acs, enacs, eA
        enter_alt_charset_mode,     // enter_alt_charset_mode, smacs, as
        enter_am_mode,              // enter_am_mode, smam, SA
        enter_blink_mode,           // enter_blink_mode, blink, mb
        enter_bold_mode,            // enter_bold_mode, bold, md
        enter_ca_mode,              // enter_ca_mode, smcup, ti
        enter_delete_mode,          // enter_delete_mode, smdc, dm
        enter_dim_mode,             // enter_dim_mode, dim, mh
        enter_doublewide_mode,      // enter_doublewide_mode, swidm, ZF
        enter_draft_quality,        // enter_draft_quality, sdrfq, ZG
        enter_insert_mode,          // enter_insert_mode, smir, im
        enter_italics_mode,         // enter_italics_mode, sitm, ZH
        enter_leftward_mode,        // enter_leftward_mode, slm, ZI
        enter_micro_mode,           // enter_micro_mode, smicm, ZJ
        enter_near_letter_quality,  // enter_near_letter_quality, snlq, ZK
        enter_normal_quality,       // enter_normal_quality, snrmq, ZL
        enter_protected_mode,       // enter_protected_mode, prot, mp
        enter_reverse_mode,         // enter_reverse_mode, rev, mr
        enter_secure_mode,          // enter_secure_mode, invis, mk
        enter_shadow_mode,          // enter_shadow_mode, sshm, ZM
        enter_standout_mode,        // enter_standout_mode, smso, so
        enter_subscript_mode,       // enter_subscript_mode, ssubm, ZN
        enter_superscript_mode,     // enter_superscript_mode, ssupm, ZO
        enter_underline_mode,       // enter_underline_mode, smul, us
        enter_upward_mode,          // enter_upward_mode, sum, ZP
        enter_xon_mode,             // enter_xon_mode, smxon, SX
        erase_chars,                // erase_chars, ech, ec
        exit_alt_charset_mode,      // exit_alt_charset_mode, rmacs, ae
        exit_am_mode,               // exit_am_mode, rmam, RA
        exit_attribute_mode,        // exit_attribute_mode, sgr0, me
        exit_ca_mode,               // exit_ca_mode, rmcup, te
        exit_delete_mode,           // exit_delete_mode, rmdc, ed
        exit_doublewide_mode,       // exit_doublewide_mode, rwidm, ZQ
        exit_insert_mode,           // exit_insert_mode, rmir, ei
        exit_italics_mode,          // exit_italics_mode, ritm, ZR
        exit_leftward_mode,         // exit_leftward_mode, rlm, ZS
        exit_micro_mode,            // exit_micro_mode, rmicm, ZT
        exit_shadow_mode,           // exit_shadow_mode, rshm, ZU
        exit_standout_mode,         // exit_standout_mode, rmso, se
        exit_subscript_mode,        // exit_subscript_mode, rsubm, ZV
        exit_superscript_mode,      // exit_superscript_mode, rsupm, ZW
        exit_underline_mode,        // exit_underline_mode, rmul, ue
        exit_upward_mode,           // exit_upward_mode, rum, ZX
        exit_xon_mode,              // exit_xon_mode, rmxon, RX
        fixed_pause,                // fixed_pause, pause, PA
        flash_hook,                 // flash_hook, hook, fh
        flash_screen,               // flash_screen, flash, vb
        form_feed,                  // form_feed, ff, ff
        from_status_line,           // from_status_line, fsl, fs
        goto_window,                // goto_window, wingo, WG
        hangup,                     // hangup, hup, HU
        init_1string,               // init_1string, is1, i1
        init_2string,               // init_2string, is2, is
        init_3string,               // init_3string, is3, i3
        init_file,                  // init_file, if, if
        init_prog,                  // init_prog, iprog, iP
        initialize_color,           // initialize_color, initc, Ic
        initialize_pair,            // initialize_pair, initp, Ip
        insert_character,           // insert_character, ich1, ic
        insert_line,                // insert_line, il1, al
        insert_padding,             // insert_padding, ip, ip
        key_a1,                     // key_a1, ka1, K1
        key_a3,                     // key_a3, ka3, K3
        key_b2,                     // key_b2, kb2, K2
        key_backspace,              // key_backspace, kbs, kb
        key_beg,                    // key_beg, kbeg, @1
        key_btab,                   // key_btab, kcbt, kB
        key_c1,                     // key_c1, kc1, K4
        key_c3,                     // key_c3, kc3, K5
        key_cancel,                 // key_cancel, kcan, @2
        key_catab,                  // key_catab, ktbc, ka
        key_clear,                  // key_clear, kclr, kC
        key_close,                  // key_close, kclo, @3
        key_command,                // key_command, kcmd, @4
        key_copy,                   // key_copy, kcpy, @5
        key_create,                 // key_create, kcrt, @6
        key_ctab,                   // key_ctab, kctab, kt
        key_dc,                     // key_dc, kdch1, kD
        key_dl,                     // key_dl, kdl1, kL
        key_down,                   // key_down, kcud1, kd
        key_eic,                    // key_eic, krmir, kM
        key_end,                    // key_end, kend, @7
        key_enter,                  // key_enter, kent, @8
        key_eol,                    // key_eol, kel, kE
        key_eos,                    // key_eos, ked, kS
        key_exit,                   // key_exit, kext, @9
        key_f0,                     // key_f0, kf0, k0
        key_f1,                     // key_f1, kf1, k1
        key_f10,                    // key_f10, kf10, k;
        key_f11,                    // key_f11, kf11, F1
        key_f12,                    // key_f12, kf12, F2
        key_f13,                    // key_f13, kf13, F3
        key_f14,                    // key_f14, kf14, F4
        key_f15,                    // key_f15, kf15, F5
        key_f16,                    // key_f16, kf16, F6
        key_f17,                    // key_f17, kf17, F7
        key_f18,                    // key_f18, kf18, F8
        key_f19,                    // key_f19, kf19, F9
        key_f2,                     // key_f2, kf2, k2
        key_f20,                    // key_f20, kf20, FA
        key_f21,                    // key_f21, kf21, FB
        key_f22,                    // key_f22, kf22, FC
        key_f23,                    // key_f23, kf23, FD
        key_f24,                    // key_f24, kf24, FE
        key_f25,                    // key_f25, kf25, FF
        key_f26,                    // key_f26, kf26, FG
        key_f27,                    // key_f27, kf27, FH
        key_f28,                    // key_f28, kf28, FI
        key_f29,                    // key_f29, kf29, FJ
        key_f3,                     // key_f3, kf3, k3
        key_f30,                    // key_f30, kf30, FK
        key_f31,                    // key_f31, kf31, FL
        key_f32,                    // key_f32, kf32, FM
        key_f33,                    // key_f33, kf33, FN
        key_f34,                    // key_f34, kf34, FO
        key_f35,                    // key_f35, kf35, FP
        key_f36,                    // key_f36, kf36, FQ
        key_f37,                    // key_f37, kf37, FR
        key_f38,                    // key_f38, kf38, FS
        key_f39,                    // key_f39, kf39, FT
        key_f4,                     // key_f4, kf4, k4
        key_f40,                    // key_f40, kf40, FU
        key_f41,                    // key_f41, kf41, FV
        key_f42,                    // key_f42, kf42, FW
        key_f43,                    // key_f43, kf43, FX
        key_f44,                    // key_f44, kf44, FY
        key_f45,                    // key_f45, kf45, FZ
        key_f46,                    // key_f46, kf46, Fa
        key_f47,                    // key_f47, kf47, Fb
        key_f48,                    // key_f48, kf48, Fc
        key_f49,                    // key_f49, kf49, Fd
        key_f5,                     // key_f5, kf5, k5
        key_f50,                    // key_f50, kf50, Fe
        key_f51,                    // key_f51, kf51, Ff
        key_f52,                    // key_f52, kf52, Fg
        key_f53,                    // key_f53, kf53, Fh
        key_f54,                    // key_f54, kf54, Fi
        key_f55,                    // key_f55, kf55, Fj
        key_f56,                    // key_f56, kf56, Fk
        key_f57,                    // key_f57, kf57, Fl
        key_f58,                    // key_f58, kf58, Fm
        key_f59,                    // key_f59, kf59, Fn
        key_f6,                     // key_f6, kf6, k6
        key_f60,                    // key_f60, kf60, Fo
        key_f61,                    // key_f61, kf61, Fp
        key_f62,                    // key_f62, kf62, Fq
        key_f63,                    // key_f63, kf63, Fr
        key_f7,                     // key_f7, kf7, k7
        key_f8,                     // key_f8, kf8, k8
        key_f9,                     // key_f9, kf9, k9
        key_find,                   // key_find, kfnd, @0
        key_help,                   // key_help, khlp, %1
        key_home,                   // key_home, khome, kh
        key_ic,                     // key_ic, kich1, kI
        key_il,                     // key_il, kil1, kA
        key_left,                   // key_left, kcub1, kl
        key_ll,                     // key_ll, kll, kH
        key_mark,                   // key_mark, kmrk, %2
        key_message,                // key_message, kmsg, %3
        key_move,                   // key_move, kmov, %4
        key_next,                   // key_next, knxt, %5
        key_npage,                  // key_npage, knp, kN
        key_open,                   // key_open, kopn, %6
        key_options,                // key_options, kopt, %7
        key_ppage,                  // key_ppage, kpp, kP
        key_previous,               // key_previous, kprv, %8
        key_print,                  // key_print, kprt, %9
        key_redo,                   // key_redo, krdo, %0
        key_reference,              // key_reference, kref, &1
        key_refresh,                // key_refresh, krfr, &2
        key_replace,                // key_replace, krpl, &3
        key_restart,                // key_restart, krst, &4
        key_resume,                 // key_resume, kres, &5
        key_right,                  // key_right, kcuf1, kr
        key_save,                   // key_save, ksav, &6
        key_sbeg,                   // key_sbeg, kBEG, &9
        key_scancel,                // key_scancel, kCAN, &0
        key_scommand,               // key_scommand, kCMD, *1
        key_scopy,                  // key_scopy, kCPY, *2
        key_screate,                // key_screate, kCRT, *3
        key_sdc,                    // key_sdc, kDC, *4
        key_sdl,                    // key_sdl, kDL, *5
        key_select,                 // key_select, kslt, *6
        key_send,                   // key_send, kEND, *7
        key_seol,                   // key_seol, kEOL, *8
        key_sexit,                  // key_sexit, kEXT, *9
        key_sf,                     // key_sf, kind, kF
        key_sfind,                  // key_sfind, kFND, *0
        key_shelp,                  // key_shelp, kHLP, #1
        key_shome,                  // key_shome, kHOM, #2
        key_sic,                    // key_sic, kIC, #3
        key_sleft,                  // key_sleft, kLFT, #4
        key_smessage,               // key_smessage, kMSG, %a
        key_smove,                  // key_smove, kMOV, %b
        key_snext,                  // key_snext, kNXT, %c
        key_soptions,               // key_soptions, kOPT, %d
        key_sprevious,              // key_sprevious, kPRV, %e
        key_sprint,                 // key_sprint, kPRT, %f
        key_sr,                     // key_sr, kri, kR
        key_sredo,                  // key_sredo, kRDO, %g
        key_sreplace,               // key_sreplace, kRPL, %h
        key_sright,                 // key_sright, kRIT, %i
        key_srsume,                 // key_srsume, kRES, %j
        key_ssave,                  // key_ssave, kSAV, !1
        key_ssuspend,               // key_ssuspend, kSPD, !2
        key_stab,                   // key_stab, khts, kT
        key_sundo,                  // key_sundo, kUND, !3
        key_suspend,                // key_suspend, kspd, &7
        key_undo,                   // key_undo, kund, &8
        key_up,                     // key_up, kcuu1, ku
        keypad_local,               // keypad_local, rmkx, ke
        keypad_xmit,                // keypad_xmit, smkx, ks
        lab_f0,                     // lab_f0, lf0, l0
        lab_f1,                     // lab_f1, lf1, l1
        lab_f10,                    // lab_f10, lf10, la
        lab_f2,                     // lab_f2, lf2, l2
        lab_f3,                     // lab_f3, lf3, l3
        lab_f4,                     // lab_f4, lf4, l4
        lab_f5,                     // lab_f5, lf5, l5
        lab_f6,                     // lab_f6, lf6, l6
        lab_f7,                     // lab_f7, lf7, l7
        lab_f8,                     // lab_f8, lf8, l8
        lab_f9,                     // lab_f9, lf9, l9
        label_format,               // label_format, fln, Lf
        label_off,                  // label_off, rmln, LF
        label_on,                   // label_on, smln, LO
        meta_off,                   // meta_off, rmm, mo
        meta_on,                    // meta_on, smm, mm
        micro_column_address,       // micro_column_address, mhpa, ZY
        micro_down,                 // micro_down, mcud1, ZZ
        micro_left,                 // micro_left, mcub1, Za
        micro_right,                // micro_right, mcuf1, Zb
        micro_row_address,          // micro_row_address, mvpa, Zc
        micro_up,                   // micro_up, mcuu1, Zd
        newline,                    // newline, nel, nw
        order_of_pins,              // order_of_pins, porder, Ze
        orig_colors,                // orig_colors, oc, oc
        orig_pair,                  // orig_pair, op, op
        pad_char,                   // pad_char, pad, pc
        parm_dch,                   // parm_dch, dch, DC
        parm_delete_line,           // parm_delete_line, dl, DL
        parm_down_cursor,           // parm_down_cursor, cud, DO
        parm_down_micro,            // parm_down_micro, mcud, Zf
        parm_ich,                   // parm_ich, ich, IC
        parm_index,                 // parm_index, indn, SF
        parm_insert_line,           // parm_insert_line, il, AL
        parm_left_cursor,           // parm_left_cursor, cub, LE
        parm_left_micro,            // parm_left_micro, mcub, Zg
        parm_right_cursor,          // parm_right_cursor, cuf, RI
        parm_right_micro,           // parm_right_micro, mcuf, Zh
        parm_rindex,                // parm_rindex, rin, SR
        parm_up_cursor,             // parm_up_cursor, cuu, UP
        parm_up_micro,              // parm_up_micro, mcuu, Zi
        pkey_key,                   // pkey_key, pfkey, pk
        pkey_local,                 // pkey_local, pfloc, pl
        pkey_xmit,                  // pkey_xmit, pfx, px
        plab_norm,                  // plab_norm, pln, pn
        print_screen,               // print_screen, mc0, ps
        prtr_non,                   // prtr_non, mc5p, pO
        prtr_off,                   // prtr_off, mc4, pf
        prtr_on,                    // prtr_on, mc5, po
        pulse,                      // pulse, pulse, PU
        quick_dial,                 // quick_dial, qdial, QD
        remove_clock,               // remove_clock, rmclk, RC
        repeat_char,                // repeat_char, rep, rp
        req_for_input,              // req_for_input, rfi, RF
        reset_1string,              // reset_1string, rs1, r1
        reset_2string,              // reset_2string, rs2, r2
        reset_3string,              // reset_3string, rs3, r3
        reset_file,                 // reset_file, rf, rf
        restore_cursor,             // restore_cursor, rc, rc
        row_address,                // row_address, vpa, cv
        save_cursor,                // save_cursor, sc, sc
        scroll_forward,             // scroll_forward, ind, sf
        scroll_reverse,             // scroll_reverse, ri, sr
        select_char_set,            // select_char_set, scs, Zj
        set_attributes,             // set_attributes, sgr, sa
        set_background,             // set_background, setb, Sb
        set_bottom_margin,          // set_bottom_margin, smgb, Zk
        set_bottom_margin_parm,     // set_bottom_margin_parm, smgbp, Zl
        set_clock,                  // set_clock, sclk, SC
        set_color_pair,             // set_color_pair, scp, sp
        set_foreground,             // set_foreground, setf, Sf
        set_left_margin,            // set_left_margin, smgl, ML
        set_left_margin_parm,       // set_left_margin_parm, smglp, Zm
        set_right_margin,           // set_right_margin, smgr, MR
        set_right_margin_parm,      // set_right_margin_parm, smgrp, Zn
        set_tab,                    // set_tab, hts, st
        set_top_margin,             // set_top_margin, smgt, Zo
        set_top_margin_parm,        // set_top_margin_parm, smgtp, Zp
        set_window,                 // set_window, wind, wi
        start_bit_image,            // start_bit_image, sbim, Zq
        start_char_set_def,         // start_char_set_def, scsd, Zr
        stop_bit_image,             // stop_bit_image, rbim, Zs
        stop_char_set_def,          // stop_char_set_def, rcsd, Zt
        subscript_characters,       // subscript_characters, subcs, Zu
        superscript_characters,     // superscript_characters, supcs, Zv
        tab,                        // tab, ht, ta
        these_cause_cr,             // these_cause_cr, docr, Zw
        to_status_line,             // to_status_line, tsl, ts
        tone,                       // tone, tone, TO
        underline_char,             // underline_char, uc, uc
        up_half_line,               // up_half_line, hu, hu
        user0,                      // user0, u0, u0
        user1,                      // user1, u1, u1
        user2,                      // user2, u2, u2
        user3,                      // user3, u3, u3
        user4,                      // user4, u4, u4
        user5,                      // user5, u5, u5
        user6,                      // user6, u6, u6
        user7,                      // user7, u7, u7
        user8,                      // user8, u8, u8
        user9,                      // user9, u9, u9
        wait_tone,                  // wait_tone, wait, WA
        xoff_character,             // xoff_character, xoffc, XF
        xon_character,              // xon_character, xonc, XN
        zero_motion,                // zero_motion, zerom, Zx
        alt_scancode_esc,           // alt_scancode_esc, scesa, S8
        bit_image_carriage_return,  // bit_image_carriage_return, bicr, Yv
        bit_image_newline,          // bit_image_newline, binel, Zz
        bit_image_repeat,           // bit_image_repeat, birep, Xy
        char_set_names,             // char_set_names, csnm, Zy
        code_set_init,              // code_set_init, csin, ci
        color_names,                // color_names, colornm, Yw
        define_bit_image_region,    // define_bit_image_region, defbi, Yx
        device_type,                // device_type, devt, dv
        display_pc_char,            // display_pc_char, dispc, S1
        end_bit_image_region,       // end_bit_image_region, endbi, Yy
        enter_pc_charset_mode,      // enter_pc_charset_mode, smpch, S2
        enter_scancode_mode,        // enter_scancode_mode, smsc, S4
        exit_pc_charset_mode,       // exit_pc_charset_mode, rmpch, S3
        exit_scancode_mode,         // exit_scancode_mode, rmsc, S5
        get_mouse,                  // get_mouse, getm, Gm
        key_mouse,                  // key_mouse, kmous, Km
        mouse_info,                 // mouse_info, minfo, Mi
        pc_term_options,            // pc_term_options, pctrm, S6
        pkey_plab,                  // pkey_plab, pfxl, xl
        req_mouse_pos,              // req_mouse_pos, reqmp, RQ
        scancode_escape,            // scancode_escape, scesc, S7
        set0_des_seq,               // set0_des_seq, s0ds, s0
        set1_des_seq,               // set1_des_seq, s1ds, s1
        set2_des_seq,               // set2_des_seq, s2ds, s2
        set3_des_seq,               // set3_des_seq, s3ds, s3
        set_a_background,           // set_a_background, setab, AB
        set_a_foreground,           // set_a_foreground, setaf, AF
        set_color_band,             // set_color_band, setcolor, Yz
        set_lr_margin,              // set_lr_margin, smglr, ML
        set_page_length,            // set_page_length, slines, YZ
        set_tb_margin,              // set_tb_margin, smgtb, MT
        enter_horizontal_hl_mode,   // enter_horizontal_hl_mode, ehhlm, Xh
        enter_left_hl_mode,         // enter_left_hl_mode, elhlm, Xl
        enter_low_hl_mode,          // enter_low_hl_mode, elohlm, Xo
        enter_right_hl_mode,        // enter_right_hl_mode, erhlm, Xr
        enter_top_hl_mode,          // enter_top_hl_mode, ethlm, Xt
        enter_vertical_hl_mode,     // enter_vertical_hl_mode, evhlm, Xv
        set_a_attributes,           // set_a_attributes, sgr1, sA
        set_pglen_inch,             // set_pglen_inch, slength, sL)
        ;

        public String[] getNames() {
            return getCapabilitiesByName().entrySet().stream()
                    .filter(e -> e.getValue() == this)
                    .map(Map.Entry::getValue)
                    .toArray(String[]::new);
        }

        public static Capability byName(String name) {
            return getCapabilitiesByName().get(name);
        }
    }

    public static Map<String, Capability> getCapabilitiesByName() {
        Map<String, Capability> capabilities = new LinkedHashMap<>();
        try (InputStream is = InfoCmp.class.getResourceAsStream("capabilities.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.lines().map(String::trim)
                    .filter(s -> !s.startsWith("#"))
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> {
                        String[] names = s.split(", ");
                        Capability cap = Enum.valueOf(Capability.class, names[0]);
                        capabilities.put(names[0], cap);
                        capabilities.put(names[1], cap);
                    });
            return capabilities;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static void setDefaultInfoCmp(String terminal, String caps) {
        CAPS.putIfAbsent(terminal, caps);
    }

    public static void setDefaultInfoCmp(String terminal, Supplier<String> caps) {
        CAPS.putIfAbsent(terminal, caps);
    }

    public static String getInfoCmp(
            String terminal
    ) throws IOException, InterruptedException {
        String caps = getLoadedInfoCmp(terminal);
        if (caps == null) {
            Process p = new ProcessBuilder(OSUtils.INFOCMP_COMMAND, terminal).start();
            caps = ExecHelper.waitAndCapture(p);
            CAPS.put(terminal, caps);
        }
        return caps;
    }

    public static String getLoadedInfoCmp(String terminal) {
        Object caps = CAPS.get(terminal);
        if (caps instanceof Supplier) {
            caps = ((Supplier) caps).get();
        }
        return (String) caps;
    }

    public static void parseInfoCmp(
            String capabilities,
            Set<Capability> bools,
            Map<Capability, Integer> ints,
            Map<Capability, String> strings
    ) {
        Map<String, Capability> capsByName = getCapabilitiesByName();
        String[] lines = capabilities.split("\n");
        for (int i = 1; i < lines.length; i++) {
            Matcher m = Pattern.compile("\\s*(([^,]|\\\\,)+)\\s*[,$]").matcher(lines[i]);
            while (m.find()) {
                String cap = m.group(1);
                if (cap.contains("#")) {
                    int index = cap.indexOf('#');
                    String key = cap.substring(0, index);
                    String val = cap.substring(index + 1);
                    int iVal;
                    if ("0".equals(val)) {
                        iVal = 0;
                    } else if (val.startsWith("0x")) {
                        iVal = Integer.parseInt(val.substring(2), 16);
                    } else if (val.startsWith("0")) {
                        iVal = Integer.parseInt(val.substring(1), 8);
                    } else {
                        iVal = Integer.parseInt(val);
                    }
                    Capability c = capsByName.get(key);
                    if (c != null) {
                        ints.put(c, iVal);
                    }
                } else if (cap.contains("=")) {
                    int index = cap.indexOf('=');
                    String key = cap.substring(0, index);
                    String val = cap.substring(index + 1);
                    Capability c = capsByName.get(key);
                    if (c != null) {
                        strings.put(c, val);
                    }
                } else {
                    Capability c = capsByName.get(cap);
                    if (c != null) {
                        bools.add(c);
                    }
                }
            }
        }
    }

    static String loadDefaultInfoCmp(String name) {
        try (InputStream is = InfoCmp.class.getResourceAsStream(name + ".caps");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    static {
        for (String s : Arrays.asList("dumb", "dumb-color", "ansi", "xterm", "xterm-256color",
                "windows", "windows-256color", "windows-conemu", "windows-vtp",
                "screen", "screen-256color")) {
            setDefaultInfoCmp(s, () -> loadDefaultInfoCmp(s));
        }
    }

}
