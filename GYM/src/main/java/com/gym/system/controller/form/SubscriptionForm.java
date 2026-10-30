package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Subscription;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class SubscriptionForm {
    private SubscriptionForm() {}

    public static Optional<Subscription> show(List<GymRepository.MemberRow> members,
                                              List<GymRepository.ActivityRow> activities,
                                              boolean isDark, boolean isArabic,
                                              Function<String, String> t) {
        return show(members, activities, null, isDark, isArabic, t);
    }

    public static Optional<Subscription> show(List<GymRepository.MemberRow> members,
                                              List<GymRepository.ActivityRow> activities,
                                              Integer preselectedMemberId,
                                              boolean isDark, boolean isArabic,
                                              Function<String, String> t) {
        if (members == null || members.isEmpty()) {
            return Optional.empty();
        }

        ComboBox<String> memberBox = new ComboBox<>();
        members.forEach(m -> memberBox.getItems().add(m.id() + " | " + m.fullName() + " (" + m.phone() + ")"));
        memberBox.getSelectionModel().selectFirst();
        if (preselectedMemberId != null) {
            String prefix = preselectedMemberId + " | ";
            memberBox.getItems().stream().filter(item -> item.startsWith(prefix)).findFirst()
                    .ifPresent(item -> memberBox.getSelectionModel().select(item));
            memberBox.setDisable(true);
        }

        TextField planNameField = new TextField();

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("subscription.TIME_BASED"),
                t.apply("subscription.SESSION_BASED")
        ));
        typeBox.getSelectionModel().selectFirst();

        TextField durationField = new TextField("1");
        TextField priceField = new TextField();
        TextField discountField = new TextField("0");
        DatePicker startDatePicker = new DatePicker(LocalDate.now());

        ComboBox<String> activityBox = new ComboBox<>();
        activityBox.getItems().add(t.apply("common.none"));
        if (activities != null) {
            activities.forEach(a -> {
                String actName = isArabic ? a.nameArabic() : a.nameEnglish();
                activityBox.getItems().add(a.id() + " | " + actName);
            });
        }
        activityBox.getSelectionModel().selectFirst();

        List<String> values = FormDialog.show(t.apply("subscription.add"), isDark, isArabic,
                FormDialog.field(t.apply("members.name"), memberBox),
                FormDialog.field(t.apply("subscription.plan"), planNameField),
                FormDialog.field(t.apply("subscription.type"), typeBox),
                FormDialog.field(t.apply("subscription.duration"), durationField),
                FormDialog.field(t.apply("finance.amount"), priceField),
                FormDialog.field(t.apply("subscription.discount"), discountField),
                FormDialog.field(t.apply("subscription.start"), startDatePicker),
                FormDialog.field(t.apply("activity.title"), activityBox)
        );

        if (values.isEmpty()) return Optional.empty();

        String memberVal = memberBox.getValue();
        String planName = values.get(1).trim();
        if (memberVal == null || planName.isEmpty()) {
            return Optional.empty();
        }

        try {
            int memberId = Integer.parseInt(memberVal.substring(0, memberVal.indexOf(" | ")));
            boolean sessionBased = t.apply("subscription.SESSION_BASED").equals(typeBox.getValue());
            int duration = Integer.parseInt(values.get(3).trim());
            double price = Double.parseDouble(values.get(4).trim());
            double discount = Double.parseDouble(values.get(5).trim());

            if (duration <= 0 || price < 0 || discount < 0 || discount > 100) {
                return Optional.empty();
            }

            LocalDate start = values.get(6).isEmpty() ? LocalDate.now() : LocalDate.parse(values.get(6));
            LocalDate end = sessionBased ? null : start.plusMonths(duration);

            Integer activityId = null;
            String actVal = values.get(7);
            if (actVal != null && !actVal.equals(t.apply("common.none")) && actVal.contains(" | ")) {
                activityId = Integer.parseInt(actVal.substring(0, actVal.indexOf(" | ")));
            }

            return Optional.of(new Subscription(
                    0, memberId, activityId, planName,
                    sessionBased ? Subscription.PlanType.SESSION_BASED : Subscription.PlanType.TIME_BASED,
                    sessionBased ? null : duration,
                    sessionBased ? duration : null,
                    sessionBased ? duration : null,
                    start, end, price, discount, Subscription.Status.ACTIVE
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
