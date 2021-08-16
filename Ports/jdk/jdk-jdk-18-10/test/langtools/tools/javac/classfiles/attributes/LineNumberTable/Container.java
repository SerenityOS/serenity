/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Source code template container. Contains methods for inserting one template inside another one and for generating
 * final sources replacing placeholder by language construction.
 */
public class Container {

    private static final String TEMPLATE_LEVEL = "#LEVEL";
    private static final String SUB_TEMPLATE = "#SUB_TEMPLATE";

    private String template;
    private int level;

    Container(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }

    public Container insert(Container container) {
        template = template.replace(SUB_TEMPLATE, container.getTemplate());
        template = template.replaceAll(TEMPLATE_LEVEL, String.valueOf(level++));
        return this;
    }

    public List<TestCase> generate(Construction... constructions) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        String template = getTemplate();

        int lineNumberOffset = template.substring(0, template.indexOf(SUB_TEMPLATE)).split("\n").length - 1;
        for (Construction c : constructions) {
            String src = template.replace(SUB_TEMPLATE, c.getSource());
            Collection<Integer> significantLines = IntStream.of(c.getExpectedLines())
                    .mapToObj(line -> lineNumberOffset + line)
                    .collect(toList());
            testCases.add(new TestCase(src, significantLines, c.name()));
        }
        return testCases;
    }

    public interface Construction {
        String getSource();

        int[] getExpectedLines();

        String name();
    }
}
