/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.jshell.tool;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.internal.jshell.tool.JShellTool.CompletionProvider;

import static java.util.stream.Collectors.*;
import static jdk.internal.jshell.tool.ContinuousCompletionProvider.PERFECT_MATCHER;
import static jdk.internal.jshell.tool.JShellTool.EMPTY_COMPLETION_PROVIDER;
import static jdk.internal.jshell.tool.Selector.SelectorKind;
import static jdk.internal.jshell.tool.Selector.SelectorInstanceWithDoc;
import static jdk.internal.jshell.tool.Selector.SelectorBuilder;
import static jdk.internal.jshell.tool.Selector.FormatAction;
import static jdk.internal.jshell.tool.Selector.FormatCase;
import static jdk.internal.jshell.tool.Selector.FormatErrors;
import static jdk.internal.jshell.tool.Selector.FormatResolve;
import static jdk.internal.jshell.tool.Selector.FormatUnresolved;
import static jdk.internal.jshell.tool.Selector.FormatWhen;


/**
 * Feedback customization support
 *
 * @author Robert Field
 */
class Feedback {

    // Patern for substituted fields within a customized format string
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\{(.*?)\\}");

    // Internal field name for truncation length
    private static final String TRUNCATION_FIELD = "<truncation>";

    // For encoding to Properties String
    private static final String RECORD_SEPARATOR = "\u241E";

    // Selector for truncation of var value
    private static final Selector VAR_VALUE_ADD_SELECTOR = new Selector(
            FormatCase.VARVALUE,
            FormatAction.ADDED,
            FormatWhen.PRIMARY,
            FormatResolve.OK,
            FormatUnresolved.UNRESOLVED0,
            FormatErrors.ERROR0);

    // Selector for typeKind record
    private static final Selector RECORD_TYPEKIND_SELECTOR = new Selector(
            EnumSet.of(FormatCase.RECORD),
            EnumSet.noneOf(FormatAction.class),
            EnumSet.noneOf(FormatWhen.class),
            EnumSet.noneOf(FormatResolve.class),
            EnumSet.noneOf(FormatUnresolved.class),
            EnumSet.noneOf(FormatErrors.class));

    // Current mode -- initial value is placeholder during start-up
    private Mode mode = new Mode("");

    // Retained current mode -- for checks
    private Mode retainedCurrentMode = null;

    // Mapping of mode name to mode
    private final Map<String, Mode> modeMap = new HashMap<>();

    // Mapping of mode names to encoded retained mode
    private final Map<String, String> retainedMap = new HashMap<>();

    public boolean shouldDisplayCommandFluff() {
        return mode.commandFluff;
    }

    public String getPre() {
        return mode.format("pre", Selector.ANY);
    }

    public String getPost() {
        return mode.format("post", Selector.ANY);
    }

    public String getErrorPre() {
        return mode.format("errorpre", Selector.ANY);
    }

    public String getErrorPost() {
        return mode.format("errorpost", Selector.ANY);
    }

    public String format(FormatCase fc, FormatAction fa, FormatWhen fw,
                         FormatResolve fr, FormatUnresolved fu, FormatErrors fe,
                         String name, String type, String value, String unresolved, List<String> errorLines) {
        return mode.format(fc, fa, fw, fr, fu, fe,
                name, type, value, unresolved, errorLines);
    }

    public String format(String field, FormatCase fc, FormatAction fa, FormatWhen fw,
                         FormatResolve fr, FormatUnresolved fu, FormatErrors fe,
                         String name, String type, String value, String unresolved, List<String> errorLines) {
        return mode.format(field, fc, fa, fw, fr, fu, fe,
                name, type, value, unresolved, errorLines);
    }

    public String truncateVarValue(String value) {
        return mode.truncateVarValue(value);
    }

    public String getPrompt(String nextId) {
        return mode.getPrompt(nextId);
    }

    public String getContinuationPrompt(String nextId) {
        return mode.getContinuationPrompt(nextId);
    }

    public boolean setFeedback(MessageHandler messageHandler, ArgTokenizer at, Consumer<String> retainer) {
        return new Setter(messageHandler, at).setFeedback(retainer);
    }

    public boolean setFormat(MessageHandler messageHandler, ArgTokenizer at) {
        return new Setter(messageHandler, at).setFormat();
    }

    public boolean setTruncation(MessageHandler messageHandler, ArgTokenizer at) {
        return new Setter(messageHandler, at).setTruncation();
    }

    public boolean setMode(MessageHandler messageHandler, ArgTokenizer at, Consumer<String> retainer) {
        return new Setter(messageHandler, at).setMode(retainer);
    }

    public boolean setPrompt(MessageHandler messageHandler, ArgTokenizer at) {
        return new Setter(messageHandler, at).setPrompt();
    }

    public boolean restoreEncodedModes(MessageHandler messageHandler, String encoded) {
        return new Setter(messageHandler, new ArgTokenizer("<init>", "")).restoreEncodedModes(encoded);
    }

    public void markModesReadOnly() {
        modeMap.values().stream()
                .forEach(m -> m.readOnly = true);
    }

    JShellTool.CompletionProvider modeCompletions() {
        return modeCompletions(EMPTY_COMPLETION_PROVIDER);
    }

    JShellTool.CompletionProvider modeCompletions(CompletionProvider successor) {
        return new ContinuousCompletionProvider(
                () -> modeMap.keySet().stream()
                        .collect(toMap(Function.identity(), m -> successor)),
                PERFECT_MATCHER);
    }

    /**
     * Holds all the context of a mode mode
     */
    private static class Mode {

        // Name of mode
        final String name;

        // Display command verification/information
        boolean commandFluff;

        // Setting (including format) by field
        final Map<String, List<Setting>> byField;

        boolean readOnly = false;

        String prompt = "\n-> ";
        String continuationPrompt = ">> ";

        static class Setting {

            final String format;
            final Selector selector;

            Setting(String format, Selector selector) {
                this.format = format;
                this.selector = selector;
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof Setting) {
                    Setting ing = (Setting) o;
                    return format.equals(ing.format)
                            && selector.equals(ing.selector);
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 67 * hash + Objects.hashCode(this.selector);
                hash = 67 * hash + Objects.hashCode(this.format);
                return hash;
            }

            @Override
            public String toString() {
                return "Setting(" + format + "," + selector.toString() + ")";
            }
        }

        /**
         * Set up an empty mode.
         *
         * @param name
         */
        Mode(String name) {
            this.name = name;
            this.byField = new HashMap<>();
            set("name", "%1$s", Selector.ALWAYS);
            set("type", "%2$s", Selector.ALWAYS);
            set("value", "%3$s", Selector.ALWAYS);
            set("unresolved", "%4$s", Selector.ALWAYS);
            set("errors", "%5$s", Selector.ALWAYS);
            set("err", "%6$s", Selector.ALWAYS);

            set("errorline", "    {err}%n", Selector.ALWAYS);

            set("pre", "|  ", Selector.ALWAYS);
            set("post", "%n", Selector.ALWAYS);
            set("errorpre", "|  ", Selector.ALWAYS);
            set("errorpost", "%n", Selector.ALWAYS);
        }

        private Mode(String name, boolean commandFluff, String prompt, String continuationPrompt) {
            this.name = name;
            this.commandFluff = commandFluff;
            this.prompt = prompt;
            this.continuationPrompt = continuationPrompt;
            this.byField = new HashMap<>();
        }

        /**
         * Set up a copied mode.
         *
         * @param name
         * @param m    Mode to copy, or null for no fresh
         */
        Mode(String name, Mode m) {
            this(name, m.commandFluff, m.prompt, m.continuationPrompt);
            m.byField.forEach((fieldName, settingList) ->
                    settingList.forEach(setting -> set(fieldName, setting.format, setting.selector)));

        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Mode) {
                Mode m = (Mode) o;
                return name.equals((m.name))
                        && commandFluff == m.commandFluff
                        && prompt.equals((m.prompt))
                        && continuationPrompt.equals((m.continuationPrompt))
                        && byField.equals((m.byField));
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        /**
         * Set if this mode displays informative/confirmational messages on
         * commands.
         *
         * @param fluff the value to set
         */
        void setCommandFluff(boolean fluff) {
            commandFluff = fluff;
        }

        /**
         * Encodes the mode into a String so it can be saved in Preferences.
         *
         * @return the string representation
         */
        String encode() {
            List<String> el = new ArrayList<>();
            el.add(name);
            el.add(String.valueOf(commandFluff));
            el.add(prompt);
            el.add(continuationPrompt);
            for (Entry<String, List<Setting>> es : byField.entrySet()) {
                el.add(es.getKey());
                el.add("(");
                for (Setting ing : es.getValue()) {
                    el.add(ing.selector.toString());
                    el.add(ing.format);
                }
                el.add(")");
            }
            el.add("***");
            return String.join(RECORD_SEPARATOR, el);
        }

        private void add(String field, Setting ing) {
            List<Setting> settings = byField.get(field);
            if (settings == null) {
                settings = new ArrayList<>();
                byField.put(field, settings);
            } else {
                // remove completely obscured settings.
                // transformation of partially obscured would be confusing to user and complex
                Selector addedSelector = ing.selector;
                settings.removeIf(t -> t.selector.includedIn(addedSelector));
            }
            settings.add(ing);
        }

        void set(String field, String format, Selector selector) {
            add(field, new Setting(format, selector));
        }

        /**
         * Lookup format Replace fields with context specific formats.
         *
         * @return format string
         */
        String format(String field, Selector selector) {
            List<Setting> settings = byField.get(field);
            if (settings == null) {
                return ""; //TODO error?
            }
            String format = null;
            // Iterate backward, as most recent setting that covers the case is used
            for (int i = settings.size() - 1; i >= 0; --i) {
                Setting ing = settings.get(i);
                if (ing.selector.covers(selector)) {
                    format = ing.format;
                    break;
                }
            }
            if (format == null || format.isEmpty()) {
                return "";
            }
            Matcher m = FIELD_PATTERN.matcher(format);
            StringBuffer sb = new StringBuffer(format.length());
            while (m.find()) {
                String fieldName = m.group(1);
                String sub = format(fieldName, selector);
                m.appendReplacement(sb, Matcher.quoteReplacement(sub));
            }
            m.appendTail(sb);
            return sb.toString();
        }

        String truncateVarValue(String value) {
            return truncateValue(value, VAR_VALUE_ADD_SELECTOR);
        }

        String truncateValue(String value, Selector selector) {
            if (value==null) {
                return "";
            } else {
                // Retrieve the truncation length
                String truncField = format(TRUNCATION_FIELD, selector);
                if (truncField.isEmpty()) {
                    // No truncation set, use whole value
                    return value;
                } else {
                    // Convert truncation length to int
                    // this is safe since it has been tested before it is set
                    int trunc = Integer.parseUnsignedInt(truncField);
                    int len = value.length();
                    if (len > trunc) {
                        if (trunc <= 13) {
                            // Very short truncations have no room for "..."
                            return value.substring(0, trunc);
                        } else {
                            // Normal truncation, make total length equal truncation length
                            int endLen = trunc / 3;
                            int startLen = trunc - 5 - endLen;
                            return value.substring(0, startLen) + " ... " + value.substring(len -endLen);
                        }
                    } else {
                        // Within truncation length, use whole value
                        return value;
                    }
                }
            }
        }

        // Compute the display output given full context and values
        String format(FormatCase fc, FormatAction fa, FormatWhen fw,
                      FormatResolve fr, FormatUnresolved fu, FormatErrors fe,
                      String name, String type, String value, String unresolved, List<String> errorLines) {
            return format("display", fc, fa, fw, fr, fu, fe,
                name, type, value, unresolved, errorLines);
        }

        // Compute the display output given full context and values
        String format(String field, FormatCase fc, FormatAction fa, FormatWhen fw,
                      FormatResolve fr, FormatUnresolved fu, FormatErrors fe,
                      String name, String type, String value, String unresolved, List<String> errorLines) {
            // Convert the context into a bit representation used as selectors for store field formats
            Selector selector  = new Selector(fc, fa, fw, fr, fu, fe);
            String fname = name==null? "" : name;
            String ftype = type==null? "" : type;
            // Compute the representation of value
            String fvalue = truncateValue(value, selector);
            String funresolved = unresolved==null? "" : unresolved;
            String errors = errorLines.stream()
                    .map(el -> String.format(
                            format("errorline", selector),
                            fname, ftype, fvalue, funresolved, "*cannot-use-errors-here*", el))
                    .collect(joining());
            return String.format(
                    format(field, selector),
                    fname, ftype, fvalue, funresolved, errors, "*cannot-use-err-here*");
        }

        void setPrompts(String prompt, String continuationPrompt) {
            this.prompt = prompt;
            this.continuationPrompt = continuationPrompt;
        }

        String getPrompt(String nextId) {
            return String.format(prompt, nextId);
        }

        String getContinuationPrompt(String nextId) {
            return String.format(continuationPrompt, nextId);
        }
    }

    // Class used to set custom eval output formats
    // For both /set format  -- Parse arguments, setting custom format, or printing error
    private class Setter {

        private final ArgTokenizer at;
        private final MessageHandler messageHandler;
        boolean valid = true;

        Setter(MessageHandler messageHandler, ArgTokenizer at) {
            this.messageHandler = messageHandler;
            this.at = at;
            at.allowedOptions("-retain");
        }

        void fluff(String format, Object... args) {
            messageHandler.fluff(format, args);
        }

        void hard(String format, Object... args) {
            messageHandler.hard(format, args);
        }

        void fluffmsg(String messageKey, Object... args) {
            messageHandler.fluffmsg(messageKey, args);
        }

        void hardmsg(String messageKey, Object... args) {
            messageHandler.hardmsg(messageKey, args);
        }

        boolean showFluff() {
            return messageHandler.showFluff();
        }

        void errorat(String messageKey, Object... args) {
            if (!valid) {
                // no spew of errors
                return;
            }
            valid = false;
            Object[] a2 = Arrays.copyOf(args, args.length + 2);
            a2[args.length] = at.whole();
            messageHandler.errormsg(messageKey, a2);
        }

        // Show format settings -- in a predictable order, for testing...
        void showFormatSettings(Mode sm, String f) {
            if (sm == null) {
                modeMap.entrySet().stream()
                        .sorted((es1, es2) -> es1.getKey().compareTo(es2.getKey()))
                        .forEach(m -> showFormatSettings(m.getValue(), f));
            } else {
                sm.byField.entrySet().stream()
                        .filter(ec -> (f == null)
                            ? !ec.getKey().equals(TRUNCATION_FIELD)
                            : ec.getKey().equals(f))
                        .sorted((ec1, ec2) -> ec1.getKey().compareTo(ec2.getKey()))
                        .forEach(ec -> {
                            ec.getValue().forEach(s -> {
                                hard("/set format %s %s %s %s",
                                        sm.name, ec.getKey(), toStringLiteral(s.format),
                                        s.selector.toString());

                            });
                        });
            }
        }

        void showTruncationSettings(Mode sm) {
            if (sm == null) {
                modeMap.values().forEach(this::showTruncationSettings);
            } else {
                List<Mode.Setting> trunc = sm.byField.get(TRUNCATION_FIELD);
                if (trunc != null) {
                    trunc.forEach(s -> {
                        hard("/set truncation %s %s %s",
                                sm.name, s.format,
                                s.selector.toString());
                    });
                }
            }
        }

        void showPromptSettings(Mode sm) {
            if (sm == null) {
                modeMap.values().forEach(this::showPromptSettings);
            } else {
                hard("/set prompt %s %s %s",
                        sm.name,
                        toStringLiteral(sm.prompt),
                        toStringLiteral(sm.continuationPrompt));
            }
        }

        void showModeSettings(String umode, String msg) {
            if (umode == null) {
                modeMap.values().forEach(this::showModeSettings);
            } else {
                Mode m;
                String retained = retainedMap.get(umode);
                if (retained == null) {
                    m = searchForMode(umode, msg);
                    if (m == null) {
                        return;
                    }
                    umode = m.name;
                    retained = retainedMap.get(umode);
                } else {
                    m = modeMap.get(umode);
                }
                if (retained != null) {
                    Mode rm = buildMode(encodedModeIterator(retained));
                    showModeSettings(rm);
                    hard("/set mode -retain %s", umode);
                    if (m != null && !m.equals(rm)) {
                        hard("");
                        showModeSettings(m);
                    }
                } else {
                    showModeSettings(m);
                }
            }
        }

        void showModeSettings(Mode sm) {
            hard("/set mode %s %s",
                    sm.name, sm.commandFluff ? "-command" : "-quiet");
            showPromptSettings(sm);
            showFormatSettings(sm, null);
            showTruncationSettings(sm);
        }

        void showFeedbackSetting() {
            if (retainedCurrentMode != null) {
                hard("/set feedback -retain %s", retainedCurrentMode.name);
            }
            if (mode != retainedCurrentMode) {
                hard("/set feedback %s", mode.name);
            }
        }

        // For /set prompt <mode> "<prompt>" "<continuation-prompt>"
        boolean setPrompt() {
            Mode m = nextMode();
            String prompt = nextFormat();
            String continuationPrompt = nextFormat();
            checkOptionsAndRemainingInput();
            if (valid && prompt == null) {
                showPromptSettings(m);
                return valid;
            }
            if (valid && m.readOnly) {
                errorat("jshell.err.not.valid.with.predefined.mode", m.name);
            } else if (continuationPrompt == null) {
                errorat("jshell.err.continuation.prompt.required");
            }
            if (valid) {
                m.setPrompts(prompt, continuationPrompt);
            } else {
                fluffmsg("jshell.msg.see", "/help /set prompt");
            }
            return valid;
        }

        /**
         * Set mode. Create, changed, or delete a feedback mode. For @{code /set
         * mode <mode> [<old-mode>] [-command|-quiet|-delete|-retain]}.
         *
         * @return true if successful
         */
        boolean setMode(Consumer<String> retainer) {
            class SetMode {

                final String umode;
                final String omode;
                final boolean commandOption;
                final boolean quietOption;
                final boolean deleteOption;
                final boolean retainOption;

                SetMode() {
                    at.allowedOptions("-command", "-quiet", "-delete", "-retain");
                    umode = nextModeIdentifier();
                    omode = nextModeIdentifier();
                    checkOptionsAndRemainingInput();
                    commandOption = at.hasOption("-command");
                    quietOption = at.hasOption("-quiet");
                    deleteOption = at.hasOption("-delete");
                    retainOption = at.hasOption("-retain");
                }

                void delete() {
                    // Note: delete, for safety reasons, does NOT do name matching
                    if (commandOption || quietOption) {
                        errorat("jshell.err.conflicting.options");
                    } else if (retainOption
                            ? !retainedMap.containsKey(umode) && !modeMap.containsKey(umode)
                            : !modeMap.containsKey(umode)) {
                        // Cannot delete a mode that does not exist
                        errorat("jshell.err.mode.unknown", umode);
                    } else if (omode != null) {
                        // old mode is for creation
                        errorat("jshell.err.unexpected.at.end", omode);
                    } else if (mode.name.equals(umode)) {
                        // Cannot delete the current mode out from under us
                        errorat("jshell.err.cannot.delete.current.mode", umode);
                    } else if (retainOption && retainedCurrentMode != null &&
                             retainedCurrentMode.name.equals(umode)) {
                        // Cannot delete the retained mode or re-start will have an error
                        errorat("jshell.err.cannot.delete.retained.mode", umode);
                    } else {
                        Mode m = modeMap.get(umode);
                        if (m != null && m.readOnly) {
                            errorat("jshell.err.not.valid.with.predefined.mode", umode);
                        } else {
                            // Remove the mode
                            modeMap.remove(umode);
                            if (retainOption) {
                                // Remove the retained mode
                                retainedMap.remove(umode);
                                updateRetainedModes();
                            }
                        }
                    }
                }

                void retain() {
                    if (commandOption || quietOption) {
                        errorat("jshell.err.conflicting.options");
                    } else if (omode != null) {
                        // old mode is for creation
                        errorat("jshell.err.unexpected.at.end", omode);
                    } else {
                        Mode m = modeMap.get(umode);
                        if (m == null) {
                            // can only retain existing modes
                            errorat("jshell.err.mode.unknown", umode);
                        } else if (m.readOnly) {
                            errorat("jshell.err.not.valid.with.predefined.mode", umode);
                        } else {
                            // Add to local cache of retained current encodings
                            retainedMap.put(m.name, m.encode());
                            updateRetainedModes();
                        }
                    }
                }

                void updateRetainedModes() {
                    // Join all the retained encodings
                    String encoded = String.join(RECORD_SEPARATOR, retainedMap.values());
                    // Retain it
                    retainer.accept(encoded);
                }

                void create() {
                    if (commandOption && quietOption) {
                        errorat("jshell.err.conflicting.options");
                    } else if (!commandOption && !quietOption) {
                        errorat("jshell.err.mode.creation");
                    } else if (modeMap.containsKey(umode)) {
                        // Mode already exists
                        errorat("jshell.err.mode.exists", umode);
                    } else {
                        Mode om = searchForMode(omode);
                        if (valid) {
                            // We are copying an existing mode and/or creating a
                            // brand-new mode -- in either case create from scratch
                            Mode m = (om != null)
                                    ? new Mode(umode, om)
                                    : new Mode(umode);
                            modeMap.put(umode, m);
                            fluffmsg("jshell.msg.feedback.new.mode", m.name);
                            m.setCommandFluff(commandOption);
                        }
                    }
                }

                boolean set() {
                    if (valid && !commandOption && !quietOption && !deleteOption &&
                            omode == null && !retainOption) {
                        // Not a creation, deletion, or retain -- show mode(s)
                        showModeSettings(umode, "jshell.err.mode.creation");
                    } else if (valid && umode == null) {
                        errorat("jshell.err.missing.mode");
                    } else if (valid && deleteOption) {
                        delete();
                    } else if (valid && retainOption) {
                        retain();
                    } else if (valid) {
                        create();
                    }
                    if (!valid) {
                        fluffmsg("jshell.msg.see", "/help /set mode");
                    }
                    return valid;
                }
            }
            return new SetMode().set();
        }

        // For /set format <mode> <field> "<format>" <selector>...
        boolean setFormat() {
            Mode m = nextMode();
            String field = toIdentifier(next(), "jshell.err.field.name");
            String format = nextFormat();
            if (valid && format == null) {
                if (field != null && m != null && !m.byField.containsKey(field)) {
                    errorat("jshell.err.field.name", field);
                } else {
                    showFormatSettings(m, field);
                }
            } else {
                installFormat(m, field, format, "/help /set format");
            }
            return valid;
        }

        // For /set truncation <mode> <length> <selector>...
        boolean setTruncation() {
            Mode m = nextMode();
            String length = next();
            if (length == null) {
                showTruncationSettings(m);
            } else {
                try {
                    // Assure that integer format is correct
                    Integer.parseUnsignedInt(length);
                } catch (NumberFormatException ex) {
                    errorat("jshell.err.truncation.length.not.integer", length);
                }
                // install length into an internal format field
                installFormat(m, TRUNCATION_FIELD, length, "/help /set truncation");
            }
            return valid;
        }

        // For /set feedback <mode>
        boolean setFeedback(Consumer<String> retainer) {
            String umode = next();
            checkOptionsAndRemainingInput();
            boolean retainOption = at.hasOption("-retain");
            if (valid && umode == null && !retainOption) {
                showFeedbackSetting();
                hard("");
                showFeedbackModes();
                return true;
            }
            if (valid) {
                Mode m = umode == null
                        ? mode
                        : searchForMode(toModeIdentifier(umode));
                if (valid && retainOption && !m.readOnly && !retainedMap.containsKey(m.name)) {
                    errorat("jshell.err.retained.feedback.mode.must.be.retained.or.predefined");
                }
                if (valid) {
                    if (umode != null) {
                        mode = m;
                        fluffmsg("jshell.msg.feedback.mode", mode.name);
                    }
                    if (retainOption) {
                        retainedCurrentMode = m;
                        retainer.accept(m.name);
                    }
                }
            }
            if (!valid) {
                fluffmsg("jshell.msg.see", "/help /set feedback");
                return false;
            }
            return true;
        }

        boolean restoreEncodedModes(String allEncoded) {
            try {
                // Iterate over each record in each encoded mode
                Iterator<String> itr = encodedModeIterator(allEncoded);
                while (itr.hasNext()) {
                    // Reconstruct the encoded mode
                    Mode m = buildMode(itr);
                    modeMap.put(m.name, m);
                    // Continue to retain if a new retains occur
                    retainedMap.put(m.name, m.encode());
                }
                return true;
            } catch (Throwable exc) {
                // Catastrophic corruption -- clear map
                errorat("jshell.err.retained.mode.failure", exc);
                retainedMap.clear();
                return false;
            }
        }


        /**
         * Set up a mode reconstituted from a preferences string.
         *
         * @param it the encoded Mode broken into String chunks, may contain
         *           subsequent encoded modes
         */
        private Mode buildMode(Iterator<String> it) {
            Mode newMode = new Mode(it.next(), Boolean.parseBoolean(it.next()),  it.next(), it.next());
            Map<String, List<Mode.Setting>> fields = new HashMap<>();
            long suspiciousBits = Selector.OLD_ALWAYS.asBits();
            boolean suspicious = false;
            String field;
            while (!(field = it.next()).equals("***")) {
                String open = it.next();
                assert open.equals("(");
                List<Mode.Setting> settings = new ArrayList<>();
                String selectorText;
                while (!(selectorText = it.next()).equals(")")) {
                    String format = it.next();
                    Selector selector;
                    if (selectorText.isEmpty()) {
                        selector = Selector.ALWAYS;
                    } else if (Character.isDigit(selectorText.charAt(0))) {
                        // legacy format, bits
                        long bits = Long.parseLong(selectorText);
                        suspicious |= bits == suspiciousBits;
                        selector = new Selector(bits);
                    } else {
                        selector = parseSelector(selectorText);
                    }
                    Mode.Setting ing = new Mode.Setting(format, selector);
                    settings.add(ing);
                }
                fields.put(field, settings);
            }
            List<Mode.Setting> tk;
            List<Mode.Setting> errf;
            // If suspicious that this is a pre-JDK-14 settings, check deeper...
            if (suspicious
                    // Super simple might not define typeKind, otherwise check for JDK-14 presence of record
                    && ((tk = fields.get("typeKind")) == null
                    || !tk.stream().anyMatch(tkc -> tkc.selector.equals(RECORD_TYPEKIND_SELECTOR)))
                    // no record typeKind, now check for corruption
                    && ((errf = fields.get("err")) == null
                    || errf.stream().anyMatch(tkc -> tkc.selector.equals(Selector.OLD_ALWAYS)))) {
                // Pre-JDK-14 setting found, convert them

                // start with solid base, ideally normal
                Mode base = modeMap.get("normal");
                if (base == null) {
                    base = mode;
                }

                // Make sure any current fields/selectors are covered: filling in with the base (normal)
                base.byField.forEach((fieldName, settingList) ->
                        settingList.forEach(setting -> newMode.set(fieldName, setting.format, setting.selector)));

                // Now, overlay with user's settings (position adjusted).
                // Assume any setting for class applies to record, except for typeKind definition where base definition
                // should fall through.
                fields.forEach((fieldName, settingList) -> {
                        settingList.forEach(setting -> newMode.set(fieldName, setting.format,
                                Selector.fromPreJDK14(setting.selector, !fieldName.equals("typeKind"))));
                        });
            } else {
                fields.forEach((fieldName, settingList) ->
                        settingList.forEach(setting -> newMode.set(fieldName, setting.format, setting.selector)));
            }
            return newMode;
        }

        Iterator<String> encodedModeIterator(String encoded) {
            String[] ms = encoded.split(RECORD_SEPARATOR);
            return Arrays.asList(ms).iterator();
        }

        // install the format of a field under parsed selectors
        void installFormat(Mode m, String field, String format, String help) {
            String slRaw;
            List<Selector> selectorList = new ArrayList<>();
            while (valid && (slRaw = next()) != null) {
                selectorList.add(parseSelector(slRaw));
            }
            checkOptionsAndRemainingInput();
            if (valid) {
                if (m.readOnly) {
                    errorat("jshell.err.not.valid.with.predefined.mode", m.name);
                } else if (selectorList.isEmpty()) {
                    // No selectors specified, then always use the format
                    m.set(field, format, Selector.ALWAYS);
                } else {
                    // Set the format of the field for specified selector
                    selectorList.forEach(sel -> m.set(field, format, sel));
                }
            } else {
                fluffmsg("jshell.msg.see", help);
            }
        }

        void checkOptionsAndRemainingInput() {
            String junk = at.remainder();
            if (!junk.isEmpty()) {
                errorat("jshell.err.unexpected.at.end", junk);
            } else {
                String bad = at.badOptions();
                if (!bad.isEmpty()) {
                    errorat("jshell.err.unknown.option", bad);
                }
            }
        }

        String next() {
            String s = at.next();
            if (s == null) {
                checkOptionsAndRemainingInput();
            }
            return s;
        }

        /**
         * Check that the specified string is an identifier (Java identifier).
         * If null display the missing error. If it is not an identifier,
         * display the error.
         *
         * @param id the string to check, MUST be the most recently retrieved
         * token from 'at'.
         * @param err the resource error to display if not an identifier
         * @return the identifier string, or null if null or not an identifier
         */
        private String toIdentifier(String id, String err) {
            if (!valid || id == null) {
                return null;
            }
            if (at.isQuoted() ||
                    !id.codePoints().allMatch(Character::isJavaIdentifierPart)) {
                errorat(err, id);
                return null;
            }
            return id;
        }

        private String toModeIdentifier(String id) {
            return toIdentifier(id, "jshell.err.mode.name");
        }

        private String nextModeIdentifier() {
            return toModeIdentifier(next());
        }

        private Mode nextMode() {
            String umode = nextModeIdentifier();
            return searchForMode(umode);
        }

        private Mode searchForMode(String umode) {
            return searchForMode(umode, null);
        }

        private Mode searchForMode(String umode, String msg) {
            if (!valid || umode == null) {
                return null;
            }
            Mode m = modeMap.get(umode);
            if (m != null) {
                return m;
            }
            // Failing an exact match, go searching
            Mode[] matches = modeMap.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(umode))
                    .map(Entry::getValue)
                    .toArray(Mode[]::new);
            if (matches.length == 1) {
                return matches[0];
            } else {
                if (msg != null) {
                    hardmsg(msg, "");
                }
                if (matches.length == 0) {
                    errorat("jshell.err.feedback.does.not.match.mode", umode);
                } else {
                    errorat("jshell.err.feedback.ambiguous.mode", umode);
                }
                if (showFluff()) {
                    showFeedbackModes();
                }
                return null;
            }
        }

        void showFeedbackModes() {
            if (!retainedMap.isEmpty()) {
                hardmsg("jshell.msg.feedback.retained.mode.following");
                retainedMap.keySet().stream()
                        .sorted()
                        .forEach(mk -> hard("   %s", mk));
            }
            hardmsg("jshell.msg.feedback.mode.following");
            modeMap.keySet().stream()
                    .sorted()
                    .forEach(mk -> hard("   %s", mk));
        }

        // Read and test if the format string is correctly
        private String nextFormat() {
            return toFormat(next());
        }

        // Test if the format string is correctly
        private String toFormat(String format) {
            if (!valid || format == null) {
                return null;
            }
            if (!at.isQuoted()) {
                errorat("jshell.err.feedback.must.be.quoted", format);
               return null;
            }
            return format;
        }

        // Convert to a quoted string
        private String toStringLiteral(String s) {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            final int length = s.length();
            for (int offset = 0; offset < length;) {
                final int codepoint = s.codePointAt(offset);

                switch (codepoint) {
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\"':
                        sb.append("\\\"");
                        break;
                    case '\'':
                        sb.append("\\'");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    default:
                        if (codepoint < 040) {
                            sb.append(String.format("\\%o", codepoint));
                        } else {
                            sb.appendCodePoint(codepoint);
                        }
                        break;
                }

                // do something with the codepoint
                offset += Character.charCount(codepoint);

            }
            sb.append('"');
            return sb.toString();
        }

        private Selector parseSelector(String selectorText) {
            SelectorBuilder seb = new SelectorBuilder(selectorText);
            EnumSet<SelectorKind> seen = EnumSet.noneOf(SelectorKind.class);
            for (String s : selectorText.split("-")) {
                SelectorKind lastKind = null;
                for (String as : s.split(",")) {
                    if (!as.isEmpty()) {
                        SelectorInstanceWithDoc<?> sel = Selector.selectorMap.get(as);
                        if (sel == null) {
                            errorat("jshell.err.feedback.not.a.valid.selector", as, s);
                            return Selector.ALWAYS;
                        }
                        SelectorKind kind = sel.kind();
                        if (lastKind == null) {
                            if (seen.contains(kind)) {
                                errorat("jshell.err.feedback.multiple.sections", as, s);
                                return Selector.ALWAYS;
                            }
                        } else if (kind != lastKind) {
                            errorat("jshell.err.feedback.different.selector.kinds", as, s);
                            return Selector.ALWAYS;
                        }
                        seb.add(sel);
                        seen.add(kind);
                        lastKind = kind;
                    }
                }
            }
            return seb.toSelector();
         }
    }
}
