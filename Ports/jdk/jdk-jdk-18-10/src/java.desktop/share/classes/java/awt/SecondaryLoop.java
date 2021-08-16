/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

/**
 * A helper interface to run the nested event loop.
 * <p>
 * Objects that implement this interface are created with the
 * {@link EventQueue#createSecondaryLoop} method. The interface
 * provides two methods, {@link #enter} and {@link #exit},
 * which can be used to start and stop the event loop.
 * <p>
 * When the {@link #enter} method is called, the current
 * thread is blocked until the loop is terminated by the
 * {@link #exit} method. Also, a new event loop is started
 * on the event dispatch thread, which may or may not be
 * the current thread. The loop can be terminated on any
 * thread by calling its {@link #exit} method. After the
 * loop is terminated, the {@code SecondaryLoop} object can
 * be reused to run a new nested event loop.
 * <p>
 * A typical use case of applying this interface is AWT
 * and Swing modal dialogs. When a modal dialog is shown on
 * the event dispatch thread, it enters a new secondary loop.
 * Later, when the dialog is hidden or disposed, it exits
 * the loop, and the thread continues its execution.
 * <p>
 * The following example illustrates a simple use case of
 * secondary loops:
 *
 * <pre>
 *   SecondaryLoop loop;
 *
 *   JButton jButton = new JButton("Button");
 *   jButton.addActionListener(new ActionListener() {
 *       {@code @Override}
 *       public void actionPerformed(ActionEvent e) {
 *           Toolkit tk = Toolkit.getDefaultToolkit();
 *           EventQueue eq = tk.getSystemEventQueue();
 *           loop = eq.createSecondaryLoop();
 *
 *           // Spawn a new thread to do the work
 *           Thread worker = new WorkerThread();
 *           worker.start();
 *
 *           // Enter the loop to block the current event
 *           // handler, but leave UI responsive
 *           if (!loop.enter()) {
 *               // Report an error
 *           }
 *       }
 *   });
 *
 *   class WorkerThread extends Thread {
 *       {@code @Override}
 *       public void run() {
 *           // Perform calculations
 *           doSomethingUseful();
 *
 *           // Exit the loop
 *           loop.exit();
 *       }
 *   }
 * </pre>
 *
 * @see Dialog#show
 * @see EventQueue#createSecondaryLoop
 * @see Toolkit#getSystemEventQueue
 *
 * @author Anton Tarasov, Artem Ananiev
 *
 * @since 1.7
 */
public interface SecondaryLoop {

    /**
     * Blocks the execution of the current thread and enters a new
     * secondary event loop on the event dispatch thread.
     * <p>
     * This method can be called by any thread including the event
     * dispatch thread. This thread will be blocked until the {@link
     * #exit} method is called or the loop is terminated. A new
     * secondary loop will be created on the event dispatch thread
     * for dispatching events in either case.
     * <p>
     * This method can only start one new event loop at a time per
     * object. If a secondary event loop has already been started
     * by this object and is currently still running, this method
     * returns {@code false} to indicate that it was not successful
     * in starting a new event loop. Otherwise, this method blocks
     * the calling thread and later returns {@code true} when the
     * new event loop is terminated. At such time, this object can
     * again be used to start another new event loop.
     *
     * @return {@code true} after termination of the secondary loop,
     *         if the secondary loop was started by this call,
     *         {@code false} otherwise
     */
    public boolean enter();

    /**
     * Unblocks the execution of the thread blocked by the {@link
     * #enter} method and exits the secondary loop.
     * <p>
     * This method resumes the thread that called the {@link #enter}
     * method and exits the secondary loop that was created when
     * the {@link #enter} method was invoked.
     * <p>
     * Note that if any other secondary loop is started while this
     * loop is running, the blocked thread will not resume execution
     * until the nested loop is terminated.
     * <p>
     * If this secondary loop has not been started with the {@link
     * #enter} method, or this secondary loop has already finished
     * with the {@link #exit} method, this method returns {@code
     * false}, otherwise {@code true} is returned.
     *
     * @return {@code true} if this loop was previously started and
     *         has not yet been finished with the {@link #exit} method,
     *         {@code false} otherwise
     */
    public boolean exit();

}
