# Refactored Gym Forms

- MemberWizard.java is included unchanged and remains the reference form.
- ActivityForm, CoachForm, ExpenseForm, PlanForm, ScheduledClassForm, and UserForm now use WizardDialog + GridPane, matching the MemberWizard layout approach.
- Existing public method signatures and data records are retained.
- Validation now happens inside the dialog, so invalid input is explained before the dialog closes.

## Integration
Replace the corresponding files under:
`src/main/java/com/gym/system/controller/form/`

These files rely on the existing project classes:
- `com.gym.system.controller.WizardDialog`
- `com.gym.system.dao.GymRepository`
- Existing model classes and translation keys

The original project source/build files were not included in the uploaded archive, so a full project compilation could not be run here. Please compile and run the project after replacing the files.
