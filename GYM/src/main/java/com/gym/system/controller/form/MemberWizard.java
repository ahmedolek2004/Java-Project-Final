package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Member;
import com.gym.system.model.Subscription;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.Function;

public final class MemberWizard {
    private static final List<String> GENDER_VALUES = List.of("UNSPECIFIED", "MALE", "FEMALE");

    private MemberWizard() {}

    public record NewMember(Member member, Subscription subscription, boolean paidNow) {}

    public record Renewal(Subscription subscription, boolean paidNow) {}

    public record Edit(Member member, boolean classChanged, Integer classId) {}

    private record Context(boolean dark, boolean arabic, Function<String, String> t, DoubleFunction<String> money) {}

    public static Optional<NewMember> showAdd(List<GymRepository.PlanRow> plans,
                                              List<GymRepository.ClassSeat> classes,
                                              boolean dark, boolean arabic, Function<String, String> t,
                                              DoubleFunction<String> money) {
        Context context = new Context(dark, arabic, t, money);
        DetailsStep details = new DetailsStep(context, null);
        PlanStep plan = new PlanStep(context, plans);
        ClassStep classStep = new ClassStep(context, classes, null);
        ReviewStep review = new ReviewStep(context, true);
        List<WizardDialog.Step> steps = List.of(
                new WizardDialog.Step(t.apply("wizard.stepDetails"), details.node(), details::validate),
                new WizardDialog.Step(t.apply("wizard.stepPlan"), plan.node(), plan::validate),
                new WizardDialog.Step(t.apply("wizard.stepClass"), classStep.node(), classStep::validate),
                new WizardDialog.Step(t.apply("wizard.stepReview"), review.node(), () -> null)
                        .onEnter(() -> review.show(details.member(0), plan, classStep)));
        boolean finished = WizardDialog.show(t.apply("wizard.finishAdd"), t.apply("wizard.finishAdd"), dark, arabic, t,
                steps);
        if (!finished) {
            return Optional.empty();
        }
        Subscription subscription = plan.subscription(0, classStep.selected());
        return Optional.of(new NewMember(details.member(0), subscription, review.paidNow()));
    }

    public static Optional<Edit> showEdit(GymRepository.MemberRow existing,
                                          List<GymRepository.ClassSeat> classes,
                                          boolean dark, boolean arabic, Function<String, String> t,
                                          DoubleFunction<String> money) {
        Context context = new Context(dark, arabic, t, money);
        DetailsStep details = new DetailsStep(context, existing);
        List<WizardDialog.Step> steps = new ArrayList<>();
        steps.add(new WizardDialog.Step(t.apply("wizard.stepDetails"), details.node(), details::validate));
        ClassStep classStep = null;
        boolean hasSubscription = existing.subscriptionId() != 0;
        if (hasSubscription) {
            classStep = new ClassStep(context, classes, existing.classId() == 0 ? null : existing.classId());
            steps.add(new WizardDialog.Step(t.apply("wizard.stepClass"), classStep.node(), classStep::validate));
        }
        if (!WizardDialog.show(t.apply("members.edit"), t.apply("wizard.finishSave"), dark, arabic, t, steps)) {
            return Optional.empty();
        }
        Integer classId = classStep == null ? null : classStep.selected().id();
        boolean changed = classStep != null && classId != existing.classId();
        return Optional.of(new Edit(details.member(existing.id()), changed, classId));
    }

    public static Optional<Renewal> showRenew(GymRepository.MemberRow member,
                                              List<GymRepository.PlanRow> plans,
                                              List<GymRepository.ClassSeat> classes,
                                              boolean dark, boolean arabic, Function<String, String> t,
                                              DoubleFunction<String> money) {
        Context context = new Context(dark, arabic, t, money);
        PlanStep plan = new PlanStep(context, plans);
        ClassStep classStep = new ClassStep(context, classes, member.classId() == 0 ? null : member.classId());
        ReviewStep review = new ReviewStep(context, false);
        Member reviewed = new Member(member.id(), member.fullName(), member.phone(), member.email(),
                member.gender(), member.medicalNotes());
        List<WizardDialog.Step> steps = List.of(
                new WizardDialog.Step(t.apply("wizard.stepPlan"), plan.node(), plan::validate),
                new WizardDialog.Step(t.apply("wizard.stepClass"), classStep.node(), classStep::validate),
                new WizardDialog.Step(t.apply("wizard.stepReview"), review.node(), () -> null)
                        .onEnter(() -> review.show(reviewed, plan, classStep)));
        if (!WizardDialog.show(t.apply("subscription.add") + " - " + member.fullName(),
                t.apply("wizard.finishRenew"), dark, arabic, t, steps)) {
            return Optional.empty();
        }
        return Optional.of(new Renewal(plan.subscription(member.id(), classStep.selected()), review.paidNow()));
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 0, 0));
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);
        labels.setPrefWidth(170);
        labels.setMaxWidth(190);
        ColumnConstraints inputs = new ColumnConstraints();
        inputs.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, inputs);
        return grid;
    }

    private static void addRow(GridPane grid, int row, String label, Node input) {
        Label caption = new Label(label);
        caption.getStyleClass().add("wizard-field-label");
        caption.setWrapText(true);
        grid.add(caption, 0, row);
        if (input instanceof javafx.scene.control.Control control) {
            control.setMaxWidth(Double.MAX_VALUE);
        }
        grid.add(input, 1, row);
    }

    private static final class DetailsStep {
        private final Context context;
        private final TextField name = new TextField();
        private final TextField phone = new TextField();
        private final TextField email = new TextField();
        private final ComboBox<String> gender;
        private final TextArea notes = new TextArea();
        private final GridPane grid = grid();

        DetailsStep(Context context, GymRepository.MemberRow existing) {
            this.context = context;
            Function<String, String> t = context.t();
            gender = new ComboBox<>(FXCollections.observableArrayList(
                    t.apply("gender.unspecified"), t.apply("gender.male"), t.apply("gender.female")));
            int genderIndex = 0;
            if (existing != null) {
                name.setText(existing.fullName());
                phone.setText(existing.phone());
                email.setText(existing.email());
                notes.setText(existing.medicalNotes() == null ? "" : existing.medicalNotes());
                genderIndex = Math.max(0, GENDER_VALUES.indexOf(existing.gender().toUpperCase()));
            }
            gender.getSelectionModel().select(genderIndex);
            notes.setPrefRowCount(3);
            notes.setMinHeight(84);
            notes.setPrefHeight(84);
            notes.setWrapText(true);
            addRow(grid, 0, t.apply("members.name"), name);
            addRow(grid, 1, t.apply("members.phone"), phone);
            addRow(grid, 2, t.apply("members.email"), email);
            addRow(grid, 3, t.apply("members.gender"), gender);
            if (existing == null || existing.medicalNotes() != null) {
                addRow(grid, 4, t.apply("members.notes"), notes);
            }
        }

        Node node() {
            return grid;
        }

        String validate() {
            if (name.getText().trim().isEmpty() || phone.getText().trim().isEmpty()) {
                return context.t().apply("wizard.requiredDetails");
            }
            return null;
        }

        Member member(int id) {
            return new Member(id, name.getText().trim(), phone.getText().trim(), email.getText().trim(),
                    GENDER_VALUES.get(Math.max(0, gender.getSelectionModel().getSelectedIndex())),
                    notes.getText().trim());
        }
    }

    private static final class PlanStep {
        private final Context context;
        private final ComboBox<GymRepository.PlanRow> planBox;
        private final DatePicker start = new DatePicker(LocalDate.now());
        private final TextField discount = new TextField("0");
        private final Label summary = new Label();
        private final VBox box = new VBox(14);

        PlanStep(Context context, List<GymRepository.PlanRow> plans) {
            this.context = context;
            Function<String, String> t = context.t();
            planBox = new ComboBox<>(FXCollections.observableArrayList(plans));
            planBox.setPromptText(t.apply("wizard.selectPlan"));
            planBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(GymRepository.PlanRow plan) {
                    return plan == null ? "" : describe(plan);
                }

                @Override
                public GymRepository.PlanRow fromString(String value) {
                    return null;
                }
            });
            summary.setWrapText(true);
            summary.getStyleClass().add("wizard-summary");
            GridPane grid = grid();
            addRow(grid, 0, t.apply("wizard.plan"), planBox);
            addRow(grid, 1, t.apply("subscription.start"), start);
            addRow(grid, 2, t.apply("subscription.discount"), discount);
            box.getChildren().add(grid);
            if (plans.isEmpty()) {
                Label empty = new Label(t.apply("wizard.noPlans"));
                empty.setWrapText(true);
                empty.getStyleClass().add("wizard-note");
                box.getChildren().add(empty);
            }
            box.getChildren().add(summary);
            planBox.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
            start.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
            discount.textProperty().addListener((obs, oldValue, newValue) -> refresh());
            refresh();
        }

        Node node() {
            return box;
        }

        private String describe(GymRepository.PlanRow plan) {
            Function<String, String> t = context.t();
            String length = plan.sessionBased()
                    ? plan.totalSessions() + " " + t.apply("wizard.sessionsUnit")
                    : plan.durationMonths() + " " + t.apply("wizard.monthsUnit");
            return plan.name() + "  •  " + length + "  •  " + context.money().apply(plan.price());
        }

        private Double discountValue() {
            try {
                double value = Double.parseDouble(discount.getText().trim().isEmpty() ? "0" : discount.getText().trim());
                return value >= 0 && value <= 100 ? value : null;
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private void refresh() {
            GymRepository.PlanRow plan = planBox.getValue();
            Double percent = discountValue();
            if (plan == null || percent == null) {
                summary.setText("");
                return;
            }
            double total = plan.price() * (1 - percent / 100);
            Function<String, String> t = context.t();
            String period = "";
            if (!plan.sessionBased() && start.getValue() != null) {
                period = "\n" + t.apply("subscription.end") + ": " + start.getValue().plusMonths(plan.durationMonths());
            }
            summary.setText(t.apply("receipt.total") + ": " + context.money().apply(total) + period);
        }

        String validate() {
            Function<String, String> t = context.t();
            if (planBox.getValue() == null) {
                return t.apply(planBox.getItems().isEmpty() ? "wizard.noPlans" : "wizard.selectPlan");
            }
            if (start.getValue() == null) {
                return t.apply("wizard.invalidDate");
            }
            if (discountValue() == null) {
                return t.apply("wizard.invalidDiscount");
            }
            return null;
        }

        GymRepository.PlanRow plan() {
            return planBox.getValue();
        }

        LocalDate startDate() {
            return start.getValue();
        }

        double discountPercent() {
            return discountValue() == null ? 0 : discountValue();
        }

        Subscription subscription(int memberId, GymRepository.ClassSeat seat) {
            GymRepository.PlanRow plan = planBox.getValue();
            LocalDate begin = start.getValue();
            boolean sessions = plan.sessionBased();
            Subscription subscription = new Subscription(0, memberId, seat.activityId(), plan.name(),
                    sessions ? Subscription.PlanType.SESSION_BASED : Subscription.PlanType.TIME_BASED,
                    sessions ? null : plan.durationMonths(), sessions ? plan.totalSessions() : null,
                    sessions ? plan.totalSessions() : null, begin,
                    sessions ? null : begin.plusMonths(plan.durationMonths()), plan.price(), discountPercent(),
                    Subscription.Status.ACTIVE);
            subscription.setClassId(seat.id());
            return subscription;
        }
    }

    private static final class ClassStep {
        private final Context context;
        private final ListView<GymRepository.ClassSeat> list;
        private final VBox box = new VBox(8);

        ClassStep(Context context, List<GymRepository.ClassSeat> classes, Integer selectedId) {
            this.context = context;
            Function<String, String> t = context.t();
            list = new ListView<>(FXCollections.observableArrayList(classes));
            list.setPrefHeight(230);
            list.getStyleClass().add("class-list");
            Label placeholder = new Label(t.apply("wizard.noClasses"));
            placeholder.setWrapText(true);
            list.setPlaceholder(placeholder);
            list.setCellFactory(view -> new ListCell<>() {
                @Override
                protected void updateItem(GymRepository.ClassSeat seat, boolean empty) {
                    super.updateItem(seat, empty);
                    if (empty || seat == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Label title = new Label(seat.title());
                    title.getStyleClass().add("class-cell-title");
                    String activity = context.arabic() ? seat.activityArabic() : seat.activityEnglish();
                    String coach = "-".equals(seat.coach()) ? "" : seat.coach() + "  •  ";
                    Label detail = new Label(t.apply("day." + seat.dayOfWeek()) + "  " + seat.startTime() + "–"
                            + seat.endTime() + "  •  " + activity + "  •  " + coach
                            + t.apply("wizard.seatsLeft").replace("{0}", String.valueOf(seat.free())));
                    detail.getStyleClass().add("class-cell-detail");
                    setGraphic(new VBox(2, title, detail));
                    setText(null);
                }
            });
            if (selectedId != null) {
                classes.stream().filter(seat -> seat.id() == selectedId).findFirst()
                        .ifPresent(seat -> list.getSelectionModel().select(seat));
            }
            Label hint = new Label(t.apply("wizard.classHint"));
            hint.setWrapText(true);
            hint.getStyleClass().add("wizard-note");
            box.getChildren().addAll(hint, list);
        }

        Node node() {
            return box;
        }

        String validate() {
            return list.getSelectionModel().getSelectedItem() == null
                    ? context.t().apply(list.getItems().isEmpty() ? "wizard.noClasses" : "wizard.selectClass") : null;
        }

        GymRepository.ClassSeat selected() {
            return list.getSelectionModel().getSelectedItem();
        }
    }

    private static final class ReviewStep {
        private final Context context;
        private final boolean showMember;
        private final GridPane grid = grid();
        private final CheckBox paid = new CheckBox();
        private final VBox box = new VBox(14, grid, paid);

        ReviewStep(Context context, boolean showMember) {
            this.context = context;
            this.showMember = showMember;
            paid.setText(context.t().apply("wizard.paidNow"));
            paid.setSelected(true);
        }

        Node node() {
            return box;
        }

        boolean paidNow() {
            return paid.isSelected();
        }

        void show(Member member, PlanStep plan, ClassStep classStep) {
            Function<String, String> t = context.t();
            grid.getChildren().clear();
            int row = 0;
            if (showMember) {
                row = line(row, t.apply("members.name"), member.getFullName());
                row = line(row, t.apply("members.phone"), member.getPhone());
            }
            GymRepository.PlanRow selectedPlan = plan.plan();
            row = line(row, t.apply("wizard.plan"), selectedPlan.name() + " (" + t.apply("subscription."
                    + selectedPlan.type()) + ")");
            if (selectedPlan.sessionBased()) {
                row = line(row, t.apply("members.sessions"), String.valueOf(selectedPlan.totalSessions()));
            } else {
                row = line(row, t.apply("members.period"), plan.startDate() + "  →  "
                        + plan.startDate().plusMonths(selectedPlan.durationMonths()));
            }
            GymRepository.ClassSeat seat = classStep.selected();
            String activity = context.arabic() ? seat.activityArabic() : seat.activityEnglish();
            row = line(row, t.apply("members.class"), seat.title() + "  •  " + t.apply("day." + seat.dayOfWeek())
                    + " " + seat.startTime() + "–" + seat.endTime() + "  •  " + activity);
            double total = selectedPlan.price() * (1 - plan.discountPercent() / 100);
            line(row, t.apply("receipt.total"), context.money().apply(total));
        }

        private int line(int row, String label, String value) {
            Label caption = new Label(label);
            caption.getStyleClass().add("wizard-field-label");
            Label text = new Label(value);
            text.setWrapText(true);
            text.getStyleClass().add("wizard-review-value");
            grid.add(caption, 0, row);
            grid.add(text, 1, row);
            return row + 1;
        }
    }
}
