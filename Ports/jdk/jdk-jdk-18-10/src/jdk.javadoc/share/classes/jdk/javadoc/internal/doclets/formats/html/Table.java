/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.lang.model.element.Element;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlAttr;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.toolkit.Content;

/**
 * An HTML container used to display summary tables for various kinds of elements.
 * This class historically used to generate an HTML {@code <table>} element but has been
 * updated to render elements as a stream of {@code <div>} elements that rely on
 * <a href="https://www.w3.org/TR/css-grid-1/">CSS Grid Layout</a> for styling.
 * This provides for more flexible layout options, such as splitting up table rows on
 * small displays.
 *
 * <p>The table should be used in three phases:
 * <ol>
 * <li>Configuration: the overall characteristics of the table should be specified
 * <li>Population: the content for the cells in each row should be added
 * <li>Generation: the HTML content and any associated JavaScript can be accessed
 * </ol>
 *
 * Many methods return the current object, to facilitate fluent builder-style usage.
 *
 * A table may support filtered views, which can be selected by clicking on
 * one of a list of tabs above the table. If the table does not support filtered
 * views, the caption element is typically displayed as a single (inactive)
 * tab.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Table extends Content {
    private final HtmlStyle tableStyle;
    private Content caption;
    private Map<Content, Predicate<Element>> tabMap;
    private Content defaultTab;
    private Set<Content> tabs;
    private HtmlStyle tabListStyle = HtmlStyle.tableTabs;
    private HtmlStyle activeTabStyle = HtmlStyle.activeTableTab;
    private HtmlStyle tabStyle = HtmlStyle.tableTab;
    private TableHeader header;
    private List<HtmlStyle> columnStyles;
    private List<HtmlStyle> stripedStyles = Arrays.asList(HtmlStyle.evenRowColor, HtmlStyle.oddRowColor);
    private final List<Content> bodyRows;
    private HtmlId id;
    private boolean alwaysShowDefaultTab = false;

    /**
     * Creates a builder for an HTML element representing a table.
     *
     * @param tableStyle the style class for the top-level {@code <div>} element
     */
    public Table(HtmlStyle tableStyle) {
        this.tableStyle = tableStyle;
        bodyRows = new ArrayList<>();
    }

    /**
     * Sets the caption for the table.
     * This is ignored if the table is configured to provide tabs to select
     * different subsets of rows within the table.
     *
     * @param captionContent the caption
     * @return this object
     */
    public Table setCaption(Content captionContent) {
        caption = getCaption(captionContent);
        return this;
    }

    /**
     * Adds a tab to the table.
     * Tabs provide a way to display subsets of rows, as determined by a
     * predicate for the tab, and an element associated with each row.
     * Tabs will appear left-to-right in the order they are added.
     *
     * @param label     the tab label
     * @param predicate the predicate
     * @return this object
     */
    public Table addTab(Content label, Predicate<Element> predicate) {
        if (tabMap == null) {
            tabMap = new LinkedHashMap<>();     // preserves order that tabs are added
            tabs = new HashSet<>();             // order not significant
        }
        tabMap.put(label, predicate);
        return this;
    }

    /**
     * Sets the label for the default tab, which displays all the rows in the table.
     * This tab will appear first in the left-to-right list of displayed tabs.
     *
     * @param label the default tab label
     * @return this object
     */
    public Table setDefaultTab(Content label) {
        defaultTab = label;
        return this;
    }

    /**
     * Sets whether to display the default tab even if tabs are empty or only contain a single tab.
     * @param showDefaultTab true if default tab should always be shown
     * @return this object
     */
    public Table setAlwaysShowDefaultTab(boolean showDefaultTab) {
        this.alwaysShowDefaultTab = showDefaultTab;
        return this;
    }

    /**
     * Sets the name of the styles used to display the tabs.
     *
     * @param tabListStyle      the style for the {@code <div>} element containing the tabs
     * @param activeTabStyle    the style for the active tab
     * @param tabStyle          the style for other tabs
     * @return  this object
     */
    public Table setTabStyles(HtmlStyle tabListStyle, HtmlStyle activeTabStyle, HtmlStyle tabStyle) {
        this.tabListStyle = tabListStyle;
        this.activeTabStyle = activeTabStyle;
        this.tabStyle = tabStyle;
        return this;
    }

    /**
     * Sets the header for the table.
     *
     * <p>Notes:
     * <ul>
     * <li>The column styles are not currently applied to the header, but probably should, eventually
     * </ul>
     *
     * @param header the header
     * @return this object
     */
    public Table setHeader(TableHeader header) {
        this.header = header;
        return this;
    }

    /**
     * Sets the styles used for {@code <tr>} tags, to give a "striped" appearance.
     * The defaults are currently {@code evenRowColor} and {@code oddRowColor}.
     *
     * @param evenRowStyle  the style to use for even-numbered rows
     * @param oddRowStyle   the style to use for odd-numbered rows
     * @return this object
     */
    public Table setStripedStyles(HtmlStyle evenRowStyle, HtmlStyle oddRowStyle) {
        stripedStyles = Arrays.asList(evenRowStyle, oddRowStyle);
        return this;
    }

    /**
     * Sets the styles for be used for the cells in each row.
     *
     * <p>Note:
     * <ul>
     * <li>The column styles are not currently applied to the header, but probably should, eventually
     * </ul>
     *
     * @param styles the styles
     * @return this object
     */
    public Table setColumnStyles(HtmlStyle... styles) {
        return setColumnStyles(Arrays.asList(styles));
    }

    /**
     * Sets the styles for be used for the cells in each row.
     *
     * <p>Note:
     * <ul>
     * <li>The column styles are not currently applied to the header, but probably should, eventually
     * </ul>
     *
     * @param styles the styles
     * @return this object
     */
    public Table setColumnStyles(List<HtmlStyle> styles) {
        columnStyles = styles;
        return this;
    }

    /**
     * Sets the id attribute of the table.
     * This is required if the table has tabs, in which case a subsidiary id
     * will be generated for the tabpanel. This subsidiary id is required for
     * the ARIA support.
     *
     * @param id the id
     * @return this object
     */
    public Table setId(HtmlId id) {
        this.id = id;
        return this;
    }

    /**
     * Adds a row of data to the table.
     * Each item of content should be suitable for use as the content of a
     * {@code <th>} or {@code <td>} cell.
     * This method should not be used when the table has tabs: use a method
     * that takes an {@code Element} parameter instead.
     *
     * @param contents the contents for the row
     */
    public void addRow(Content... contents) {
        addRow(null, Arrays.asList(contents));
    }

    /**
     * Adds a row of data to the table.
     * Each item of content should be suitable for use as the content of a
     * {@code <th>} or {@code <td> cell}.
     * This method should not be used when the table has tabs: use a method
     * that takes an {@code element} parameter instead.
     *
     * @param contents the contents for the row
     */
    public void addRow(List<Content> contents) {
        addRow(null, contents);
    }

    /**
     * Adds a row of data to the table.
     * Each item of content should be suitable for use as the content of a
     * {@code <th>} or {@code <td>} cell.
     *
     * If tabs have been added to the table, the specified element will be used
     * to determine whether the row should be displayed when any particular tab
     * is selected, using the predicate specified when the tab was
     * {@link #addTab(Content, Predicate) added}.
     *
     * @param element the element
     * @param contents the contents for the row
     * @throws NullPointerException if tabs have previously been added to the table
     *      and {@code element} is null
     */
    public void addRow(Element element, Content... contents) {
        addRow(element, Arrays.asList(contents));
    }

    /**
     * Adds a row of data to the table.
     * Each item of content should be suitable for use as the content of a
     * {@code <div>} cell.
     *
     * If tabs have been added to the table, the specified element will be used
     * to determine whether the row should be displayed when any particular tab
     * is selected, using the predicate specified when the tab was
     * {@link #addTab(Content, Predicate) added}.
     *
     * @param element the element
     * @param contents the contents for the row
     * @throws NullPointerException if tabs have previously been added to the table
     *      and {@code element} is null
     */
    public void addRow(Element element, List<Content> contents) {
        if (tabMap != null && element == null) {
            throw new NullPointerException();
        }
        if (contents.size() != columnStyles.size()) {
            throw new IllegalArgumentException("row content size does not match number of columns");
        }

        Content row = new ContentBuilder();

        HtmlStyle rowStyle = null;
        if (stripedStyles != null) {
            int rowIndex = bodyRows.size();
            rowStyle = stripedStyles.get(rowIndex % 2);
        }
        List<String> tabClasses = new ArrayList<>();
        if (tabMap != null) {
            // Construct a series of values to add to the HTML 'class' attribute for the cells of
            // this row, such that there is a default value and a value corresponding to each tab
            // whose predicate matches the element. The values correspond to the equivalent ids.
            // The values are used to determine the cells to make visible when a tab is selected.
            tabClasses.add(id.name());
            int tabIndex = 1;
            for (Map.Entry<Content, Predicate<Element>> e : tabMap.entrySet()) {
                Content label = e.getKey();
                Predicate<Element> predicate = e.getValue();
                if (predicate.test(element)) {
                    tabs.add(label);
                    tabClasses.add(HtmlIds.forTab(id, tabIndex).name());
                }
                tabIndex++;
            }
        }
        int colIndex = 0;
        for (Content c : contents) {
            HtmlStyle cellStyle = columnStyles.get(colIndex);
            // Replace invalid content with HtmlTree.EMPTY to make sure the cell isn't dropped
            HtmlTree cell = HtmlTree.DIV(cellStyle, c.isValid() ? c : HtmlTree.EMPTY);
            if (rowStyle != null) {
                cell.addStyle(rowStyle);
            }
            for (String tabClass : tabClasses) {
                cell.addStyle(tabClass);
            }
            row.add(cell);
            colIndex++;
        }
        bodyRows.add(row);
    }

    /**
     * Returns whether or not the table is empty.
     * The table is empty if it has no (body) rows.
     *
     * @return true if the table has no rows
     */
    public boolean isEmpty() {
        return bodyRows.isEmpty();
    }

    @Override
    public boolean write(Writer out, boolean atNewline) throws IOException {
        return toContent().write(out, atNewline);
    }

    /**
     * Returns the HTML for the table.
     *
     * @return the HTML
     */
    private Content toContent() {
        Content main;
        if (id != null) {
            main = new HtmlTree(TagName.DIV).setId(id);
        } else {
            main = new ContentBuilder();
        }
        HtmlStyle columnStyle = switch (columnStyles.size()) {
            case 2 -> HtmlStyle.twoColumnSummary;
            case 3 -> HtmlStyle.threeColumnSummary;
            case 4 -> HtmlStyle.fourColumnSummary;
            default -> throw new IllegalStateException();
        };

        HtmlTree table = new HtmlTree(TagName.DIV).setStyle(tableStyle).addStyle(columnStyle);
        if ((tabMap == null || tabs.size() == 1) && !alwaysShowDefaultTab) {
            if (tabMap == null) {
                main.add(caption);
            } else {
                main.add(getCaption(tabs.iterator().next()));
            }
            table.add(getTableBody());
            main.add(table);
        } else {
            HtmlTree tablist = new HtmlTree(TagName.DIV).setStyle(tabListStyle)
                    .put(HtmlAttr.ROLE, "tablist")
                    .put(HtmlAttr.ARIA_ORIENTATION, "horizontal");

            int tabIndex = 0;
            tablist.add(createTab(HtmlIds.forTab(id, tabIndex), activeTabStyle, true, defaultTab));
            table.put(HtmlAttr.ARIA_LABELLEDBY, HtmlIds.forTab(id, tabIndex).name());
            for (Content tabLabel : tabMap.keySet()) {
                tabIndex++;
                if (tabs.contains(tabLabel)) {
                    HtmlTree tab = createTab(HtmlIds.forTab(id, tabIndex), tabStyle, false, tabLabel);
                    tablist.add(tab);
                }
            }
            if (id == null) {
                throw new IllegalStateException("no id set for table");
            }
            HtmlTree tabpanel = new HtmlTree(TagName.DIV)
                    .setId(HtmlIds.forTabPanel(id))
                    .put(HtmlAttr.ROLE, "tabpanel");
            table.add(getTableBody());
            tabpanel.add(table);
            main.add(tablist);
            main.add(tabpanel);
        }
        return main;
    }

    private HtmlTree createTab(HtmlId tabId, HtmlStyle style, boolean defaultTab, Content tabLabel) {
        HtmlTree tab = new HtmlTree(TagName.BUTTON)
                .setId(tabId)
                .put(HtmlAttr.ROLE, "tab")
                .put(HtmlAttr.ARIA_SELECTED, defaultTab ? "true" : "false")
                .put(HtmlAttr.ARIA_CONTROLS, HtmlIds.forTabPanel(id).name())
                .put(HtmlAttr.TABINDEX, defaultTab ? "0" : "-1")
                .put(HtmlAttr.ONKEYDOWN, "switchTab(event)")
                .put(HtmlAttr.ONCLICK, "show('" + id.name() + "', '" + (defaultTab ? id : tabId).name()
                        + "', " + columnStyles.size() + ")")
                .setStyle(style);
        tab.add(tabLabel);
        return tab;
    }

    private Content getTableBody() {
        ContentBuilder tableContent = new ContentBuilder();
        tableContent.add(header);
        bodyRows.forEach(tableContent::add);
        return tableContent;
    }

    /**
     * Returns whether or not the table needs JavaScript support.
     * It requires such support if tabs have been added.
     *
     * @return true if JavaScript is required
     */
    public boolean needsScript() {
        return (tabs != null) && (tabs.size() > 1);
    }

    /**
     * Returns the script to be used in conjunction with the table.
     *
     * @return the script
     */
    public String getScript() {
        if (tabMap == null)
            throw new IllegalStateException();

        StringBuilder sb = new StringBuilder();

        // Add the variables defining the stylenames
        appendStyleInfo(sb,
                stripedStyles.get(0), stripedStyles.get(1), tabStyle, activeTabStyle);
        return sb.toString();
    }

    private void appendStyleInfo(StringBuilder sb, HtmlStyle... styles) {
        for (HtmlStyle style : styles) {
            sb.append("var ").append(style.name()).append(" = \"").append(style.cssName()).append("\";\n");
        }

    }

    private HtmlTree getCaption(Content title) {
        return new HtmlTree(TagName.DIV)
                .setStyle(HtmlStyle.caption)
                .add(HtmlTree.SPAN(title));
    }
}
