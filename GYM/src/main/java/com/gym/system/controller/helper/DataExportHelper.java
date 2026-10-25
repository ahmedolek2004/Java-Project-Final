package com.gym.system.controller.helper;

import com.gym.system.dao.GymRepository;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DataExportHelper {
    private DataExportHelper() {}

    public static void exportMembers(Window window, List<GymRepository.MemberRow> members,
                                     Function<String, String> t, Consumer<String> onSuccess, Consumer<String> onError) {
        File file = chooseSaveFile(window, "members_export.csv", "CSV Files (*.csv)", "*.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for proper Arabic support in Excel
            writer.write('\ufeff');
            writer.println(String.join(",",
                    escape("ID"),
                    escape(t.apply("members.name")),
                    escape(t.apply("members.phone")),
                    escape(t.apply("members.email")),
                    escape(t.apply("members.gender")),
                    escape(t.apply("members.plan")),
                    escape(t.apply("members.status")),
                    escape(t.apply("members.notes"))
            ));

            for (GymRepository.MemberRow m : members) {
                writer.println(String.join(",",
                        escape(String.valueOf(m.id())),
                        escape(m.fullName()),
                        escape(m.phone()),
                        escape(m.email()),
                        escape(m.gender()),
                        escape(m.plan()),
                        escape(m.status()),
                        escape(m.medicalNotes())
                ));
            }
            onSuccess.accept(file.getAbsolutePath());
        } catch (Exception e) {
            onError.accept(e.getMessage());
        }
    }

    public static void exportPayments(Window window, List<GymRepository.Payment> payments,
                                      Function<String, String> t, Consumer<String> onSuccess, Consumer<String> onError) {
        File file = chooseSaveFile(window, "finance_payments.csv", "CSV Files (*.csv)", "*.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.println(String.join(",",
                    escape(t.apply("members.name")),
                    escape(t.apply("finance.description")),
                    escape(t.apply("finance.amount")),
                    escape(t.apply("finance.date")),
                    escape(t.apply("finance.status"))
            ));

            for (GymRepository.Payment p : payments) {
                writer.println(String.join(",",
                        escape(p.member()),
                        escape(p.description()),
                        escape(String.valueOf(p.amount())),
                        escape(p.date()),
                        escape(p.status())
                ));
            }
            onSuccess.accept(file.getAbsolutePath());
        } catch (Exception e) {
            onError.accept(e.getMessage());
        }
    }

    private static File chooseSaveFile(Window window, String initialName, String filterDesc, String filterPattern) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterDesc, filterPattern));
        return chooser.showSaveDialog(window);
    }

    private static String escape(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
