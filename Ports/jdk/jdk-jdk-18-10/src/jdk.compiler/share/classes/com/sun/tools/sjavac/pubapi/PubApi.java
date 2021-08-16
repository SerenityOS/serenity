/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.pubapi;


import static com.sun.tools.sjavac.Util.union;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.StringUtils;

public class PubApi implements Serializable {

    private static final long serialVersionUID = 5926627347801986850L;

    // Used to have Set here. Problem is that the objects are mutated during
    // javac_state loading, causing them to change hash codes. We could probably
    // change back to Set once javac_state loading is cleaned up.
    public final Map<String, PubType> types = new HashMap<>();
    public final Map<String, PubVar> variables = new HashMap<>();
    public final Map<String, PubMethod> methods = new HashMap<>();
    public final Map<String, PubVar> recordComponents = new HashMap<>();

    public PubApi() {
    }

    public PubApi(Collection<PubType> types,
                  Collection<PubVar> variables,
                  Collection<PubMethod> methods) {
        types.forEach(this::addPubType);
        variables.forEach(this::addPubVar);
        methods.forEach(this::addPubMethod);
    }

    // Currently this is implemented as equality. This is far from optimal. It
    // should preferably make sure that all previous methods are still available
    // and no abstract methods are added. It should also be aware of inheritance
    // of course.
    public boolean isBackwardCompatibleWith(PubApi older) {
        return equals(older);
    }

    private static String typeLine(PubType type) {
        if (type.fqName.isEmpty())
            throw new RuntimeException("empty class name " + type);
        return String.format("TYPE %s%s", asString(type.modifiers), type.fqName);
    }

    private static String varLine(PubVar var) {
        return String.format("VAR %s%s %s%s",
                             asString(var.modifiers),
                             TypeDesc.encodeAsString(var.type),
                             var.identifier,
                             var.getConstValue().map(v -> " = " + v).orElse(""));
    }

    private static String methodLine(PubMethod method) {
        return String.format("METHOD %s%s%s %s(%s)%s",
                             asString(method.modifiers),
                             method.typeParams.isEmpty() ? "" : ("<" + method.typeParams.stream().map(PubApiTypeParam::asString).collect(Collectors.joining(",")) + "> "),
                             TypeDesc.encodeAsString(method.returnType),
                             method.identifier,
                             commaSeparated(method.paramTypes),
                             method.throwDecls.isEmpty()
                                 ? ""
                                 : " throws " + commaSeparated(method.throwDecls));
    }

    public List<String> asListOfStrings() {
        List<String> lines = new ArrayList<>();

        // Types
        types.values()
             .stream()
             .sorted(Comparator.comparing(PubApi::typeLine))
             .forEach(type -> {
                 lines.add(typeLine(type));
                 for (String subline : type.pubApi.asListOfStrings())
                     lines.add("  " + subline);
             });

        // Variables
        variables.values()
                 .stream()
                 .map(PubApi::varLine)
                 .sorted()
                 .forEach(lines::add);

        // Methods
        methods.values()
               .stream()
               .map(PubApi::methodLine)
               .sorted()
               .forEach(lines::add);

        return lines;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass())
            return false;
        PubApi other = (PubApi) obj;
        return types.equals(other.types)
            && variables.equals(other.variables)
            && methods.equals(other.methods);
    }

    @Override
    public int hashCode() {
        return types.keySet().hashCode()
             ^ variables.keySet().hashCode()
             ^ methods.keySet().hashCode();
    }

    private static String commaSeparated(List<TypeDesc> typeDescs) {
        return typeDescs.stream()
                        .map(TypeDesc::encodeAsString)
                        .collect(Collectors.joining(","));
    }

    // Create space separated list of modifiers (with a trailing space)
    private static String asString(Set<Modifier> modifiers) {
        return modifiers.stream()
                        .map(mod -> mod + " ")
                        .sorted()
                        .collect(Collectors.joining());
    }

    // Used to combine class PubApis to package level PubApis
    public static PubApi mergeTypes(PubApi api1, PubApi api2) {
        Assert.check(api1.methods.isEmpty(), "Can only merge types.");
        Assert.check(api2.methods.isEmpty(), "Can only merge types.");
        Assert.check(api1.variables.isEmpty(), "Can only merge types.");
        Assert.check(api2.variables.isEmpty(), "Can only merge types.");
        PubApi merged = new PubApi();
        merged.types.putAll(api1.types);
        merged.types.putAll(api2.types);
        return merged;
    }


    // Used for line-by-line parsing
    private PubType lastInsertedType = null;

    private static final String MODIFIERS = Stream.of(Modifier.values())
                                                  .map(Modifier::name)
                                                  .map(StringUtils::toLowerCase)
                                                  .collect(Collectors.joining("|", "(", ")"));

    private static final Pattern MOD_PATTERN = Pattern.compile("(" + MODIFIERS + " )*");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?<ret>.+?) (?<name>\\S+)\\((?<params>.*)\\)( throws (?<throws>.*))?");
    private static final Pattern VAR_PATTERN = Pattern.compile("VAR (?<modifiers>("+MODIFIERS+" )*)(?<type>.+?) (?<id>\\S+)( = (?<val>.*))?");
    private static final Pattern TYPE_PATTERN = Pattern.compile("TYPE (?<modifiers>("+MODIFIERS+" )*)(?<fullyQualified>\\S+)");

    public void appendItem(String l) {
        try {
            if (l.startsWith("  ")) {
                lastInsertedType.pubApi.appendItem(l.substring(2));
                return;
            }

            if (l.startsWith("METHOD")) {
                l = l.substring("METHOD ".length());
                Set<Modifier> modifiers = new HashSet<>();
                Matcher modMatcher = MOD_PATTERN.matcher(l);
                if (modMatcher.find()) {
                    String modifiersStr = modMatcher.group();
                    modifiers.addAll(parseModifiers(modifiersStr));
                    l = l.substring(modifiersStr.length());
                }
                List<PubApiTypeParam> typeParams = new ArrayList<>();
                if (l.startsWith("<")) {
                    int closingPos = findClosingTag(l, 0);
                    String str = l.substring(1, closingPos);
                    l = l.substring(closingPos+1);
                    typeParams.addAll(parseTypeParams(splitOnTopLevelCommas(str)));
                }
                Matcher mm = METHOD_PATTERN.matcher(l);
                if (!mm.matches())
                    throw new AssertionError("Could not parse return type, identifier, parameter types or throws declaration of method: " + l);

                List<String> params = splitOnTopLevelCommas(mm.group("params"));
                String th = Optional.ofNullable(mm.group("throws")).orElse("");
                List<String> throwz = splitOnTopLevelCommas(th);
                PubMethod m = new PubMethod(modifiers,
                                            typeParams,
                                            TypeDesc.decodeString(mm.group("ret")),
                                            mm.group("name"),
                                            parseTypeDescs(params),
                                            parseTypeDescs(throwz));
                addPubMethod(m);
                return;
            }

            Matcher vm = VAR_PATTERN.matcher(l);
            if (vm.matches()) {
                addPubVar(new PubVar(parseModifiers(vm.group("modifiers")),
                                     TypeDesc.decodeString(vm.group("type")),
                                     vm.group("id"),
                                     vm.group("val")));
                return;
            }

            Matcher tm = TYPE_PATTERN.matcher(l);
            if (tm.matches()) {
                addPubType(new PubType(parseModifiers(tm.group("modifiers")),
                                       tm.group("fullyQualified"),
                                       new PubApi()));
                return;
            }

            throw new AssertionError("No matching line pattern.");
        } catch (Throwable e) {
            throw new AssertionError("Could not parse API line: " + l, e);
        }
    }

    public void addPubType(PubType t) {
        types.put(t.fqName, t);
        lastInsertedType = t;
    }

    public void addPubVar(PubVar v) {
        variables.put(v.identifier, v);
    }

    public void addPubMethod(PubMethod m) {
        methods.put(m.asSignatureString(), m);
    }

    private static List<TypeDesc> parseTypeDescs(List<String> strs) {
        return strs.stream()
                   .map(TypeDesc::decodeString)
                   .toList();
    }

    private static List<PubApiTypeParam> parseTypeParams(List<String> strs) {
        return strs.stream().map(PubApi::parseTypeParam).toList();
    }

    // Parse a type parameter string. Example input:
    //     identifier
    //     identifier extends Type (& Type)*
    private static PubApiTypeParam parseTypeParam(String typeParamString) {
        int extPos = typeParamString.indexOf(" extends ");
        if (extPos == -1)
            return new PubApiTypeParam(typeParamString, Collections.emptyList());
        String identifier = typeParamString.substring(0, extPos);
        String rest = typeParamString.substring(extPos + " extends ".length());
        List<TypeDesc> bounds = parseTypeDescs(splitOnTopLevelChars(rest, '&'));
        return new PubApiTypeParam(identifier, bounds);
    }

    public Set<Modifier> parseModifiers(String modifiers) {
        if (modifiers == null)
            return Collections.emptySet();
        return Stream.of(modifiers.split(" "))
                     .map(String::trim)
                     .map(StringUtils::toUpperCase)
                     .filter(s -> !s.isEmpty())
                     .map(Modifier::valueOf)
                     .collect(Collectors.toSet());
    }

    // Find closing tag of the opening tag at the given 'pos'.
    private static int findClosingTag(String l, int pos) {
        while (true) {
            pos = pos + 1;
            if (l.charAt(pos) == '>')
                return pos;
            if (l.charAt(pos) == '<')
                pos = findClosingTag(l, pos);
        }
    }

    public List<String> splitOnTopLevelCommas(String s) {
        return splitOnTopLevelChars(s, ',');
    }

    public static List<String> splitOnTopLevelChars(String s, char split) {
        if (s.isEmpty())
            return Collections.emptyList();
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int depth = 0;
        for (char c : s.toCharArray()) {
            if (c == split && depth == 0) {
                result.add(buf.toString().trim());
                buf = new StringBuilder();
            } else {
                if (c == '<') depth++;
                if (c == '>') depth--;
                buf.append(c);
            }
        }
        result.add(buf.toString().trim());
        return result;
    }

    public boolean isEmpty() {
        return types.isEmpty() && variables.isEmpty() && methods.isEmpty();
    }

    // Used for descriptive debug messages when figuring out what triggers
    // recompilation.
    public List<String> diff(PubApi prevApi) {
        return diff("", prevApi);
    }
    private List<String> diff(String scopePrefix, PubApi prevApi) {

        List<String> diffs = new ArrayList<>();

        for (String typeKey : union(types.keySet(), prevApi.types.keySet())) {
            PubType type = types.get(typeKey);
            PubType prevType = prevApi.types.get(typeKey);
            if (prevType == null) {
                diffs.add("Type " + scopePrefix + typeKey + " was added");
            } else if (type == null) {
                diffs.add("Type " + scopePrefix + typeKey + " was removed");
            } else {
                // Check modifiers
                if (!type.modifiers.equals(prevType.modifiers)) {
                    diffs.add("Modifiers for type " + scopePrefix + typeKey
                            + " changed from " + prevType.modifiers + " to "
                            + type.modifiers);
                }

                // Recursively check types pub API
                diffs.addAll(type.pubApi.diff(prevType.pubApi));
            }
        }

        for (String varKey : union(variables.keySet(), prevApi.variables.keySet())) {
            PubVar var = variables.get(varKey);
            PubVar prevVar = prevApi.variables.get(varKey);
            if (prevVar == null) {
                diffs.add("Variable " + scopePrefix + varKey + " was added");
            } else if (var == null) {
                diffs.add("Variable " + scopePrefix + varKey + " was removed");
            } else {
                if (!var.modifiers.equals(prevVar.modifiers)) {
                    diffs.add("Modifiers for var " + scopePrefix + varKey
                            + " changed from " + prevVar.modifiers + " to "
                            + var.modifiers);
                }
                if (!var.type.equals(prevVar.type)) {
                    diffs.add("Type of " + scopePrefix + varKey
                            + " changed from " + prevVar.type + " to "
                            + var.type);
                }
                if (!var.getConstValue().equals(prevVar.getConstValue())) {
                    diffs.add("Const value of " + scopePrefix + varKey
                            + " changed from " + prevVar.getConstValue().orElse("<none>")
                            + " to " + var.getConstValue().orElse("<none>"));
                }
            }
        }

        for (String methodKey : union(methods.keySet(), prevApi.methods.keySet())) {
            PubMethod method = methods.get(methodKey);
            PubMethod prevMethod = prevApi.methods.get(methodKey);
            if (prevMethod == null) {
                diffs.add("Method " + scopePrefix + methodKey + " was added");
            } else if (method == null) {
                diffs.add("Method " + scopePrefix + methodKey + " was removed");
            } else {
                if (!method.modifiers.equals(prevMethod.modifiers)) {
                    diffs.add("Modifiers for method " + scopePrefix + methodKey
                            + " changed from " + prevMethod.modifiers + " to "
                            + method.modifiers);
                }
                if (!method.typeParams.equals(prevMethod.typeParams)) {
                    diffs.add("Type parameters for method " + scopePrefix
                            + methodKey + " changed from " + prevMethod.typeParams
                            + " to " + method.typeParams);
                }
                if (!method.throwDecls.equals(prevMethod.throwDecls)) {
                    diffs.add("Throw decl for method " + scopePrefix + methodKey
                            + " changed from " + prevMethod.throwDecls + " to "
                            + " to " + method.throwDecls);
                }
            }
        }

        return diffs;
    }

    public String toString() {
        return String.format("%s[types: %s, variables: %s, methods: %s]",
                             getClass().getSimpleName(),
                             types.values(),
                             variables.values(),
                             methods.values());
    }
}
