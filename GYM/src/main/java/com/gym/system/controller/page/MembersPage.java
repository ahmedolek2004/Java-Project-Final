package com.gym.system.controller.page;

import com.gym.system.controller.form.MemberForm;
import com.gym.system.controller.helper.DataExportHelper;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Member;
import com.gym.system.service.MemberService;
import com.gym.system.service.SubscriptionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.List;
import java.util.Optional;

public class MembersPage {
    private static final List<String> STATUS_FILTERS = List.of("ALL", "ACTIVE", "FROZEN", "EXPIRED", "NONE");

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;
    private final PageContext ctx;

    public MembersPage(MemberService memberService, SubscriptionService subscriptionService, PageContext ctx) {
        this.memberService = memberService;
        this.subscriptionService = subscriptionService;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                ctx.t("common.all"),
                ctx.t("status.ACTIVE"),
                ctx.t("status.FROZEN"),
                ctx.t("status.EXPIRED"),
                ctx.t("status.NONE")
        ));
        statusFilter.getSelectionModel().selectFirst();

        Button addBtn = new Button(ctx.t("members.add"));
        addBtn.getStyleClass().add("primary-button");
        Button exportBtn = new Button("📥 " + (ctx.isArabic() ? "تصدير إلى CSV" : "Export CSV"));

        HBox toolbar = new HBox(10, search, statusFilter, addBtn, exportBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        TableView<GymRepository.MemberRow> table = TableFactory.createTable(ctx.t("table.empty"));
        ObservableList<GymRepository.MemberRow> masterList =
                FXCollections.observableArrayList(memberService.getAllMembers());
        FilteredList<GymRepository.MemberRow> filtered = new FilteredList<>(masterList, member -> true);
        table.setItems(filtered);
        table.setMinHeight(180);

        MemberDetailPanel detail = new MemberDetailPanel(memberService, subscriptionService, ctx,
                memberId -> refresh(masterList, table, memberId));

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.MemberRow::fullName, 1.4);
        TableFactory.addColumn(table, ctx.t("members.phone"), GymRepository.MemberRow::phone, 1.4);
        TableFactory.addColumn(table, ctx.t("members.class"),
                row -> ctx.isArabic() ? row.activityArabic() : row.activityEnglish(), 1);
        TableFactory.addColumn(table, ctx.t("subscription.plan"), GymRepository.MemberRow::plan, 1.1);
        TableFactory.addColumn(table, ctx.t("members.status"), row -> ctx.t("status." + row.status()), 1.3);

        table.getColumns().forEach(column -> column.setMinWidth(70));

        Runnable updateFilter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            int index = Math.max(0, statusFilter.getSelectionModel().getSelectedIndex());
            String status = STATUS_FILTERS.get(index);
            filtered.setPredicate(member -> {
                String activity = (member.activityEnglish() + " " + member.activityArabic()).toLowerCase();
                boolean matchesQuery = query.isEmpty()
                        || member.fullName().toLowerCase().contains(query)
                        || member.phone().toLowerCase().contains(query)
                        || member.email().toLowerCase().contains(query)
                        || member.plan().toLowerCase().contains(query)
                        || activity.contains(query);
                boolean matchesStatus = "ALL".equals(status) || status.equals(member.status());
                return matchesQuery && matchesStatus;
            });
        };
        search.textProperty().addListener((obs, oldValue, newValue) -> updateFilter.run());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> updateFilter.run());

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> detail.show(newValue));

        addBtn.setOnAction(event -> {
            Optional<Member> member = MemberForm.showAdd(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            member.ifPresent(value -> {
                try {
                    memberService.addMember(value);
                    search.clear();
                    statusFilter.getSelectionModel().selectFirst();
                    masterList.setAll(memberService.getAllMembers());
                    table.getSelectionModel().selectFirst();
                } catch (Exception exception) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
                }
            });
        });

        exportBtn.setOnAction(event -> DataExportHelper.exportMembers(
                exportBtn.getScene().getWindow(),
                filtered,
                ctx.tFunction(),
                path -> ctx.showInfo().accept((ctx.isArabic() ? "تم التصدير بنجاح:\n" : "Exported successfully:\n") + path),
                error -> ctx.showError().accept(error)
        ));

        HBox body = new HBox(14, table, detail.node());
        HBox.setHgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(toolbar, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private void refresh(ObservableList<GymRepository.MemberRow> masterList,
                         TableView<GymRepository.MemberRow> table, int memberId) {
        try {
            masterList.setAll(memberService.getAllMembers());
            for (GymRepository.MemberRow row : table.getItems()) {
                if (row.id() == memberId) {
                    table.getSelectionModel().select(row);
                    table.scrollTo(row);
                    break;
                }
            }
        } catch (SQLException exception) {
            ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
        }
    }
}
