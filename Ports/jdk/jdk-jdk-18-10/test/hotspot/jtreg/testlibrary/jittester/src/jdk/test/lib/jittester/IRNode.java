/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test.lib.jittester;

// Production is base class for all elements of the IR-tree.
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import jdk.test.lib.jittester.loops.For;
import jdk.test.lib.jittester.loops.DoWhile;
import jdk.test.lib.jittester.loops.While;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.visitors.Visitor;

public abstract class IRNode {
    private IRNode parent;
    private final List<IRNode> children = new ArrayList<>();
    protected TypeKlass owner;
    protected int level;
    private final Type resultType;

    protected IRNode(Type resultType) {
        this.resultType = resultType;
    }

    public Type getResultType() {
        return resultType;
    }

    //TODO
    //private boolean isCFDeviation;

    public abstract <T> T accept(Visitor<T> v);

    public void setOwner(TypeKlass owner) {
        this.owner = owner;
        if (Objects.isNull(owner)) {
            System.out.println(getClass().getName() + " null");
            for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
                System.out.println(s.toString());
            }
        }
    }

    public void addChild(IRNode child) {
        if (Objects.nonNull(child)) {
            children.add(child);
            child.parent = this;
        }
    }

    public void addChildren(List<? extends IRNode> ch) {
        if (Objects.nonNull(ch)) {
            ch.stream()
                .filter(c -> c != null)
                .forEach(this::addChild);
        }
    }

    public List<IRNode> getChildren() {
        return children;
    }

    public IRNode getChild(int i) {
        return i < children.size() ? children.get(i) : null;
    }

    public TypeKlass getOwner() {
        return owner;
    }

    public IRNode getParent() {
        return parent;
    }

    public void setChild(int index, IRNode child) {
        children.set(index, child);
        if (Objects.nonNull(child)) {
            child.parent = this;
        }
    }

    public boolean removeChild(IRNode l) {
        return children.remove(l);
    }

    public boolean removeSelf() {
        return parent.children.remove(this);
    }

    public void resizeUpChildren(int size) {
        for (int i = children.size(); i < size; ++i) {
            children.add(null);
        }
    }

    public void removeAllChildren() {
        children.clear();
    }

    public String getTreeTextView(int indent) {
        StringBuilder sb = new StringBuilder();
        if (level > 0) {
        for (int i = 0; i < indent; ++i) {
            sb.append("\t");
        }
        sb.append(getName())
                .append(" [")
                .append(level)
                .append("]")
                    .append(System.lineSeparator());
        }
        children.stream()
                .filter(ch -> !Objects.isNull(ch))
                .forEach(ch -> sb.append(ch.getTreeTextView(indent + 1)));
        return sb.toString();
    }

    protected IRNode evolve() {
        throw new Error("Not implemented");
    }

    public int getLevel() {
        return level;
    }

    public long complexity() {
        return 0L;
    }

    @Override
    public final String toString() {
        return getName();
    }

    public String getName() {
        return this.getClass().getName();
    }

    public static long countDepth(Collection<IRNode> input) {
        return input.stream()
                .filter(Objects::nonNull)
                .mapToLong(IRNode::countDepth)
                .max().orElse(0L);
    }

    public long countDepth() {
        return IRNode.countDepth(children);
    }

    public List<IRNode> getStackableLeaves() {
        List<IRNode> result = new ArrayList<>();
        children.stream()
                .filter(Objects::nonNull)
                .forEach(c -> {
                    if (countDepth() == c.level && (c instanceof Block)) {
                        result.add(c);
                    } else {
                        result.addAll(c.getStackableLeaves());
                    }
                });
        return result;
    }

    public List<IRNode> getDeviantBlocks(long depth) {
        List<IRNode> result = new ArrayList<>();
        children.stream()
                .filter(c -> !Objects.isNull(c))
                .forEach(c -> {
            if (depth == c.level && c.isCFDeviation()) {
                        result.add(c);
                    } else {
                        result.addAll(c.getDeviantBlocks(depth));
                    }
                });
        return result;
    }

    public static long getModifiableNodesCount(List<IRNode> nodes) {
        return nodes.stream()
                .map(IRNode::getStackableLeaves)
                .mapToInt(List::size)
                .filter(i -> i > 0)
                .count();
    }

    public static boolean tryToReduceNodesDepth(List<IRNode> nodes, int maxDepth) {
        boolean allSucceed = true;
        for (IRNode child : nodes) {
            for (IRNode leaf : child.getDeviantBlocks(Math.max(child.countDepth(), maxDepth + 1))) {
                if (child.countDepth() > maxDepth) {
                    // doesn't remove control deviation block. Just some parts.
                    leaf.removeSelf();
                    boolean successfull = child.countDepth() > maxDepth;
                    allSucceed &= successfull;
                } else {
                    break;
                }
            }
        }
        return allSucceed;
    }

    // TODO: add field instead this function
    public boolean isCFDeviation() {
        return this instanceof If || this instanceof Switch
            || this instanceof For || this instanceof While
            || this instanceof DoWhile
            || (this instanceof Block && this.parent instanceof Block);
    }
}
