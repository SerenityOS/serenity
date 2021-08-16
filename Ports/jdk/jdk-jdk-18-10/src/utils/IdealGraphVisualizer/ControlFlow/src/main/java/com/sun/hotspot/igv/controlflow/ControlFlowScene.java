/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.controlflow;

import com.sun.hotspot.igv.data.InputBlockEdge;
import com.sun.hotspot.igv.data.InputBlock;
import com.sun.hotspot.igv.data.InputGraph;
import com.sun.hotspot.igv.data.services.InputGraphProvider;
import com.sun.hotspot.igv.data.InputNode;
import com.sun.hotspot.igv.util.LookupHistory;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.RectangularSelectDecorator;
import org.netbeans.api.visual.action.RectangularSelectProvider;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.anchor.AnchorShape;
import org.netbeans.api.visual.router.RouterFactory;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.graph.layout.GraphLayout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.widget.ConnectionWidget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class ControlFlowScene extends GraphScene<InputBlock, InputBlockEdge> implements SelectProvider, MoveProvider, RectangularSelectDecorator, RectangularSelectProvider {

    private HashSet<BlockWidget> selection;
    private InputGraph oldGraph;
    private LayerWidget edgeLayer;
    private LayerWidget mainLayer;
    private LayerWidget selectLayer;
    private WidgetAction hoverAction = this.createWidgetHoverAction();
    private WidgetAction selectAction = new DoubleClickSelectAction(this);
    private WidgetAction moveAction = ActionFactory.createMoveAction(null, this);

    public ControlFlowScene() {
        selection = new HashSet<BlockWidget>();

        this.getInputBindings().setZoomActionModifiers(0);
        this.setLayout(LayoutFactory.createAbsoluteLayout());

        mainLayer = new LayerWidget(this);
        this.addChild(mainLayer);

        edgeLayer = new LayerWidget(this);
        this.addChild(edgeLayer);

        selectLayer = new LayerWidget(this);
        this.addChild(selectLayer);

        this.getActions().addAction(hoverAction);
        this.getActions().addAction(selectAction);
        this.getActions().addAction(ActionFactory.createRectangularSelectAction(this, selectLayer, this));
        this.getActions().addAction(ActionFactory.createMouseCenteredZoomAction(1.1));
    }

    public void setGraph(InputGraph g) {
        if (g == oldGraph) {
            return;
        }
        oldGraph = g;

        ArrayList<InputBlock> blocks = new ArrayList<InputBlock>(this.getNodes());
        for (InputBlock b : blocks) {
            removeNode(b);
        }

        ArrayList<InputBlockEdge> edges = new ArrayList<InputBlockEdge>(this.getEdges());
        for (InputBlockEdge e : edges) {
            removeEdge(e);
        }

        for (InputBlock b : g.getBlocks()) {
            addNode(b);
        }

        for (InputBlockEdge e : g.getBlockEdges()) {
            addEdge(e);
            assert g.getBlocks().contains(e.getFrom());
            assert g.getBlocks().contains(e.getTo());
            this.setEdgeSource(e, e.getFrom());
            this.setEdgeTarget(e, e.getTo());
        }

        GraphLayout<InputBlock, InputBlockEdge> layout = new HierarchicalGraphLayout<InputBlock, InputBlockEdge>();//GridGraphLayout();
        SceneLayout sceneLayout = LayoutFactory.createSceneGraphLayout(this, layout);
        sceneLayout.invokeLayout();

        this.validate();
    }

    public void clearSelection() {
        for (BlockWidget w : selection) {
            w.setState(w.getState().deriveSelected(false));
        }
        selection.clear();
        selectionChanged();
    }

    public void selectionChanged() {
        InputGraphProvider p = LookupHistory.getLast(InputGraphProvider.class);//)Utilities.actionsGlobalContext().lookup(InputGraphProvider.class);
        if (p != null) {
            Set<InputNode> inputNodes = new HashSet<InputNode>();
            for (BlockWidget w : selection) {
                inputNodes.addAll(w.getBlock().getNodes());
            }
            p.setSelectedNodes(inputNodes);
        }
    }

    public void addToSelection(BlockWidget widget) {
        widget.setState(widget.getState().deriveSelected(true));
        selection.add(widget);
        selectionChanged();
    }

    public void removeFromSelection(BlockWidget widget) {
        widget.setState(widget.getState().deriveSelected(false));
        selection.remove(widget);
        selectionChanged();
    }

    public boolean isAimingAllowed(Widget widget, Point point, boolean b) {
        return false;
    }

    public boolean isSelectionAllowed(Widget widget, Point point, boolean b) {
        return true;
    }

    public void select(Widget widget, Point point, boolean change) {
        if (widget == this) {
            clearSelection();
        } else {

            assert widget instanceof BlockWidget;
            BlockWidget bw = (BlockWidget) widget;
            if (change) {
                if (selection.contains(bw)) {
                    removeFromSelection(bw);
                } else {
                    addToSelection(bw);
                }
            } else {
                if (!selection.contains(bw)) {
                    clearSelection();
                    addToSelection(bw);
                }
            }
        }
    }

    public void movementStarted(Widget widget) {
    }

    public void movementFinished(Widget widget) {
    }

    public Point getOriginalLocation(Widget widget) {
        return widget.getPreferredLocation();
    }

    public void setNewLocation(Widget widget, Point location) {
        if (selection.contains(widget)) {
            // move entire selection
            Point originalLocation = getOriginalLocation(widget);
            int xOffset = location.x - originalLocation.x;
            int yOffset = location.y - originalLocation.y;
            for (Widget w : selection) {
                Point p = new Point(w.getPreferredLocation());
                p.translate(xOffset, yOffset);
                w.setPreferredLocation(p);
            }
        } else {
            widget.setPreferredLocation(location);
        }
    }

    public Widget createSelectionWidget() {
        Widget widget = new Widget(this);
        widget.setOpaque(false);
        widget.setBorder(BorderFactory.createLineBorder(Color.black, 2));
        widget.setForeground(Color.red);
        return widget;
    }

    public void performSelection(Rectangle rectangle) {

        if (rectangle.width < 0) {
            rectangle.x += rectangle.width;
            rectangle.width *= -1;
        }

        if (rectangle.height < 0) {
            rectangle.y += rectangle.height;
            rectangle.height *= -1;
        }

        boolean changed = false;
        for (InputBlock b : this.getNodes()) {
            BlockWidget w = (BlockWidget) findWidget(b);
            Rectangle r = new Rectangle(w.getBounds());
            r.setLocation(w.getLocation());
            if (r.intersects(rectangle)) {
                if (!selection.contains(w)) {
                    changed = true;
                    selection.add(w);
                    w.setState(w.getState().deriveSelected(true));
                }
            } else {
                if (selection.contains(w)) {
                    changed = true;
                    selection.remove(w);
                    w.setState(w.getState().deriveSelected(false));
                }
            }
        }

        if (changed) {
            selectionChanged();
        }

    }

    protected Widget attachNodeWidget(InputBlock node) {
        BlockWidget w = new BlockWidget(this, node);
        mainLayer.addChild(w);
        w.getActions().addAction(hoverAction);
        w.getActions().addAction(selectAction);
        w.getActions().addAction(moveAction);
        return w;
    }

    protected Widget attachEdgeWidget(InputBlockEdge edge) {
        BlockConnectionWidget w = new BlockConnectionWidget(this, edge);
        switch (edge.getState()) {
            case NEW:
                w.setBold(true);
                break;
            case DELETED:
                w.setDashed(true);
                break;
        }
        w.setRouter(RouterFactory.createDirectRouter());
        w.setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED);
        edgeLayer.addChild(w);
        return w;
    }

    protected void attachEdgeSourceAnchor(InputBlockEdge edge, InputBlock oldSourceNode, InputBlock sourceNode) {
        Widget w = this.findWidget(edge);
        assert w instanceof ConnectionWidget;
        ConnectionWidget cw = (ConnectionWidget) w;
        cw.setSourceAnchor(AnchorFactory.createRectangularAnchor(findWidget(sourceNode)));

    }

    protected void attachEdgeTargetAnchor(InputBlockEdge edge, InputBlock oldTargetNode, InputBlock targetNode) {
        Widget w = this.findWidget(edge);
        assert w instanceof ConnectionWidget;
        ConnectionWidget cw = (ConnectionWidget) w;
        cw.setTargetAnchor(AnchorFactory.createRectangularAnchor(findWidget(targetNode)));
    }
}
