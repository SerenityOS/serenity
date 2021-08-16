/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package idea;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import java.util.EnumSet;
import java.util.Stack;

import static org.apache.tools.ant.Project.*;

/**
 * This class is used to wrap the IntelliJ ant logger in order to provide more meaningful
 * output when building langtools. The basic ant output in IntelliJ can be quite cumbersome to
 * work with, as it provides two separate views: (i) a tree view, which is good to display build task
 * in a hierarchical fashion as they are processed; and a (ii) plain text view, which gives you
 * the full ant output. The main problem is that javac-related messages are buried into the
 * ant output (which is made very verbose by IntelliJ in order to support the tree view). It is
 * not easy to figure out which node to expand in order to see the error message; switching
 * to plain text doesn't help either, as now the output is totally flat.
 *
 * This logger class removes a lot of verbosity from the IntelliJ ant logger by not propagating
 * all the events to the IntelliJ's logger. In addition, certain events are handled in a custom
 * fashion, to generate better output during the build.
 */
public final class LangtoolsIdeaAntLogger extends DefaultLogger {

    /**
     * This is just a way to pass in customized binary string predicates;
     *
     * TODO: replace with @code{BiPredicate<String, String>} and method reference when moving to 8
     */
    enum StringBinaryPredicate {
        CONTAINS() {
            @Override
            boolean apply(String s1, String s2) {
                return s1.contains(s2);
            }
        },
        STARTS_WITH {
            @Override
            boolean apply(String s1, String s2) {
                return s1.startsWith(s2);
            }
        };

        abstract boolean apply(String s1, String s2);
    }

    /**
     * Various kinds of ant messages that we shall intercept
     */
    enum MessageKind {

        /** a javac error */
        JAVAC_ERROR(StringBinaryPredicate.CONTAINS, MSG_ERR, "error:", "compiler.err"),
        /** a javac warning */
        JAVAC_WARNING(StringBinaryPredicate.CONTAINS, MSG_WARN, "warning:", "compiler.warn"),
        /** a javac note */
        JAVAC_NOTE(StringBinaryPredicate.CONTAINS, MSG_INFO, "note:", "compiler.note"),
        /** a javac raw error (these typically come from a build misconfiguration - such as a bad javac flag) */
        JAVAC_RAW_ERROR(StringBinaryPredicate.STARTS_WITH, MSG_INFO, "javac: "),
        /** continuation of some javac error message */
        JAVAC_NESTED_DIAG(StringBinaryPredicate.STARTS_WITH, MSG_INFO, "  "),
        /** a javac crash */
        JAVAC_CRASH(StringBinaryPredicate.STARTS_WITH, MSG_ERR, "An exception has occurred in the compiler"),
        /** jtreg test success */
        JTREG_TEST_PASSED(StringBinaryPredicate.STARTS_WITH, MSG_INFO, "Passed: "),
        /** jtreg test failure */
        JTREG_TEST_FAILED(StringBinaryPredicate.STARTS_WITH, MSG_ERR, "FAILED: "),
        /** jtreg test error */
        JTREG_TEST_ERROR(StringBinaryPredicate.STARTS_WITH, MSG_ERR, "Error: "),
        /** jtreg report */
        JTREG_TEST_REPORT(StringBinaryPredicate.STARTS_WITH, MSG_INFO, "Report written");

        StringBinaryPredicate sbp;
        int priority;
        String[] keys;

        MessageKind(StringBinaryPredicate sbp, int priority, String... keys) {
            this.sbp = sbp;
            this.priority = priority;
            this.keys = keys;
        }

        /**
         * Does a given message string matches this kind?
         */
        boolean matches(String s) {
            for (String key : keys) {
                if (sbp.apply(s, key)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This enum is used to represent the list of tasks we need to keep track of during logging.
     */
    enum Task {
        /** exec task - invoked during compilation */
        JAVAC("exec", MessageKind.JAVAC_ERROR, MessageKind.JAVAC_WARNING, MessageKind.JAVAC_NOTE,
                       MessageKind.JAVAC_RAW_ERROR, MessageKind.JAVAC_NESTED_DIAG, MessageKind.JAVAC_CRASH),
        /** jtreg task - invoked during test execution */
        JTREG("jtreg", MessageKind.JTREG_TEST_PASSED, MessageKind.JTREG_TEST_FAILED, MessageKind.JTREG_TEST_ERROR, MessageKind.JTREG_TEST_REPORT),
        /** initial synthetic task when the logger is created */
        ROOT("") {
            @Override
            boolean matches(String s) {
                return false;
            }
        },
        /** synthetic task catching any other tasks not in this list */
        ANY("") {
            @Override
            boolean matches(String s) {
                return true;
            }
        };

        String taskName;
        MessageKind[] msgs;

        Task(String taskName, MessageKind... msgs) {
            this.taskName = taskName;
            this.msgs = msgs;
        }

        boolean matches(String s) {
            return s.equals(taskName);
        }
    }

    /**
     * This enum is used to represent the list of targets we need to keep track of during logging.
     * A regular expression is used to match a given target name.
     */
    enum Target {
        /** jtreg target - executed when launching tests */
        JTREG("jtreg") {
            @Override
            String getDisplayMessage(BuildEvent e) {
                return "Running jtreg tests: " + e.getProject().getProperty("jtreg.tests");
            }
        },
        /** build bootstrap tool target - executed when bootstrapping javac */
        BUILD_BOOTSTRAP_JAVAC("build-bootstrap-javac-classes") {
            @Override
            String getDisplayMessage(BuildEvent e) {
                return "Building bootstrap javac...";
            }
        },
        /** build classes target - executed when building classes of given tool */
        BUILD_ALL_CLASSES("build-all-classes") {
            @Override
            String getDisplayMessage(BuildEvent e) {
                return "Building all classes...";
            }
        },
        /** synthetic target catching any other target not in this list */
        ANY("") {
            @Override
            String getDisplayMessage(BuildEvent e) {
                return "Executing Ant target(s): " + e.getProject().getProperty("ant.project.invoked-targets");
            }
            @Override
            boolean matches(String msg) {
                return true;
            }
        };

        String targetName;

        Target(String targetName) {
            this.targetName = targetName;
        }

        boolean matches(String msg) {
            return msg.equals(targetName);
        }

        abstract String getDisplayMessage(BuildEvent e);
    }

    /**
     * A custom build event used to represent status changes which should be notified inside
     * Intellij
     */
    static class StatusEvent extends BuildEvent {

        /** the target to which the status update refers */
        Target target;

        StatusEvent(BuildEvent e, Target target) {
            super(new StatusTask(e, target.getDisplayMessage(e)));
            this.target = target;
            setMessage(getTask().getTaskName(), 2);
        }

        /**
         * A custom task used to channel info regarding a status change
         */
        static class StatusTask extends org.apache.tools.ant.Task {
            StatusTask(BuildEvent event, String msg) {
                setProject(event.getProject());
                setOwningTarget(event.getTarget());
                setTaskName(msg);
            }
        }
    }

    /** wrapped ant logger (IntelliJ's own logger) */
    DefaultLogger logger;

    /** flag - is this the first target we encounter? */
    boolean firstTarget = true;

    /** flag - should subsequenet failures be suppressed ? */
    boolean suppressTaskFailures = false;

    /** flag - have we ran into a javac crash ? */
    boolean crashFound = false;

    /** stack of status changes associated with pending targets */
    Stack<StatusEvent> statusEvents = new Stack<>();

    /** stack of pending tasks */
    Stack<Task> tasks = new Stack<>();

    public LangtoolsIdeaAntLogger(Project project) {
        for (Object o : project.getBuildListeners()) {
            if (o instanceof DefaultLogger) {
                this.logger = (DefaultLogger)o;
                project.removeBuildListener((BuildListener)o);
                project.addBuildListener(this);
            }
        }
        logger.setMessageOutputLevel(3);
        tasks.push(Task.ROOT);
    }

    @Override
    public void buildStarted(BuildEvent event) {
        //do nothing
    }

    @Override
    public void buildFinished(BuildEvent event) {
        //do nothing
    }

    @Override
    public void targetStarted(BuildEvent event) {
        EnumSet<Target> statusKinds = firstTarget ?
                EnumSet.allOf(Target.class) :
                EnumSet.complementOf(EnumSet.of(Target.ANY));

        String targetName = event.getTarget().getName();

        for (Target statusKind : statusKinds) {
            if (statusKind.matches(targetName)) {
                StatusEvent statusEvent = new StatusEvent(event, statusKind);
                statusEvents.push(statusEvent);
                logger.taskStarted(statusEvent);
                firstTarget = false;
                return;
            }
        }
    }

    @Override
    public void targetFinished(BuildEvent event) {
        if (!statusEvents.isEmpty()) {
            StatusEvent lastEvent = statusEvents.pop();
            if (lastEvent.target.matches(event.getTarget().getName())) {
                logger.taskFinished(lastEvent);
            }
        }
    }

    @Override
    public void taskStarted(BuildEvent event) {
        String taskName = event.getTask().getTaskName();
        for (Task task : Task.values()) {
            if (task.matches(taskName)) {
                tasks.push(task);
                return;
            }
        }
    }

    @Override
    public void taskFinished(BuildEvent event) {
        if (tasks.peek() == Task.ROOT) {
            //we need to 'close' the root task to get nicer output
            logger.taskFinished(event);
        } else if (!suppressTaskFailures && event.getException() != null) {
            //the first (innermost) task failure should always be logged
            event.setMessage(event.getException().toString(), 0);
            event.setException(null);
            //note: we turn this into a plain message to avoid stack trace being logged by Idea
            logger.messageLogged(event);
            suppressTaskFailures = true;
        }
        tasks.pop();
    }

    @Override
    public void messageLogged(BuildEvent event) {
        String msg = event.getMessage();

        boolean processed = false;

        if (!tasks.isEmpty()) {
            Task task = tasks.peek();
            for (MessageKind messageKind : task.msgs) {
                if (messageKind.matches(msg)) {
                    event.setMessage(msg, messageKind.priority);
                    processed = true;
                    if (messageKind == MessageKind.JAVAC_CRASH) {
                        crashFound = true;
                    }
                    break;
                }
            }
        }

        if (event.getPriority() == MSG_ERR || crashFound) {
            //we log errors regardless of owning task
            logger.messageLogged(event);
            suppressTaskFailures = true;
        } else if (processed) {
            logger.messageLogged(event);
        }
    }
}
