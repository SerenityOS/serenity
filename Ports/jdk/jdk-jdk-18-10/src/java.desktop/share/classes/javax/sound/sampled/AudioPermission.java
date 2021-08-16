/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.sound.sampled;

import java.io.Serial;
import java.security.BasicPermission;

/**
 * The {@code AudioPermission} class represents access rights to the audio
 * system resources. An {@code AudioPermission} contains a target name but no
 * actions list; you either have the named permission or you don't.
 * <p>
 * The target name is the name of the audio permission (see the table below).
 * The names follow the hierarchical property-naming convention. Also, an
 * asterisk can be used to represent all the audio permissions.
 * <p>
 * The following table lists the possible {@code AudioPermission} target names.
 * For each name, the table provides a description of exactly what that
 * permission allows, as well as a discussion of the risks of granting code the
 * permission.
 *
 * <table class="striped">
 * <caption>Permission target name, what the permission allows, and associated
 * risks</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Permission Target Name
 *     <th scope="col">What the Permission Allows
 *     <th scope="col">Risks of Allowing this Permission
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">play
 *     <td>Audio playback through the audio device or devices on the system.
 *     Allows the application to obtain and manipulate lines and mixers for
 *     audio playback (rendering).
 *     <td>In some cases use of this permission may affect other
 *     applications because the audio from one line may be mixed with other
 *     audio being played on the system, or because manipulation of a mixer
 *     affects the audio for all lines using that mixer.
 *   <tr>
 *     <th scope="row">record
 *     <td>Audio recording through the audio device or devices on the system.
 *     Allows the application to obtain and manipulate lines and mixers for
 *     audio recording (capture).
 *     <td>In some cases use of this permission may affect other applications
 *     because manipulation of a mixer affects the audio for all lines using
 *     that mixer. This permission can enable an applet or application to
 *     eavesdrop on a user.
 * </tbody>
 * </table>
 *
 * @author Kara Kytle
 * @since 1.3
 */
public class AudioPermission extends BasicPermission {

    /**
     * Use serialVersionUID from JDK 1.3 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5518053473477801126L;

    /**
     * Creates a new {@code AudioPermission} object that has the specified
     * symbolic name, such as "play" or "record". An asterisk can be used to
     * indicate all audio permissions.
     *
     * @param  name the name of the new {@code AudioPermission}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is empty
     */
    public AudioPermission(final String name) {
        super(name);
    }

    /**
     * Creates a new {@code AudioPermission} object that has the specified
     * symbolic name, such as "play" or "record". The {@code actions} parameter
     * is currently unused and should be {@code null}.
     *
     * @param  name the name of the new {@code AudioPermission}
     * @param  actions (unused; should be {@code null})
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is empty
     */
    public AudioPermission(final String name, final String actions) {
        super(name, actions);
    }
}
