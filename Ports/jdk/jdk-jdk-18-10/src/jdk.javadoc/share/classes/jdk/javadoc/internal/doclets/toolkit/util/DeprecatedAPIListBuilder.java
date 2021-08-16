/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.util.*;

import javax.lang.model.element.Element;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;

/**
 * Build list of all the deprecated packages, classes, constructors, fields and methods.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DeprecatedAPIListBuilder extends SummaryAPIListBuilder {

    private SortedSet<Element> forRemoval;
    public final List<String> releases;

    /**
     * Constructor.
     *
     * @param configuration the current configuration of the doclet
     * @param releases list of releases
     */
    public DeprecatedAPIListBuilder(BaseConfiguration configuration, List<String> releases) {
        super(configuration, configuration.utils::isDeprecated);
        this.releases = releases;
        buildSummaryAPIInfo();
    }

    public SortedSet<Element> getForRemoval() {
        if (forRemoval == null) {
            forRemoval = createSummarySet();
        }
        return forRemoval;
    }

    @Override
    protected void handleElement(Element e) {
        if (utils.isDeprecatedForRemoval(e)) {
            getForRemoval().add(e);
        }
    }
}
