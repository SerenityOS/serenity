/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.invoke.LambdaForm.*;
import static java.lang.invoke.LambdaForm.BasicType.*;

/** Working storage for an LF that is being transformed.
 *  Similarly to a StringBuffer, the editing can take place in multiple steps.
 */
final class LambdaFormBuffer {
    private int arity, length;
    private Name[] names;
    private Name[] originalNames;  // snapshot of pre-transaction names
    private byte flags;
    private int firstChange;
    private Name resultName;
    private ArrayList<Name> dups;

    private static final int F_TRANS = 0x10, F_OWNED = 0x03;

    LambdaFormBuffer(LambdaForm lf) {
        this.arity = lf.arity;
        setNames(lf.names);
        int result = lf.result;
        if (result == LAST_RESULT)  result = length - 1;
        if (result >= 0 && lf.names[result].type != V_TYPE) {
            resultName = lf.names[result];
        }
        assert(lf.nameRefsAreLegal());
    }

    private LambdaForm lambdaForm() {
        assert(!inTrans());  // need endEdit call to tidy things up
        return new LambdaForm(arity, nameArray(), resultIndex());
    }

    Name name(int i) {
        assert(i < length);
        return names[i];
    }

    Name[] nameArray() {
        return Arrays.copyOf(names, length);
    }

    int resultIndex() {
        if (resultName == null)  return VOID_RESULT;
        int index = indexOf(resultName, names);
        assert(index >= 0);
        return index;
    }

    void setNames(Name[] names2) {
        names = originalNames = names2;  // keep a record of where everything was to start with
        length = names2.length;
        flags = 0;
    }

    private boolean verifyArity() {
        for (int i = 0; i < arity && i < firstChange; i++) {
            assert(names[i].isParam()) : "#" + i + "=" + names[i];
        }
        for (int i = arity; i < length; i++) {
            assert(!names[i].isParam()) : "#" + i + "=" + names[i];
        }
        for (int i = length; i < names.length; i++) {
            assert(names[i] == null) : "#" + i + "=" + names[i];
        }
        // check resultName also
        if (resultName != null) {
            int resultIndex = indexOf(resultName, names);
            assert(resultIndex >= 0) : "not found: " + resultName.exprString() + Arrays.asList(names);
            assert(names[resultIndex] == resultName);
        }
        return true;
    }

    private boolean verifyFirstChange() {
        assert(inTrans());
        for (int i = 0; i < length; i++) {
            if (names[i] != originalNames[i]) {
                assert(firstChange == i) : Arrays.asList(firstChange, i, originalNames[i].exprString(), Arrays.asList(names));
                return true;
            }
        }
        assert(firstChange == length) : Arrays.asList(firstChange, Arrays.asList(names));
        return true;
    }

    private static int indexOf(NamedFunction fn, List<NamedFunction> fns) {
        for (int i = 0; i < fns.size(); i++) {
            if (fns.get(i) == fn)  return i;
        }
        return -1;
    }

    private static int indexOf(Name n, Name[] ns) {
        for (int i = 0; i < ns.length; i++) {
            if (ns[i] == n)  return i;
        }
        return -1;
    }

    boolean inTrans() {
        return (flags & F_TRANS) != 0;
    }

    int ownedCount() {
        return flags & F_OWNED;
    }

    void growNames(int insertPos, int growLength) {
        int oldLength = length;
        int newLength = oldLength + growLength;
        int oc = ownedCount();
        if (oc == 0 || newLength > names.length) {
            names = Arrays.copyOf(names, (names.length + growLength) * 5 / 4);
            if (oc == 0) {
                flags++;
                oc++;
                assert(ownedCount() == oc);
            }
        }
        if (originalNames != null && originalNames.length < names.length) {
            originalNames = Arrays.copyOf(originalNames, names.length);
            if (oc == 1) {
                flags++;
                oc++;
                assert(ownedCount() == oc);
            }
        }
        if (growLength == 0)  return;
        int insertEnd = insertPos + growLength;
        int tailLength = oldLength - insertPos;
        System.arraycopy(names, insertPos, names, insertEnd, tailLength);
        Arrays.fill(names, insertPos, insertEnd, null);
        if (originalNames != null) {
            System.arraycopy(originalNames, insertPos, originalNames, insertEnd, tailLength);
            Arrays.fill(originalNames, insertPos, insertEnd, null);
        }
        length = newLength;
        if (firstChange >= insertPos) {
            firstChange += growLength;
        }
    }

    int lastIndexOf(Name n) {
        int result = -1;
        for (int i = 0; i < length; i++) {
            if (names[i] == n)  result = i;
        }
        return result;
    }

    /** We have just overwritten the name at pos1 with the name at pos2.
     *  This means that there are two copies of the name, which we will have to fix later.
     */
    private void noteDuplicate(int pos1, int pos2) {
        Name n = names[pos1];
        assert(n == names[pos2]);
        assert(originalNames[pos1] != null);  // something was replaced at pos1
        assert(originalNames[pos2] == null || originalNames[pos2] == n);
        if (dups == null) {
            dups = new ArrayList<>();
        }
        dups.add(n);
    }

    /** Replace duplicate names by nulls, and remove all nulls. */
    private void clearDuplicatesAndNulls() {
        if (dups != null) {
            // Remove duplicates.
            assert(ownedCount() >= 1);
            for (Name dup : dups) {
                for (int i = firstChange; i < length; i++) {
                    if (names[i] == dup && originalNames[i] != dup) {
                        names[i] = null;
                        assert(Arrays.asList(names).contains(dup));
                        break;  // kill only one dup
                    }
                }
            }
            dups.clear();
        }
        // Now that we are done with originalNames, remove "killed" names.
        int oldLength = length;
        for (int i = firstChange; i < length; i++) {
            if (names[i] == null) {
                System.arraycopy(names, i + 1, names, i, (--length - i));
                --i;  // restart loop at this position
            }
        }
        if (length < oldLength) {
            Arrays.fill(names, length, oldLength, null);
        }
        assert(!Arrays.asList(names).subList(0, length).contains(null));
    }

    /** Create a private, writable copy of names.
     *  Preserve the original copy, for reference.
     */
    void startEdit() {
        assert(verifyArity());
        int oc = ownedCount();
        assert(!inTrans());  // no nested transactions
        flags |= F_TRANS;
        Name[] oldNames = names;
        Name[] ownBuffer = (oc == 2 ? originalNames : null);
        assert(ownBuffer != oldNames);
        if (ownBuffer != null && ownBuffer.length >= length) {
            names = copyNamesInto(ownBuffer);
        } else {
            // make a new buffer to hold the names
            final int SLOP = 2;
            names = Arrays.copyOf(oldNames, Math.max(length + SLOP, oldNames.length));
            if (oc < 2)  ++flags;
            assert(ownedCount() == oc + 1);
        }
        originalNames = oldNames;
        assert(originalNames != names);
        firstChange = length;
        assert(inTrans());
    }

    void changeName(int i, Name name) {
        assert(inTrans());
        assert(i < length);
        Name oldName = names[i];
        assert(oldName == originalNames[i]);  // no multiple changes
        assert(verifyFirstChange());
        if (ownedCount() == 0)
            growNames(0, 0);
        names[i] = name;
        if (firstChange > i) {
            firstChange = i;
        }
        if (resultName != null && resultName == oldName) {
            resultName = name;
        }
    }

    /** Change the result name.  Null means a void result. */
    void setResult(Name name) {
        assert(name == null || lastIndexOf(name) >= 0);
        resultName = name;
    }

    /** Finish a transaction. */
    LambdaForm endEdit() {
        assert(verifyFirstChange());
        // Assuming names have been changed pairwise from originalNames[i] to names[i],
        // update arguments to ensure referential integrity.
        for (int i = Math.max(firstChange, arity); i < length; i++) {
            Name name = names[i];
            if (name == null)  continue;  // space for removed duplicate
            Name newName = name.replaceNames(originalNames, names, firstChange, i);
            if (newName != name) {
                names[i] = newName;
                if (resultName == name) {
                    resultName = newName;
                }
            }
        }
        assert(inTrans());
        flags &= ~F_TRANS;
        clearDuplicatesAndNulls();
        originalNames = null;
        // If any parameters have been changed, then reorder them as needed.
        // This is a "sheep-and-goats" stable sort, pushing all non-parameters
        // to the right of all parameters.
        if (firstChange < arity) {
            Name[] exprs = new Name[arity - firstChange];
            int argp = firstChange, exprp = 0;
            for (int i = firstChange; i < arity; i++) {
                Name name = names[i];
                if (name != null && name.isParam()) {
                    names[argp++] = name;
                } else {
                    exprs[exprp++] = name;
                }
            }
            assert(exprp == (arity - argp));
            // copy the exprs just after the last remaining param
            System.arraycopy(exprs, 0, names, argp, exprp);
            // adjust arity
            arity -= exprp;
        }
        assert(verifyArity());
        return lambdaForm();
    }

    private Name[] copyNamesInto(Name[] buffer) {
        System.arraycopy(names, 0, buffer, 0, length);
        Arrays.fill(buffer, length, buffer.length, null);
        return buffer;
    }

    /** Replace any Name whose function is in oldFns with a copy
     *  whose function is in the corresponding position in newFns.
     *  Only do this if the arguments are exactly equal to the given.
     */
    LambdaFormBuffer replaceFunctions(List<NamedFunction> oldFns, List<NamedFunction> newFns,
                                      Object... forArguments) {
        assert(inTrans());
        if (oldFns.isEmpty())  return this;
        for (int i = arity; i < length; i++) {
            Name n = names[i];
            int nfi = indexOf(n.function, oldFns);
            if (nfi >= 0 && Arrays.equals(n.arguments, forArguments)) {
                changeName(i, new Name(newFns.get(nfi), n.arguments));
            }
        }
        return this;
    }

    private void replaceName(int pos, Name binding) {
        assert(inTrans());
        assert(verifyArity());
        assert(pos < arity);
        Name param = names[pos];
        assert(param.isParam());
        assert(param.type == binding.type);
        changeName(pos, binding);
    }

    /** Replace a parameter by a fresh parameter. */
    LambdaFormBuffer renameParameter(int pos, Name newParam) {
        assert(newParam.isParam());
        replaceName(pos, newParam);
        return this;
    }

    /** Replace a parameter by a fresh expression. */
    LambdaFormBuffer replaceParameterByNewExpression(int pos, Name binding) {
        assert(!binding.isParam());
        assert(lastIndexOf(binding) < 0);  // else use replaceParameterByCopy
        replaceName(pos, binding);
        return this;
    }

    /** Replace a parameter by another parameter or expression already in the form. */
    LambdaFormBuffer replaceParameterByCopy(int pos, int valuePos) {
        assert(pos != valuePos);
        replaceName(pos, names[valuePos]);
        noteDuplicate(pos, valuePos);  // temporarily, will occur twice in the names array
        return this;
    }

    private void insertName(int pos, Name expr, boolean isParameter) {
        assert(inTrans());
        assert(verifyArity());
        assert(isParameter ? pos <= arity : pos >= arity);
        growNames(pos, 1);
        if (isParameter)  arity += 1;
        changeName(pos, expr);
    }

    /** Insert a fresh expression. */
    LambdaFormBuffer insertExpression(int pos, Name expr) {
        assert(!expr.isParam());
        insertName(pos, expr, false);
        return this;
    }

    /** Insert a fresh parameter. */
    LambdaFormBuffer insertParameter(int pos, Name param) {
        assert(param.isParam());
        insertName(pos, param, true);
        return this;
    }
}
