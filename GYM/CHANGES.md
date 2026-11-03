# Changes made in this pass

## Login screen
- `login.fxml`: card is centred in the window, fixed 440 px wide, and only as tall as its form
  (`maxHeight="-Infinity"` stops the StackPane from stretching it to the full window height).
- Added a `.login-card` rule in `style.css` instead of the old inline `style="-fx-padding: 30;"`.
- Controller field IDs and handlers are unchanged.

## Compile fixes
- `ActivityForm`, `CoachForm`, `ExpenseForm`, `PlanForm`, `ScheduledClassForm`, `UserForm` used
  `Insets` and `WizardDialog` without importing them, so the project could not be compiled.

## Clean-code / OOP
- The identical `grid()` / `addRow()` helpers copied into those six forms now live in one class, `FormGrid`.
- Removed an unused private `finish(...)` method from each form.
- Removed `FormDialog` (no longer referenced anywhere; every form uses `WizardDialog`).
- Removed commented-out imports; removed stale `target/` and `.idea/` from the package.
