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
 * The {@code Line} interface represents a mono or multi-channel audio feed. A
 * line is an element of the digital audio "pipeline," such as a mixer, an input
 * or output port, or a data path into or out of a mixer.
 * <p>
 * A line can have controls, such as gain, pan, and reverb. The controls
 * themselves are instances of classes that extend the base {@link Control}
 * class. The {@code Line} interface provides two accessor methods for obtaining
 * the line's controls: {@link #getControls getControls} returns the entire set,
 * and {@link #getControl getControl} returns a single control of specified
 * type.
 * <p>
 * Lines exist in various states at different times. When a line opens, it
 * reserves system resources for itself, and when it closes, these resources are
 * freed for other objects or applications. The {@link #isOpen()} method lets
 * you discover whether a line is open or closed. An open line need not be
 * processing data, however. Such processing is typically initiated by
 * subinterface methods such as
 * {@link SourceDataLine#write SourceDataLine.write} and
 * {@link TargetDataLine#read TargetDataLine.read}.
 * <p>
 * You can register an object to receive notifications whenever the line's state
 * changes. The object must implement the {@link LineListener} interface, which
 * consists of the single method {@link LineListener#update update}. This method
 * will be invoked when a line opens and closes (and, if it's a {@link DataLine}
 * , when it starts and stops).
 * <p>
 * An object can be registered to listen to multiple lines. The event it
 * receives in its {@code update} method will specify which line created the
 * event, what type of event it was ({@code OPEN}, {@code CLOSE}, {@code START},
 * or {@code STOP}), and how many sample frames the line had processed at the
 * time the event occurred.
 * <p>
 * Certain line operations, such as open and close, can generate security
 * exceptions if invoked by unprivileged code when the line is a shared audio
 * resource.
 *
 * @author Kara Kytle
 * @see LineEvent
 * @since 1.3
 */
public interface Line extends AutoCloseable {

    /**
     * Obtains the {@code Line.Info} object describing this line.
     *
     * @return description of the line
     */
    Line.Info getLineInfo();

    /**
     * Opens the line, indicating that it should acquire any required system
     * resources and become operational. If this operation succeeds, the line is
     * marked as open, and an {@code OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Note that some lines, once closed, cannot be reopened. Attempts to reopen
     * such a line will always result in an {@code LineUnavailableException}.
     * <p>
     * Some types of lines have configurable properties that may affect resource
     * allocation. For example, a {@code DataLine} must be opened with a
     * particular format and buffer size. Such lines should provide a mechanism
     * for configuring these properties, such as an additional {@code open}
     * method or methods which allow an application to specify the desired
     * settings.
     * <p>
     * This method takes no arguments, and opens the line with the current
     * settings. For {@link SourceDataLine} and {@link TargetDataLine} objects,
     * this means that the line is opened with default settings. For a
     * {@link Clip}, however, the buffer size is determined when data is loaded.
     * Since this method does not allow the application to specify any data to
     * load, an {@code IllegalArgumentException} is thrown. Therefore, you
     * should instead use one of the {@code open} methods provided in the
     * {@code Clip} interface to load data into the {@code Clip}.
     * <p>
     * For {@code DataLine}'s, if the {@code DataLine.Info} object which was
     * used to retrieve the line, specifies at least one fully qualified audio
     * format, the last one will be used as the default format.
     *
     * @throws IllegalArgumentException if this method is called on a Clip
     *         instance
     * @throws LineUnavailableException if the line cannot be opened due to
     *         resource restrictions
     * @throws SecurityException if the line cannot be opened due to security
     *         restrictions
     * @see #close
     * @see #isOpen
     * @see LineEvent
     * @see DataLine
     * @see Clip#open(AudioFormat, byte[], int, int)
     * @see Clip#open(AudioInputStream)
     */
    void open() throws LineUnavailableException;

    /**
     * Closes the line, indicating that any system resources in use by the line
     * can be released. If this operation succeeds, the line is marked closed
     * and a {@code CLOSE} event is dispatched to the line's listeners.
     *
     * @throws SecurityException if the line cannot be closed due to security
     *         restrictions
     * @see #open
     * @see #isOpen
     * @see LineEvent
     */
    @Override
    void close();

    /**
     * Indicates whether the line is open, meaning that it has reserved system
     * resources and is operational, although it might not currently be playing
     * or capturing sound.
     *
     * @return {@code true} if the line is open, otherwise {@code false}
     * @see #open()
     * @see #close()
     */
    boolean isOpen();

    /**
     * Obtains the set of controls associated with this line. Some controls may
     * only be available when the line is open. If there are no controls, this
     * method returns an array of length 0.
     *
     * @return the array of controls
     * @see #getControl
     */
    Control[] getControls();

    /**
     * Indicates whether the line supports a control of the specified type. Some
     * controls may only be available when the line is open.
     *
     * @param  control the type of the control for which support is queried
     * @return {@code true} if at least one control of the specified type is
     *         supported, otherwise {@code false}
     */
    boolean isControlSupported(Control.Type control);

    /**
     * Obtains a control of the specified type, if there is any. Some controls
     * may only be available when the line is open.
     *
     * @param  control the type of the requested control
     * @return a control of the specified type
     * @throws IllegalArgumentException if a control of the specified type is
     *         not supported
     * @see #getControls
     * @see #isControlSupported(Control.Type control)
     */
    Control getControl(Control.Type control);

    /**
     * Adds a listener to this line. Whenever the line's status changes, the
     * listener's {@code update()} method is called with a {@code LineEvent}
     * object that describes the change.
     *
     * @param  listener the object to add as a listener to this line
     * @see #removeLineListener
     * @see LineListener#update
     * @see LineEvent
     */
    void addLineListener(LineListener listener);

    /**
     * Removes the specified listener from this line's list of listeners.
     *
     * @param  listener listener to remove
     * @see #addLineListener
     */
    void removeLineListener(LineListener listener);

    /**
     * A {@code Line.Info} object contains information about a line. The only
     * information provided by {@code Line.Info} itself is the Java class of the
     * line. A subclass of {@code Line.Info} adds other kinds of information
     * about the line. This additional information depends on which {@code Line}
     * subinterface is implemented by the kind of line that the
     * {@code Line.Info} subclass describes.
     * <p>
     * A {@code Line.Info} can be retrieved using various methods of the
     * {@code Line}, {@code Mixer}, and {@code AudioSystem} interfaces. Other
     * such methods let you pass a {@code Line.Info} as an argument, to learn
     * whether lines matching the specified configuration are available and to
     * obtain them.
     *
     * @author Kara Kytle
     * @see Line#getLineInfo()
     * @see Mixer#getSourceLineInfo()
     * @see Mixer#getTargetLineInfo()
     * @see Mixer#getLine(Line.Info)
     * @see Mixer#getSourceLineInfo(Line.Info)
     * @see Mixer#getTargetLineInfo(Line.Info)
     * @see Mixer#isLineSupported(Line.Info)
     * @see AudioSystem#getLine(Line.Info)
     * @see AudioSystem#getSourceLineInfo(Line.Info)
     * @see AudioSystem#getTargetLineInfo(Line.Info)
     * @see AudioSystem#isLineSupported(Line.Info)
     * @since 1.3
     */
    class Info {

        /**
         * The class of the line described by the info object.
         */
        private final Class<?> lineClass;

        /**
         * Constructs an info object that describes a line of the specified
         * class. This constructor is typically used by an application to
         * describe a desired line.
         *
         * @param  lineClass the class of the line that the new
         *         {@code Line.Info} object describes
         */
        public Info(Class<?> lineClass) {

            if (lineClass == null) {
                this.lineClass = Line.class;
            } else {
                this.lineClass = lineClass;
            }
        }

        /**
         * Obtains the class of the line that this {@code Line.Info} object
         * describes.
         *
         * @return the described line's class
         */
        public Class<?> getLineClass() {
            return lineClass;
        }

        /**
         * Indicates whether the specified info object matches this one. To
         * match, the specified object must be identical to or a special case of
         * this one. The specified info object must be either an instance of the
         * same class as this one, or an instance of a sub-type of this one. In
         * addition, the attributes of the specified object must be compatible
         * with the capabilities of this one. Specifically, the routing
         * configuration for the specified info object must be compatible with
         * that of this one. Subclasses may add other criteria to determine
         * whether the two objects match.
         *
         * @param  info the info object which is being compared to this one
         * @return {@code true} if the specified object matches this one,
         *         {@code false} otherwise
         */
        public boolean matches(Info info) {

            // $$kk: 08.30.99: is this backwards?
            // dataLine.matches(targetDataLine) == true: targetDataLine is always dataLine
            // targetDataLine.matches(dataLine) == false
            // so if i want to make sure i get a targetDataLine, i need:
            // targetDataLine.matches(prospective_match) == true
            // => prospective_match may be other things as well, but it is at least a targetDataLine
            // targetDataLine defines the requirements which prospective_match must meet.


            // "if this Class object represents a declared class, this method returns
            // true if the specified Object argument is an instance of the represented
            // class (or of any of its subclasses)"
            // GainControlClass.isInstance(MyGainObj) => true
            // GainControlClass.isInstance(MySpecialGainInterfaceObj) => true

            // this_class.isInstance(that_object)       => that object can by cast to this class
            //                                                                          => that_object's class may be a subtype of this_class
            //                                                                          => that may be more specific (subtype) of this

            // "If this Class object represents an interface, this method returns true
            // if the class or any superclass of the specified Object argument implements
            // this interface"
            // GainControlClass.isInstance(MyGainObj) => true
            // GainControlClass.isInstance(GenericControlObj) => may be false
            // => that may be more specific

            if (! (this.getClass().isInstance(info)) ) {
                return false;
            }

            // this.isAssignableFrom(that)  =>  this is same or super to that
            //                                                          =>      this is at least as general as that
            //                                                          =>      that may be subtype of this

            if (! (getLineClass().isAssignableFrom(info.getLineClass())) ) {
                return false;
            }

            return true;
        }

        /**
         * Returns a string representation of the info object.
         *
         * @return a string representation of the info object
         */
        @Override
        public String toString() {
            final String str = getLineClass().toString();
            if (getLineClass().getPackage() == Line.class.getPackage()) {
                return str.replace("javax.sound.sampled.", "");
            }
            return str;
        }
    }
}
