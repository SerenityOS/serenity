/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace GUI {

class AbstractButton;
class AbstractTableView;
class AbstractView;
class Action;
class ActionGroup;
class Application;
class AutocompleteBox;
class AutocompleteProvider;
class BoxLayout;
class Button;
class CheckBox;
class ComboBox;
class Command;
class DragEvent;
class DropEvent;
class EditingEngine;
class FileSystemModel;
class Frame;
class GroupBox;
class HeaderView;
class HorizontalBoxLayout;
class HorizontalSlider;
class Icon;
class IconView;
class JsonArrayModel;
class KeyEvent;
class Label;
class Layout;
class ListView;
class Menu;
class Menubar;
class MenuItem;
class Model;
class ModelEditingDelegate;
class ModelIndex;
class MouseEvent;
class MultiPaintEvent;
class MultiView;
class OpacitySlider;
class PaintEvent;
class Painter;
class RadioButton;
class ResizeCorner;
class ResizeEvent;
class ScreenRectsChangeEvent;
class Scrollbar;
class AbstractScrollableWidget;
class Slider;
class SortingProxyModel;
class SpinBox;
class Splitter;
class StackWidget;
class Statusbar;
class TabWidget;
class TableView;
class TextBox;
class TextDocument;
class TextDocumentLine;
struct TextDocumentSpan;
class TextDocumentUndoCommand;
class TextEditor;
class ThemeChangeEvent;
class Toolbar;
class ToolbarContainer;
class TreeView;
class Variant;
class VerticalBoxLayout;
class VerticalSlider;
class WMEvent;
class Widget;
class Window;
class WindowServerConnection;

enum class ModelRole;
enum class SortOrder;

}

namespace WindowServer {
class ScreenLayout;
}
