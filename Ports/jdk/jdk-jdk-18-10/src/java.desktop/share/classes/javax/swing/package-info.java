/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides a set of &quot;lightweight&quot; (all-Java language) components
 * that, to the maximum degree possible, work the same on all platforms. For a
 * programmer's guide to using these components, see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/index.html"
 * target="_top">Creating a GUI with JFC/Swing</a>, a trail in
 * <em>The Java Tutorial</em>. For other resources, see
 * <a href="#related">Related Documentation</a>.
 *
 * <h2><a id="threading">Swing's Threading Policy</a></h2>
 * In general Swing is not thread safe. All Swing components and related
 * classes, unless otherwise documented, must be accessed on the event
 * dispatching thread.
 * <p>
 * Typical Swing applications do processing in response to an event generated
 * from a user gesture. For example, clicking on a {@code JButton} notifies all
 * {@code ActionListeners} added to the {@code JButton}. As all events generated
 * from a user gesture are dispatched on the event dispatching thread, most
 * developers are not impacted by the restriction.
 * <p>
 * Where the impact lies, however, is in constructing and showing a Swing
 * application. Calls to an application's {@code main} method, or methods in
 * {@code Applet}, are not invoked on the event dispatching thread. As such,
 * care must be taken to transfer control to the event dispatching thread when
 * constructing and showing an application or applet. The preferred way to
 * transfer control and begin working with Swing is to use {@code invokeLater}.
 * The {@code invokeLater} method schedules a {@code Runnable} to be processed
 * on the event dispatching thread. The following two examples work equally well
 * for transferring control and starting up a Swing application:
 * <pre>
 * import javax.swing.SwingUtilities;
 *
 * public class MyApp implements Runnable {
 *     public void run() {
 *         // Invoked on the event dispatching thread.
 *         // Construct and show GUI.
 *     }
 *
 *     public static void main(String[] args) {
 *         SwingUtilities.invokeLater(new MyApp());
 *     }
 * }</pre>
 * Or:<pre>
 * import javax.swing.SwingUtilities;
 *
 * public class MyApp {
 *     MyApp(String[] args) {
 *         // Invoked on the event dispatching thread.
 *         // Do any initialization here.
 *     }
 *
 *     public void show() {
 *         // Show the UI.
 *     }
 *
 *     public static void main(final String[] args) {
 *         // Schedule a job for the event-dispatching thread:
 *         // creating and showing this application's GUI.
 *         SwingUtilities.invokeLater(new Runnable() {
 *             public void run() {
 *                 new MyApp(args).show();
 *             }
 *         });
 *     }
 * }</pre>
 * This restriction also applies to models attached to Swing components. For
 * example, if a {@code TableModel} is attached to a {@code JTable}, the
 * {@code TableModel} should only be modified on the event dispatching thread.
 * If you modify the model on a separate thread you run the risk of exceptions
 * and possible display corruption.
 * <p>
 * Although it is generally safe to make updates to the UI immediately,
 * when executing on the event dispatch thread, there is an exception :
 * if a model listener tries to further change the UI before the UI has been
 * updated to reflect a pending change then the UI may render incorrectly.
 *
 * This can happen if an application installed listener needs to update the UI
 * in response to an event which will cause a change in the model structure.
 * It is important to first allow component installed listeners to process this
 * change, since there is no guarantee of the order in which listeners may be
 * called.
 *
 * The solution is for the application listener to make the change using
 * {@link javax.swing.SwingUtilities#invokeLater(Runnable)} so that any changes
 * to  UI rendering will be done post processing all the model listeners
 * installed by the component.
 * </p>
 * <p>
 *
 * As all events are delivered on the event dispatching thread, care must be
 * taken in event processing. In particular, a long running task, such as
 * network io or computational intensive processing, executed on the event
 * dispatching thread blocks the event dispatching thread from dispatching any
 * other events. While the event dispatching thread is blocked the application
 * is completely unresponsive to user input. Refer to
 * {@link javax.swing.SwingWorker} for the preferred way to do such processing
 * when working with Swing.
 * <p>
 * More information on this topic can be found in the
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/">Swing tutorial</a>,
 * in particular the section on
 * <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">
 * Concurrency in Swing</a>.
 *
 * <h2><a id="related">Related Documentation</a></h2>
 * For overviews, tutorials, examples, guides, and other documentation,
 * please see:
 * <ul>
 *     <li><a href="http://www.oracle.com/technetwork/java/javase/tech/articles-jsp-139072.html"
 *     target="_top">The Swing Connection</a></li>
 *     <li><a href="https://docs.oracle.com/javase/tutorial/"
 *     target="_top">The Java Tutorial</a></li>
 *     <li><a href="http://www.oracle.com/technetwork/java/javase/training/index.html"
 *     target="_top">Online Training</a>
 *     at the Java Developer Connection <sup>SM</sup></li>
 *     <li><a href="http://www.oracle.com/technetwork/java/javase/tech/index-jsp-142216.html"
 *     target="_top">Java Foundation Classes (JFC)</a> home page</li>
 * </ul>
 *
 * @serial exclude
 */
package javax.swing;
