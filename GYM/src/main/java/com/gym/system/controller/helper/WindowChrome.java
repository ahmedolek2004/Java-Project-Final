package com.gym.system.controller.helper;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class WindowChrome {
    private static final String STATE_KEY = "gym.window.state";
    private static final double RESIZE_MARGIN = 6;
    private static final double MIN_WIDTH = 820;
    private static final double MIN_HEIGHT = 540;
    private static final double COMPACT_WIDTH = 1180;
    private static final double COMPACT_HEIGHT = 720;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int TOP = 4;
    private static final int BOTTOM = 8;

    private static Stage mainStage;

    private WindowChrome() {}

    private static final class State {
        final BooleanProperty maximized = new SimpleBooleanProperty(false);
        double x;
        double y;
        double width;
        double height;
    }

    private static final class ResizeGesture {
        int edges;
        double screenX;
        double screenY;
        double x;
        double y;
        double width;
        double height;
    }

    public static Parent wrap(Stage stage, Parent content, String title) {
        mainStage = stage;
        State state = state(stage);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("window-shell");
        shell.setCenter(content);
        shell.setTop(createMainTitleBar(stage, title, state));

        ChangeListener<Boolean> maximizedListener = (observable, oldValue, maximized) ->
                setStyleClass(shell, "maximized", maximized);
        setStyleClass(shell, "maximized", state.maximized.get());
        state.maximized.addListener(maximizedListener);
        shell.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                state.maximized.removeListener(maximizedListener);
            }
        });

        ChangeListener<Number> sizeListener = (observable, oldValue, newValue) ->
                setStyleClass(shell, "compact",
                        shell.getWidth() > 0 && (shell.getWidth() < COMPACT_WIDTH || shell.getHeight() < COMPACT_HEIGHT));
        shell.widthProperty().addListener(sizeListener);
        shell.heightProperty().addListener(sizeListener);

        installResize(stage, shell, state);
        return shell;
    }

    public static double[] fittedSize(Stage stage, double preferredWidth, double preferredHeight) {
        Rectangle2D area = visualBounds(stage);
        double minWidth = Math.min(MIN_WIDTH, area.getWidth());
        double minHeight = Math.min(MIN_HEIGHT, area.getHeight());
        return new double[]{
                Math.max(minWidth, Math.min(preferredWidth, area.getWidth())),
                Math.max(minHeight, Math.min(preferredHeight, area.getHeight()))
        };
    }

    public static void fit(Stage stage, double preferredWidth, double preferredHeight) {
        State state = state(stage);
        Rectangle2D area = visualBounds(stage);
        double minWidth = Math.min(MIN_WIDTH, area.getWidth());
        double minHeight = Math.min(MIN_HEIGHT, area.getHeight());
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        double[] size = fittedSize(stage, preferredWidth, preferredHeight);
        double width = size[0];
        double height = size[1];
        state.maximized.set(false);
        setBounds(stage, area.getMinX() + (area.getWidth() - width) / 2,
                area.getMinY() + (area.getHeight() - height) / 2, width, height);
    }

    public static void toggleMaximize(Stage stage) {
        State state = state(stage);
        if (state.maximized.get()) {
            setBounds(stage, state.x, state.y, state.width, state.height);
            state.maximized.set(false);
            return;
        }
        state.x = stage.getX();
        state.y = stage.getY();
        state.width = stage.getWidth();
        state.height = stage.getHeight();
        Rectangle2D area = visualBounds(stage);
        setBounds(stage, area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
        state.maximized.set(true);
    }

    public static void decorateDialog(Dialog<?> dialog, String title) {
        dialog.initStyle(StageStyle.UNDECORATED);
        if (mainStage != null && mainStage.isShowing()) {
            dialog.initOwner(mainStage);
        }
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("chrome-dialog")) {
            pane.getStyleClass().add("chrome-dialog");
        }
        pane.setHeader(createDialogTitleBar(dialog, title));
    }

    private static HBox createMainTitleBar(Stage stage, String title, State state) {
        Label mark = new Label("F");
        mark.getStyleClass().add("title-mark");
        Label text = new Label(title);
        text.getStyleClass().add("title-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimize = titleButton(icon("M0,5 L10,5"), "title-button");
        minimize.setOnAction(event -> stage.setIconified(true));

        SVGPath maximizeIcon = icon("M0.5,0.5 L9.5,0.5 L9.5,9.5 L0.5,9.5 Z");
        SVGPath restoreIcon = icon("M2.5,2.5 L2.5,0.5 L9.5,0.5 L9.5,7.5 L7.5,7.5 M0.5,2.5 L7.5,2.5 L7.5,9.5 L0.5,9.5 Z");
        Button maximize = titleButton(state.maximized.get() ? restoreIcon : maximizeIcon, "title-button");
        maximize.setOnAction(event -> toggleMaximize(stage));
        ChangeListener<Boolean> iconListener = (observable, oldValue, maximized) ->
                maximize.setGraphic(maximized ? restoreIcon : maximizeIcon);
        state.maximized.addListener(iconListener);

        Button close = titleButton(icon("M0,0 L10,10 M10,0 L0,10"), "title-button", "title-close");
        close.setOnAction(event -> stage.close());

        HBox bar = new HBox(10, mark, text, spacer, minimize, maximize, close);
        bar.getStyleClass().add("title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        bar.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                state.maximized.removeListener(iconListener);
            }
        });
        installDrag(bar, () -> stage, state);
        bar.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                    && !isButton(event.getTarget())) {
                toggleMaximize(stage);
            }
        });
        return bar;
    }

    private static HBox createDialogTitleBar(Dialog<?> dialog, String title) {
        Label mark = new Label("F");
        mark.getStyleClass().add("title-mark");
        Label text = new Label(title == null ? "" : title);
        text.getStyleClass().add("title-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = titleButton(icon("M0,0 L10,10 M10,0 L0,10"), "title-button", "title-close");
        close.setOnAction(event -> dialog.close());

        HBox bar = new HBox(10, mark, text, spacer, close);
        bar.getStyleClass().addAll("title-bar", "dialog-title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        bar.setMaxWidth(Double.MAX_VALUE);
        installDrag(bar, () -> bar.getScene() == null ? null : bar.getScene().getWindow(), null);
        return bar;
    }

    private static void installDrag(Node bar, java.util.function.Supplier<? extends Window> windowSupplier,
                                    State state) {
        double[] offset = new double[2];
        boolean[] dragging = new boolean[1];
        bar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Window window = windowSupplier.get();
            dragging[0] = window != null && event.getButton() == MouseButton.PRIMARY
                    && !isButton(event.getTarget());
            if (dragging[0]) {
                offset[0] = event.getScreenX() - window.getX();
                offset[1] = event.getScreenY() - window.getY();
            }
        });
        bar.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            Window window = windowSupplier.get();
            if (!dragging[0] || window == null) {
                return;
            }
            if (state != null && state.maximized.get() && window instanceof Stage stage) {
                double ratio = event.getSceneX() / Math.max(1, stage.getWidth());
                state.maximized.set(false);
                setBounds(stage, stage.getX(), stage.getY(), state.width, state.height);
                offset[0] = state.width * ratio;
                offset[1] = event.getSceneY();
            }
            window.setX(event.getScreenX() - offset[0]);
            window.setY(event.getScreenY() - offset[1]);
        });
    }

    private static void installResize(Stage stage, Region shell, State state) {
        ResizeGesture gesture = new ResizeGesture();
        shell.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (shell.getScene() == null) {
                return;
            }
            int edges = state.maximized.get() ? 0
                    : edgesAt(event.getSceneX(), event.getSceneY(), shell.getWidth(), shell.getHeight());
            shell.getScene().setCursor(cursorFor(edges));
        });
        shell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            gesture.edges = state.maximized.get() || event.getButton() != MouseButton.PRIMARY ? 0
                    : edgesAt(event.getSceneX(), event.getSceneY(), shell.getWidth(), shell.getHeight());
            if (gesture.edges != 0) {
                gesture.screenX = event.getScreenX();
                gesture.screenY = event.getScreenY();
                gesture.x = stage.getX();
                gesture.y = stage.getY();
                gesture.width = stage.getWidth();
                gesture.height = stage.getHeight();
                event.consume();
            }
        });
        shell.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (gesture.edges == 0) {
                return;
            }
            double dx = event.getScreenX() - gesture.screenX;
            double dy = event.getScreenY() - gesture.screenY;
            double minWidth = Math.max(stage.getMinWidth(), 1);
            double minHeight = Math.max(stage.getMinHeight(), 1);
            double x = gesture.x;
            double y = gesture.y;
            double width = gesture.width;
            double height = gesture.height;
            if ((gesture.edges & RIGHT) != 0) {
                width = Math.max(minWidth, gesture.width + dx);
            }
            if ((gesture.edges & BOTTOM) != 0) {
                height = Math.max(minHeight, gesture.height + dy);
            }
            if ((gesture.edges & LEFT) != 0) {
                width = Math.max(minWidth, gesture.width - dx);
                x = gesture.x + gesture.width - width;
            }
            if ((gesture.edges & TOP) != 0) {
                height = Math.max(minHeight, gesture.height - dy);
                y = gesture.y + gesture.height - height;
            }
            setBounds(stage, x, y, width, height);
            event.consume();
        });
        shell.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (gesture.edges != 0) {
                gesture.edges = 0;
                event.consume();
            }
        });
    }

    private static int edgesAt(double x, double y, double width, double height) {
        int edges = 0;
        if (x <= RESIZE_MARGIN) {
            edges |= LEFT;
        } else if (x >= width - RESIZE_MARGIN) {
            edges |= RIGHT;
        }
        if (y <= RESIZE_MARGIN) {
            edges |= TOP;
        } else if (y >= height - RESIZE_MARGIN) {
            edges |= BOTTOM;
        }
        return edges;
    }

    private static Cursor cursorFor(int edges) {
        return switch (edges) {
            case LEFT -> Cursor.W_RESIZE;
            case RIGHT -> Cursor.E_RESIZE;
            case TOP -> Cursor.N_RESIZE;
            case BOTTOM -> Cursor.S_RESIZE;
            case LEFT | TOP -> Cursor.NW_RESIZE;
            case RIGHT | TOP -> Cursor.NE_RESIZE;
            case LEFT | BOTTOM -> Cursor.SW_RESIZE;
            case RIGHT | BOTTOM -> Cursor.SE_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private static Rectangle2D visualBounds(Stage stage) {
        double centerX = stage.getX() + stage.getWidth() / 2;
        double centerY = stage.getY() + stage.getHeight() / 2;
        if (!Double.isNaN(centerX) && !Double.isNaN(centerY)) {
            for (Screen screen : Screen.getScreensForRectangle(centerX, centerY, 1, 1)) {
                return screen.getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private static void setBounds(Stage stage, double x, double y, double width, double height) {
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    private static State state(Stage stage) {
        return (State) stage.getProperties().computeIfAbsent(STATE_KEY, key -> new State());
    }

    private static Button titleButton(SVGPath icon, String... styleClasses) {
        Button button = new Button();
        button.setGraphic(icon);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll(styleClasses);
        return button;
    }

    private static SVGPath icon(String path) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("title-icon");
        return icon;
    }

    private static boolean isButton(Object target) {
        Node node = target instanceof Node value ? value : null;
        while (node != null) {
            if (node instanceof Button) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private static void setStyleClass(Node node, String styleClass, boolean present) {
        node.getStyleClass().remove(styleClass);
        if (present) {
            node.getStyleClass().add(styleClass);
        }
    }
}
