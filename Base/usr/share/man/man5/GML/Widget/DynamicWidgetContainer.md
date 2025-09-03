## Name

GML DynamicWidgetContainer

## Description

Defines a container widget that will group its child widgets together so that they can be collapsed, expanded or detached to a new window as one unit. If DynamicWidgetContainers are nested within one DynamicWidgetContainer it is possible to move the positions of the child containers dynamically.

| Property              | Description                                                                                                                                               |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| config_domain         | Defines if the changes to the widget's view state should be persisted. It is required that the domain has been already pleged by the application.         |
| detached_size         | Defines a size that the detached widget window should initially have. If not defined, the window will have the current size of the widget.                |
| section_label         | The label that will be used for the section.                                                                                                              |
| show_controls         | Defines if the buttons and label should be visible or not. This allows e.g. a parent container to hide its controls but provide rearrenage functionality. |
| with_individual_order | Configured on a parent container to enable the persistence of rearranged child containers.                                                                |

## Synopsis

`@GUI::DynamicWidgetContainer`

## Examples

Simple container:

```gml
@GUI::DynamicWidgetContainer {
    section_label: "Section 1"

    @GUI::Widget {
    }

    @GUI::Widget {
    }
}
```

Nested containers with persistence:

```gml
@GUI::DynamicWidgetContainer {
    section_label: "Parent Section"
    config_domain: "abc"
    with_individual_order: true
    detached_size: [200, 640]

    @GUI::DynamicWidgetContainer {
        section_label: "Section 1"
        config_domain: "abc"

        @GUI::Widget {
        }

        @GUI::Widget {
        }
    }

    @GUI::DynamicWidgetContainer {
        section_label: "Section 2"
        config_domain: "abc"

        @GUI::Widget {
        }

        @GUI::Widget {
        }
    }
}
```
