package com.gym.system.controller.page;

import com.gym.system.controller.form.MemberForm;
import com.gym.system.controller.helper.DataExportHelper;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Member;
import com.gym.system.service.MemberService;
import com.gym.system.util.QrCodeService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class MembersPage {
    private final MemberService memberService;
    private final PageContext ctx;

    public MembersPage(MemberService memberService, PageContext ctx) {
        this.memberService = memberService;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                ctx.t("common.none"),
                ctx.t("status.ACTIVE"),
                ctx.t("status.EXPIRED")
        ));
        statusFilter.getSelectionModel().selectFirst();

        Button addBtn = new Button(ctx.t("members.add"));
        addBtn.getStyleClass().add("primary-button");

        Button editBtn = new Button(ctx.t("button.edit"));
        Button printCardBtn = new Button(ctx.t("print.memberCard"));
        Button exportBtn = new Button("📥 " + (ctx.isArabic() ? "تصدير إلى CSV" : "Export CSV"));

        HBox toolbar = new HBox(10, search, statusFilter, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, editBtn, printCardBtn, exportBtn);

        TableView<GymRepository.MemberRow> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.MemberRow> masterList = FXCollections.observableArrayList(memberService.getAllMembers());
        FilteredList<GymRepository.MemberRow> filtered = new FilteredList<>(masterList, m -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(memberService.getAllMembers());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        // Live Search & Status Filter
        Runnable updateFilter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String status = statusFilter.getValue();
            filtered.setPredicate(member -> {
                boolean matchesQuery = query.isEmpty()
                        || member.fullName().toLowerCase().contains(query)
                        || member.phone().toLowerCase().contains(query)
                        || member.email().toLowerCase().contains(query)
                        || member.plan().toLowerCase().contains(query);

                boolean matchesStatus = status == null || status.equals(ctx.t("common.none"))
                        || ctx.t("status." + member.status()).equalsIgnoreCase(status);

                return matchesQuery && matchesStatus;
            });
        };

        search.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.MemberRow::fullName, 1.4);
        TableFactory.addColumn(table, ctx.t("members.phone"), GymRepository.MemberRow::phone, 1);
        TableFactory.addColumn(table, ctx.t("members.email"), GymRepository.MemberRow::email, 1.3);
        TableFactory.addColumn(table, ctx.t("member.qrCode"), row -> QrCodeService.memberCode(row.id()), .9);
        TableFactory.addColumn(table, ctx.t("members.plan"), GymRepository.MemberRow::plan, 1);
        TableFactory.addColumn(table, ctx.t("members.status"), row -> ctx.t("status." + row.status()), .8);
        TableFactory.addColumn(table, ctx.t("members.notes"), GymRepository.MemberRow::medicalNotes, 1.2);

        // Actions Wiring
        addBtn.setOnAction(e -> {
            Optional<Member> member = MemberForm.showAdd(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            member.ifPresent(m -> {
                try {
                    memberService.addMember(m);
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        editBtn.setOnAction(e -> {
            GymRepository.MemberRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("members.select"));
                return;
            }
            Optional<Member> updated = MemberForm.showEdit(selected, ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            updated.ifPresent(m -> {
                try {
                    memberService.updateMember(selected.id(), m);
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        printCardBtn.setOnAction(e -> {
            GymRepository.MemberRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("members.select"));
                return;
            }
            ctx.printController().printMemberCard(selected);
        });

        exportBtn.setOnAction(e -> {
            DataExportHelper.exportMembers(
                    exportBtn.getScene().getWindow(),
                    filtered,
                    ctx.tFunction(),
                    path -> ctx.showInfo().accept((ctx.isArabic() ? "تم التصدير بنجاح:\n" : "Exported successfully:\n") + path),
                    err -> ctx.showError().accept(err)
            );
        });

        content.getChildren().addAll(toolbar, actionsBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }
}