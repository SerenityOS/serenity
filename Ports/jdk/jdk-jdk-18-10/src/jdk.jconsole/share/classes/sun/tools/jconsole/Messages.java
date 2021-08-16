/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.tools.jconsole;


/**
 * Class that contains localized messages.
 *
 */
public final class Messages {
    private static final String BUNDLE_NAME = "sun.tools.jconsole.resources.messages";

    static {
        Resources.initializeMessages(Messages.class, BUNDLE_NAME);
    }
    // TODO:
    // The names of some of the constants below look strange.
    // That's because they  were generated programmatically
    // from the messages. They should be cleaned up,
    // ___ should be removed etc.
    public static String ONE_DAY;
    public static String ONE_HOUR;
    public static String ONE_MIN;
    public static String ONE_MONTH;
    public static String ONE_YEAR;
    public static String TWO_HOURS;
    public static String THREE_HOURS;
    public static String THREE_MONTHS;
    public static String FIVE_MIN;
    public static String SIX_HOURS;
    public static String SIX_MONTHS;
    public static String SEVEN_DAYS;
    public static String TEN_MIN;
    public static String TWELVE_HOURS;
    public static String THIRTY_MIN;
    public static String LESS_THAN;
    public static String A_LOT_LESS_THAN;
    public static String GREATER_THAN;
    public static String ACTION_CAPITALIZED;
    public static String ACTION_INFO_CAPITALIZED;
    public static String ALL;
    public static String ARCHITECTURE;
    public static String ATTRIBUTE;
    public static String ATTRIBUTE_VALUE;
    public static String ATTRIBUTE_VALUES;
    public static String ATTRIBUTES;
    public static String BLANK;
    public static String BLOCKED_COUNT_WAITED_COUNT;
    public static String BOOT_CLASS_PATH;
    public static String BORDERED_COMPONENT_MORE_OR_LESS_BUTTON_TOOLTIP;
    public static String CPU_USAGE;
    public static String CPU_USAGE_FORMAT;
    public static String CANCEL;
    public static String CASCADE;
    public static String CHART_COLON;
    public static String CLASS_PATH;
    public static String CLASS_NAME;
    public static String CLASS_TAB_INFO_LABEL_FORMAT;
    public static String CLASS_TAB_LOADED_CLASSES_PLOTTER_ACCESSIBLE_NAME;
    public static String CLASSES;
    public static String CLOSE;
    public static String COLUMN_NAME;
    public static String COLUMN_PID;
    public static String COMMITTED_MEMORY;
    public static String COMMITTED_VIRTUAL_MEMORY;
    public static String COMMITTED;
    public static String CONNECT;
    public static String CONNECT_DIALOG_CONNECT_BUTTON_TOOLTIP;
    public static String CONNECT_DIALOG_ACCESSIBLE_DESCRIPTION;
    public static String CONNECT_DIALOG_MASTHEAD_ACCESSIBLE_NAME;
    public static String CONNECT_DIALOG_MASTHEAD_TITLE;
    public static String CONNECT_DIALOG_STATUS_BAR_ACCESSIBLE_NAME;
    public static String CONNECT_DIALOG_TITLE;
    public static String CONNECTED_PUNCTUATION_CLICK_TO_DISCONNECT_;
    public static String CONNECTION_FAILED;
    public static String CONNECTION;
    public static String CONNECTION_NAME;
    public static String CONNECTION_NAME__DISCONNECTED_;
    public static String CONSTRUCTOR;
    public static String CURRENT_CLASSES_LOADED;
    public static String CURRENT_HEAP_SIZE;
    public static String CURRENT_VALUE;
    public static String CREATE;
    public static String DAEMON_THREADS;
    public static String DISCONNECTED_PUNCTUATION_CLICK_TO_CONNECT_;
    public static String DOUBLE_CLICK_TO_EXPAND_FORWARD_SLASH_COLLAPSE;
    public static String DOUBLE_CLICK_TO_VISUALIZE;
    public static String DESCRIPTION;
    public static String DESCRIPTOR;
    public static String DETAILS;
    public static String DETECT_DEADLOCK;
    public static String DETECT_DEADLOCK_TOOLTIP;
    public static String DIMENSION_IS_NOT_SUPPORTED_COLON;
    public static String DISCARD_CHART;
    public static String DURATION_DAYS_HOURS_MINUTES;
    public static String DURATION_HOURS_MINUTES;
    public static String DURATION_MINUTES;
    public static String DURATION_SECONDS;
    public static String EMPTY_ARRAY;
    public static String ERROR;
    public static String ERROR_COLON_MBEANS_ALREADY_EXIST;
    public static String ERROR_COLON_MBEANS_DO_NOT_EXIST;
    public static String EVENT;
    public static String EXIT;
    public static String FAIL_TO_LOAD_PLUGIN;
    public static String FILE_CHOOSER_FILE_EXISTS_CANCEL_OPTION;
    public static String FILE_CHOOSER_FILE_EXISTS_MESSAGE;
    public static String FILE_CHOOSER_FILE_EXISTS_OK_OPTION;
    public static String FILE_CHOOSER_FILE_EXISTS_TITLE;
    public static String FILE_CHOOSER_SAVED_FILE;
    public static String FILE_CHOOSER_SAVE_FAILED_MESSAGE;
    public static String FILE_CHOOSER_SAVE_FAILED_TITLE;
    public static String FREE_PHYSICAL_MEMORY;
    public static String FREE_SWAP_SPACE;
    public static String GARBAGE_COLLECTOR;
    public static String GC_INFO;
    public static String GC_TIME;
    public static String GC_TIME_DETAILS;
    public static String HEAP_MEMORY_USAGE;
    public static String HEAP;
    public static String HELP_ABOUT_DIALOG_ACCESSIBLE_DESCRIPTION;
    public static String HELP_ABOUT_DIALOG_JCONSOLE_VERSION;
    public static String HELP_ABOUT_DIALOG_JAVA_VERSION;
    public static String HELP_ABOUT_DIALOG_MASTHEAD_ACCESSIBLE_NAME;
    public static String HELP_ABOUT_DIALOG_MASTHEAD_TITLE;
    public static String HELP_ABOUT_DIALOG_TITLE;
    public static String HELP_ABOUT_DIALOG_USER_GUIDE_LINK_URL;
    public static String HELP_MENU_ABOUT_TITLE;
    public static String HELP_MENU_USER_GUIDE_TITLE;
    public static String HELP_MENU_TITLE;
    public static String HOTSPOT_MBEANS_ELLIPSIS;
    public static String HOTSPOT_MBEANS_DIALOG_ACCESSIBLE_DESCRIPTION;
    public static String IMPACT;
    public static String INFO;
    public static String INFO_CAPITALIZED;
    public static String INSECURE;
    public static String INVALID_PLUGIN_PATH;
    public static String INVALID_URL;
    public static String IS;
    public static String JAVA_MONITORING___MANAGEMENT_CONSOLE;
    public static String JCONSOLE_COLON_;
    public static String JCONSOLE_VERSION; // in version template
    public static String JCONSOLE_ACCESSIBLE_DESCRIPTION;
    public static String JIT_COMPILER;
    public static String LIBRARY_PATH;
    public static String LIVE_THREADS;
    public static String LOADED;
    public static String LOCAL_PROCESS_COLON;
    public static String MASTHEAD_FONT;
    public static String MANAGEMENT_NOT_ENABLED;
    public static String MANAGEMENT_WILL_BE_ENABLED;
    public static String MBEAN_ATTRIBUTE_INFO;
    public static String MBEAN_INFO;
    public static String MBEAN_NOTIFICATION_INFO;
    public static String MBEAN_OPERATION_INFO;
    public static String MBEANS;
    public static String MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON;
    public static String MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON_TOOLTIP;
    public static String MBEANS_TAB_COMPOSITE_NAVIGATION_MULTIPLE;
    public static String MBEANS_TAB_COMPOSITE_NAVIGATION_SINGLE;
    public static String MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON;
    public static String MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON_TOOLTIP;
    public static String MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON;
    public static String MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP;
    public static String MBEANS_TAB_TABULAR_NAVIGATION_MULTIPLE;
    public static String MBEANS_TAB_TABULAR_NAVIGATION_SINGLE;
    public static String MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON;
    public static String MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP;
    public static String MANAGE_HOTSPOT_MBEANS_IN_COLON_;
    public static String MAX;
    public static String MAXIMUM_HEAP_SIZE;
    public static String MEMORY;
    public static String MEMORY_POOL_LABEL;
    public static String MEMORY_TAB_HEAP_PLOTTER_ACCESSIBLE_NAME;
    public static String MEMORY_TAB_INFO_LABEL_FORMAT;
    public static String MEMORY_TAB_NON_HEAP_PLOTTER_ACCESSIBLE_NAME;
    public static String MEMORY_TAB_POOL_CHART_ABOVE_THRESHOLD;
    public static String MEMORY_TAB_POOL_CHART_ACCESSIBLE_NAME;
    public static String MEMORY_TAB_POOL_CHART_BELOW_THRESHOLD;
    public static String MEMORY_TAB_POOL_PLOTTER_ACCESSIBLE_NAME;
    public static String MESSAGE;
    public static String METHOD_SUCCESSFULLY_INVOKED;
    public static String MINIMIZE_ALL;
    public static String MONITOR_LOCKED;
    public static String NAME;
    public static String NAME_STATE;
    public static String NAME_STATE_LOCK_NAME;
    public static String NAME_STATE_LOCK_NAME_LOCK_OWNER;
    public static String NAME_AND_BUILD;// in version template
    public static String NEW_CONNECTION_ELLIPSIS;
    public static String NO_DEADLOCK_DETECTED;
    public static String NON_HEAP_MEMORY_USAGE;
    public static String NON_HEAP;
    public static String NOTIFICATION;
    public static String NOTIFICATION_BUFFER;
    public static String NOTIFICATIONS;
    public static String NOTIF_TYPES;
    public static String NUMBER_OF_THREADS;
    public static String NUMBER_OF_LOADED_CLASSES;
    public static String NUMBER_OF_PROCESSORS;
    public static String OBJECT_NAME;
    public static String OPERATING_SYSTEM;
    public static String OPERATION;
    public static String OPERATION_INVOCATION;
    public static String OPERATION_RETURN_VALUE;
    public static String OPERATIONS;
    public static String OVERVIEW;
    public static String OVERVIEW_PANEL_PLOTTER_ACCESSIBLE_NAME;
    public static String PARAMETER;
    public static String PASSWORD_COLON_;
    public static String PASSWORD_ACCESSIBLE_NAME;
    public static String PEAK;
    public static String PERFORM_GC;
    public static String PERFORM_GC_TOOLTIP;
    public static String PLOTTER_ACCESSIBLE_NAME;
    public static String PLOTTER_ACCESSIBLE_NAME_KEY_AND_VALUE;
    public static String PLOTTER_ACCESSIBLE_NAME_NO_DATA;
    public static String PLOTTER_SAVE_AS_MENU_ITEM;
    public static String PLOTTER_TIME_RANGE_MENU;
    public static String PLUGIN_EXCEPTION_DIALOG_BUTTON_EXIT;
    public static String PLUGIN_EXCEPTION_DIALOG_BUTTON_IGNORE;
    public static String PLUGIN_EXCEPTION_DIALOG_BUTTON_OK;
    public static String PLUGIN_EXCEPTION_DIALOG_MESSAGE;
    public static String PLUGIN_EXCEPTION_DIALOG_TITLE;
    public static String PROBLEM_ADDING_LISTENER;
    public static String PROBLEM_DISPLAYING_MBEAN;
    public static String PROBLEM_INVOKING;
    public static String PROBLEM_REMOVING_LISTENER;
    public static String PROBLEM_SETTING_ATTRIBUTE;
    public static String PROCESS_CPU_TIME;
    public static String READABLE;
    public static String RECONNECT;
    public static String REMOTE_PROCESS_COLON;
    public static String REMOTE_PROCESS_TEXT_FIELD_ACCESSIBLE_NAME;
    public static String RESTORE_ALL;
    public static String RETURN_TYPE;
    public static String SEQ_NUM;
    public static String SIZE_BYTES;
    public static String SIZE_GB;
    public static String SIZE_KB;
    public static String SIZE_MB;
    public static String SOURCE;
    public static String STACK_TRACE;
    public static String SUMMARY_TAB_HEADER_DATE_TIME_FORMAT;
    public static String SUMMARY_TAB_PENDING_FINALIZATION_LABEL;
    public static String SUMMARY_TAB_PENDING_FINALIZATION_VALUE;
    public static String SUMMARY_TAB_TAB_NAME;
    public static String SUMMARY_TAB_VM_VERSION;
    public static String THREADS;
    public static String THREAD_TAB_INFO_LABEL_FORMAT;
    public static String THREAD_TAB_THREAD_INFO_ACCESSIBLE_NAME;
    public static String THREAD_TAB_THREAD_PLOTTER_ACCESSIBLE_NAME;
    public static String THREAD_TAB_INITIAL_STACK_TRACE_MESSAGE;
    public static String THRESHOLD;
    public static String TILE;
    public static String TIME_RANGE_COLON;
    public static String TIME;
    public static String TIME_STAMP;
    public static String TOTAL_LOADED;
    public static String TOTAL_CLASSES_LOADED;
    public static String TOTAL_CLASSES_UNLOADED;
    public static String TOTAL_COMPILE_TIME;
    public static String TOTAL_PHYSICAL_MEMORY;
    public static String TOTAL_THREADS_STARTED;
    public static String TOTAL_SWAP_SPACE;
    public static String TYPE;
    public static String UNAVAILABLE;
    public static String UNKNOWN_CAPITALIZED;
    public static String UNKNOWN_HOST;
    public static String UNREGISTER;
    public static String UPTIME;
    public static String USAGE_THRESHOLD;
    public static String REMOTE_TF_USAGE;
    public static String USED;
    public static String USERNAME_COLON_;
    public static String USERNAME_ACCESSIBLE_NAME;
    public static String USER_DATA;
    public static String VIRTUAL_MACHINE;
    public static String VM_ARGUMENTS;
    public static String VMINTERNAL_FRAME_ACCESSIBLE_DESCRIPTION;
    public static String VALUE;
    public static String VENDOR;
    public static String VERBOSE_OUTPUT;
    public static String VERBOSE_OUTPUT_TOOLTIP;
    public static String VIEW;
    public static String WINDOW;
    public static String WINDOWS;
    public static String WRITABLE;
    public static String CONNECTION_FAILED1;
    public static String CONNECTION_FAILED2;
    public static String CONNECTION_FAILED_SSL1;
    public static String CONNECTION_FAILED_SSL2;
    public static String CONNECTION_LOST1;
    public static String CONNECTING_TO1;
    public static String CONNECTING_TO2;
    public static String DEADLOCK_TAB;
    public static String DEADLOCK_TAB_N;
    public static String EXPAND;
    public static String KBYTES;
    public static String PLOT;
    public static String VISUALIZE;
    public static String ZZ_USAGE_TEXT;
}
