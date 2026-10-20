package com.gym.system.util;

import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.control.Separator;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

public final class PrintService {
    private PrintService() {
    }

    public static boolean print(Window owner, Node document) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            throw new IllegalStateException("No system printer is available.");
        }
        if (!job.showPrintDialog(owner)) {
            job.cancelJob();
            return false;
        }
        List<Node> pages = paginate(document, job);
        for (Node page : pages) {
            page.applyCss();
            if (page instanceof Parent parent) {
                parent.layout();
            }
            page.autosize();
            if (!job.printPage(page)) {
                job.cancelJob();
                throw new IllegalStateException("The printer could not print the document.");
            }
        }
        if (!job.endJob()) {
            throw new IllegalStateException("The printer did not complete the print job.");
        }
        return true;
    }

    private static List<Node> paginate(Node document, PrinterJob job) {
        if (!(document instanceof VBox content)) {
            return List.of(document);
        }
        var layout = job.getJobSettings().getPageLayout();
        double pageWidth = layout.getPrintableWidth();
        double pageHeight = layout.getPrintableHeight();
        content.setMinWidth(0);
        content.setPrefWidth(pageWidth);
        content.setMaxWidth(pageWidth);
        content.applyCss();
        content.autosize();

        List<Node> children = new ArrayList<>(content.getChildren());
        int headerCount = Math.min(3, children.size());
        double bodyWidth = Math.max(1, pageWidth - 56);
        double headerHeight = 56;
        for (int index = 0; index < headerCount; index++) {
            headerHeight += nodeHeight(children.get(index), bodyWidth) + content.getSpacing();
        }

        List<List<Node>> pageBodies = new ArrayList<>();
        List<Node> body = new ArrayList<>();
        double usedHeight = headerHeight;
        for (int index = headerCount; index < children.size(); index++) {
            Node child = children.get(index);
            double childHeight = nodeHeight(child, bodyWidth) + content.getSpacing();
            if (!body.isEmpty() && usedHeight + childHeight > pageHeight) {
                pageBodies.add(body);
                body = new ArrayList<>();
                usedHeight = headerHeight;
            }
            body.add(child);
            usedHeight += childHeight;
        }
        pageBodies.add(body);

        List<Node> pages = new ArrayList<>(pageBodies.size());
        content.getChildren().clear();
        for (List<Node> pageBody : pageBodies) {
            VBox page = new VBox(content.getSpacing());
            page.setStyle(content.getStyle());
            page.setNodeOrientation(content.getNodeOrientation());
            page.setMinWidth(0);
            page.setPrefWidth(pageWidth);
            page.setMaxWidth(pageWidth);
            for (int index = 0; index < headerCount; index++) {
                page.getChildren().add(copyHeader(children.get(index)));
            }
            page.getChildren().addAll(pageBody);
            pages.add(page);
        }
        return pages;
    }

    private static double nodeHeight(Node node, double width) {
        if (node instanceof Region region) {
            return region.prefHeight(width);
        }
        return node.getBoundsInLocal().getHeight();
    }

    private static Node copyHeader(Node node) {
        if (node instanceof Label label) {
            Label copy = new Label(label.getText());
            copy.setStyle(label.getStyle());
            copy.setWrapText(label.isWrapText());
            copy.setMaxWidth(Double.MAX_VALUE);
            return copy;
        }
        if (node instanceof Separator) {
            return new Separator();
        }
        throw new IllegalArgumentException("Unsupported print document header node.");
    }
}
