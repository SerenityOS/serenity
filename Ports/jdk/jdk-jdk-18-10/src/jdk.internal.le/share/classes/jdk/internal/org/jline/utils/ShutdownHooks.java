/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the JLine shutdown-hook thread and tasks to execute on shutdown.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.7
 */
public final class ShutdownHooks
{
    private static final List<Task> tasks = new ArrayList<>();

    private static Thread hook;

    public static synchronized <T extends Task> T add(final T task) {
        Objects.requireNonNull(task);

        // Install the hook thread if needed
        if (hook == null) {
            hook = addHook(new Thread("JLine Shutdown Hook")
            {
                @Override
                public void run() {
                    runTasks();
                }
            });
        }

        // Track the task
        Log.debug("Adding shutdown-hook task: ", task);
        tasks.add(task);

        return task;
    }

    private static synchronized void runTasks() {
        Log.debug("Running all shutdown-hook tasks");

        // Iterate through copy of tasks list
        for (Task task : tasks.toArray(new Task[tasks.size()])) {
            Log.debug("Running task: ", task);
            try {
                task.run();
            }
            catch (Throwable e) {
                Log.warn("Task failed", e);
            }
        }

        tasks.clear();
    }

    private static Thread addHook(final Thread thread) {
        Log.debug("Registering shutdown-hook: ", thread);
        Runtime.getRuntime().addShutdownHook(thread);
        return thread;
    }

    public static synchronized void remove(final Task task) {
        Objects.requireNonNull(task);

        // ignore if hook never installed
        if (hook == null) {
            return;
        }

        // Drop the task
        tasks.remove(task);

        // If there are no more tasks, then remove the hook thread
        if (tasks.isEmpty()) {
            removeHook(hook);
            hook = null;
        }
    }

    private static void removeHook(final Thread thread) {
        Log.debug("Removing shutdown-hook: ", thread);

        try {
            Runtime.getRuntime().removeShutdownHook(thread);
        }
        catch (IllegalStateException e) {
            // The VM is shutting down, not a big deal; ignore
        }
    }

    /**
     * Essentially a {@link Runnable} which allows running to throw an exception.
     */
    public interface Task
    {
        void run() throws Exception;
    }
}
