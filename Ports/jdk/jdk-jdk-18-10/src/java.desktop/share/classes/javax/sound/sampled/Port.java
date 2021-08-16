/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Ports are simple lines for input or output of audio to or from audio devices.
 * Common examples of ports that act as source lines (mixer inputs) include the
 * microphone, line input, and CD-ROM drive. Ports that act as target lines
 * (mixer outputs) include the speaker, headphone, and line output. You can
 * access port using a {@link Port.Info} object.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public interface Port extends Line {

    /**
     * The {@code Port.Info} class extends {@code Line.Info} with additional
     * information specific to ports, including the port's name and whether it
     * is a source or a target for its mixer. By definition, a port acts as
     * either a source or a target to its mixer, but not both. (Audio input
     * ports are sources; audio output ports are targets.)
     * <p>
     * To learn what ports are available, you can retrieve port info objects
     * through the {@link Mixer#getSourceLineInfo getSourceLineInfo} and
     * {@link Mixer#getTargetLineInfo getTargetLineInfo} methods of the
     * {@code Mixer} interface. Instances of the {@code Port.Info} class may
     * also be constructed and used to obtain lines matching the parameters
     * specified in the {@code Port.Info} object.
     *
     * @author Kara Kytle
     * @since 1.3
     */
    class Info extends Line.Info {

        // AUDIO PORT TYPE DEFINES

        // SOURCE PORTS

        /**
         * A type of port that gets audio from a built-in microphone or a
         * microphone jack.
         */
        public static final Info MICROPHONE = new Info(Port.class,"MICROPHONE", true);

        /**
         * A type of port that gets audio from a line-level audio input jack.
         */
        public static final Info LINE_IN = new Info(Port.class,"LINE_IN", true);

        /**
         * A type of port that gets audio from a CD-ROM drive.
         */
        public static final Info COMPACT_DISC = new Info(Port.class,"COMPACT_DISC", true);

        // TARGET PORTS

        /**
         * A type of port that sends audio to a built-in speaker or a speaker
         * jack.
         */
        public static final Info SPEAKER = new Info(Port.class,"SPEAKER", false);

        /**
         * A type of port that sends audio to a headphone jack.
         */
        public static final Info HEADPHONE = new Info(Port.class,"HEADPHONE", false);

        /**
         * A type of port that sends audio to a line-level audio output jack.
         */
        public static final Info LINE_OUT = new Info(Port.class,"LINE_OUT", false);

        // FUTURE DIRECTIONS...

        // telephone
        // DAT
        // DVD

        /**
         * The string that names the port.
         */
        private final String name;

        /**
         * Whether this port is source or not.
         */
        private final boolean isSource;

        /**
         * Constructs a port's info object from the information given. This
         * constructor is typically used by an implementation of Java Sound to
         * describe a supported line.
         *
         * @param  lineClass the class of the port described by the info object
         * @param  name the string that names the port
         * @param  isSource {@code true} if the port is a source port (such as a
         *         microphone), {@code false} if the port is a target port (such
         *         as a speaker)
         */
        public Info(Class<?> lineClass, String name, boolean isSource) {

            super(lineClass);
            this.name = name;
            this.isSource = isSource;
        }

        /**
         * Obtains the name of the port.
         *
         * @return the string that names the port
         */
        public String getName() {
            return name;
        }

        /**
         * Indicates whether the port is a source or a target for its mixer.
         *
         * @return {@code true} if the port is a source port (such as a
         *         microphone), {@code false} if the port is a target port (such
         *         as a speaker)
         */
        public boolean isSource() {
            return isSource;
        }

        /**
         * Indicates whether this info object specified matches this one. To
         * match, the match requirements of the superclass must be met and the
         * types must be equal.
         *
         * @param  info the info object for which the match is queried
         * @return {@code true} if the specified object matches this one,
         *         {@code false} otherwise
         */
        @Override
        public boolean matches(Line.Info info) {

            if (! (super.matches(info)) ) {
                return false;
            }

            if (!(name.equals(((Info)info).getName())) ) {
                return false;
            }

            if (! (isSource == ((Info)info).isSource()) ) {
                return false;
            }

            return true;
        }

        /**
         * Indicates whether the specified object is equal to this info object,
         * returning {@code true} if the objects are the same.
         *
         * @param  obj the reference object with which to compare
         * @return {@code true} if the specified object is equal to this info
         *         object; {@code false} otherwise
         */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /**
         * Returns a hash code value for this info object.
         *
         * @return a hash code value for this info object
         */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /**
         * Returns a string representation of the info object.
         *
         * @return a string representation of the info object
         */
        @Override
        public final String toString() {
            return String.format("%s %s port", getName(),
                                 isSource() ? "source" : "target");
        }
    }
}
