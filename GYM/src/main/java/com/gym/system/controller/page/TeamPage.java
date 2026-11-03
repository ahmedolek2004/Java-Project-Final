package com.gym.system.controller.page;

import com.gym.system.controller.form.UserForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.model.User;
import com.gym.system.service.TeamService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class TeamPage {
    private final TeamService service;
    private final PageContext ctx;

    public TeamPage(TeamService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        Button addBtn = new Button(ctx.t("team.add"));
        addBtn.getStyleClass().add("primary-button");

        Button editBtn = new Button(ctx.t("button.editStaff"));
        Button changePassBtn = new Button(ctx.t("team.changePassword"));
        Button deactivateBtn = new Button(ctx.t("team.deactivate"));

        HBox toolbar = new HBox(10, search, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, editBtn, changePassBtn, deactivateBtn);

        TableView<User> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<User> masterList = FXCollections.observableArrayList(service.getUsers());
        FilteredList<User> filtered = new FilteredList<>(masterList, u -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getUsers());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(u -> q.isEmpty()
                    || u.username().toLowerCase().contains(q)
                    || u.fullName().toLowerCase().contains(q)
                    || u.role().toLowerCase().contains(q)
                    || (u.phone() != null && u.phone().toLowerCase().contains(q)));
        });

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("login.username"), User::username, 1);
        TableFactory.addColumn(table, ctx.t("members.name"), User::fullName, 1.2);
        TableFactory.addColumn(table, ctx.t("team.role"),
                row -> "ADMIN".equals(row.role()) ? ctx.t("role.admin") : ctx.t("role.receptionist"), 1);
        TableFactory.addColumn(table, ctx.t("members.phone"),
                row -> row.phone() == null ? "" : row.phone(), 1);
        TableFactory.addColumn(table, ctx.t("team.salary"), row -> ctx.money(row.salary()), .9);

        // Actions Wiring
        addBtn.setOnAction(e -> {
            Optional<UserForm.NewUserData> data = UserForm.showAdd(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            data.ifPresent(d -> {
                try {
                    service.addUser(d.username(), d.password(), d.fullName(), d.role(), d.phone(), d.salary());
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        editBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("team.select"));
                return;
            }
            Optional<UserForm.EditUserData> data = UserForm.showEdit(selected, ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            data.ifPresent(d -> {
                try {
                    service.updateUser(selected.id(), d.fullName(), d.role(), d.phone(), d.salary());
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        changePassBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("team.select"));
                return;
            }
            Optional<char[]> pass = UserForm.showChangePassword(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            pass.ifPresent(p -> {
                try {
                    service.updatePassword(selected.id(), p);
                    ctx.showInfo().accept(ctx.t("team.passwordChanged"));
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        deactivateBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("team.select"));
                return;
            }
            if (ctx.currentUser() != null && selected.id() == ctx.currentUser().id()) {
                ctx.showInfo().accept(ctx.t("team.cannotDeactivateSelf"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("team.confirmDeactivate")))) {
                try {
                    service.deactivateUser(selected.id());
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            }
        });

        content.getChildren().addAll(toolbar, actionsBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }
}