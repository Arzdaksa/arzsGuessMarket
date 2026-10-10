package ui.fx;

import engine.Event;
import engine.GuessMarketEngine;
import engine.MethodType;
import engine.User;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class CreateEventWizardView {

    private CreateEventWizardView() {}

    private static final class Fields {
        final TextField name = promptField("Event name", 260);
        final TextField description = promptField("Description", 260);
        final TextField commission = promptField("Commission % (0-90)", 160);
        final ToggleGroup commissionGroup = new ToggleGroup();
        final ToggleButton onPurchase = toggle("On purchase", commissionGroup);
        final ToggleButton onClose = toggle("On close", commissionGroup);
        final ToggleGroup typeGroup = new ToggleGroup();
        final ToggleButton lmsr = toggle("LMSR", typeGroup);
        final ToggleButton orderBook = toggle("Order Book", typeGroup);
        final TextField b = promptField("Liquidity 'b' (> 0)", 200);
        final TextField d = promptField("Base value 'd' (> 0)", 200);
        final TextField initial = promptField("Initial investment (>= 0)", 200);
        final CheckBox allowMint = new CheckBox("Allow mint");
        final List<TextField> options = new ArrayList<>();
        final VBox optionsBox = new VBox(8);
        final VBox typeSpecificBox = new VBox(8);
        final Label error = new Label();
        final Button submit = new Button("Create Event");

        Fields() {
            error.getStyleClass().add("error-label");
            submit.getStyleClass().add("primary-button");
            submit.setDisable(true);
            onPurchase.setSelected(true);
            lmsr.setSelected(true);
            preventDeselection(commissionGroup);
            preventDeselection(typeGroup);
        }
    }

    static VBox build(ScreenController controller, GuessMarketEngine engine, User marketMaker) {
        Fields f = new Fields();
        Runnable validate = () -> validate(f);

        for (TextField field : List.of(f.name, f.description, f.commission, f.b, f.d, f.initial)) {
            field.textProperty().addListener((o, old, val) -> validate.run());
        }
        f.commissionGroup.selectedToggleProperty().addListener((o, old, val) -> validate.run());

        addOptionRow(f, validate);
        addOptionRow(f, validate);

        Button addOption = new Button("+ Add option");
        addOption.getStyleClass().add("link-button");
        addOption.setOnAction(e -> {
            addOptionRow(f, validate);
            validate.run();
        });

        Runnable renderType = () -> {
            f.typeSpecificBox.getChildren().setAll(f.lmsr.isSelected()
                    ? List.of(f.b)
                    : List.of(f.d, f.initial, f.allowMint));
            validate.run();
        };
        f.typeGroup.selectedToggleProperty().addListener((o, old, val) -> renderType.run());
        renderType.run();

        f.submit.setOnAction(e -> submit(controller, engine, marketMaker, f));

        Label title = new Label("Create Event (Bonus)");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("The new event will be assigned to " + marketMaker.getName() + " as market maker.");
        subtitle.getStyleClass().add("content-subtitle");

        VBox detailsCard = TradeStatusView.card("Event Details", new VBox(10,
                labeledRow("Name:", f.name), labeledRow("Description:", f.description),
                labeledRow("Commission %:", f.commission),
                labeledRow("Charged:", new HBox(8, f.onPurchase, f.onClose))));
        VBox optionsCard = TradeStatusView.card("Options", new VBox(10, f.optionsBox, addOption));
        VBox typeCard = TradeStatusView.card("Market Type",
                new VBox(10, new HBox(8, f.lmsr, f.orderBook), f.typeSpecificBox));
        VBox submitCard = TradeStatusView.card("Create", new VBox(10, f.error, f.submit));

        return new VBox(16, title, subtitle, detailsCard, optionsCard, typeCard, submitCard);
    }

    private static void addOptionRow(Fields f, Runnable validate) {
        TextField field = promptField("Option name", 200);
        field.textProperty().addListener((o, old, val) -> validate.run());

        Button remove = new Button("Remove");
        remove.getStyleClass().add("link-button");
        HBox row = new HBox(8, field, remove);
        remove.setOnAction(e -> {
            f.optionsBox.getChildren().remove(row);
            f.options.remove(field);
            validate.run();
        });

        f.options.add(field);
        f.optionsBox.getChildren().add(row);
    }

    private static void submit(ScreenController controller, GuessMarketEngine engine, User marketMaker, Fields f) {
        try {
            int commissionPercent = Integer.parseInt(f.commission.getText().trim());
            String commissionType = f.onPurchase.isSelected() ? "on-purchase" : "on-close";
            List<String> optionNames = f.options.stream().map(field -> field.getText().trim()).toList();
            MethodType type = f.lmsr.isSelected() ? MethodType.LMSR : MethodType.ORDER_BOOK;
            Integer b = f.lmsr.isSelected() ? Integer.parseInt(f.b.getText().trim()) : null;
            Integer d = f.orderBook.isSelected() ? Integer.parseInt(f.d.getText().trim()) : null;
            Integer initial = f.orderBook.isSelected() ? Integer.parseInt(f.initial.getText().trim()) : null;

            Event event = engine.createEvent(marketMaker, f.name.getText().trim(), f.description.getText().trim(),
                    commissionPercent, commissionType, optionNames, type, b, d, initial, f.allowMint.isSelected());

            controller.showInfo("Event created", "'" + event.getName() + "' was created with "
                    + marketMaker.getName() + " as market maker.");
            controller.showEventDashboard(event);
        } catch (RuntimeException ex) {
            f.error.setText(ex.getMessage());
        }
    }

    private static void validate(Fields f) {
        boolean blank = f.name.getText() == null || f.name.getText().isBlank();
        String message = errorMessage(f);
        f.error.setText(blank || message == null ? "" : message);
        f.submit.setDisable(blank || message != null);
    }

    private static String errorMessage(Fields f) {
        if (f.name.getText() == null || f.name.getText().isBlank()) {
            return "Event name is required.";
        }
        Integer commission = parseInt(f.commission.getText());
        if (commission == null || commission < 0 || commission > 90) {
            return "Commission must be a whole number between 0 and 90.";
        }

        List<String> distinctOptions = f.options.stream()
                .map(field -> field.getText() == null ? "" : field.getText().trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (distinctOptions.size() < 2) {
            return "Enter at least two distinct option names.";
        }

        if (f.lmsr.isSelected()) {
            Integer b = parseInt(f.b.getText());
            return (b == null || b <= 0) ? "LMSR 'b' must be a whole number greater than 0." : null;
        }

        if (distinctOptions.size() != 2) {
            return "Order Book events support exactly two options.";
        }
        Integer d = parseInt(f.d.getText());
        if (d == null || d <= 0) {
            return "'d' must be a whole number greater than 0.";
        }
        Integer initial = parseInt(f.initial.getText());
        if (initial == null || initial < 0) {
            return "Initial investment must be zero or a positive whole number.";
        }
        return null;
    }

    private static TextField promptField(String prompt, double maxWidth) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(maxWidth);
        return field;
    }

    private static ToggleButton toggle(String label, ToggleGroup group) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(group);
        return button;
    }

    // blocks clicking the active toggle back to "none selected", which would leave the form in an
    // ambiguous state where the visible fields no longer match what submit would actually use
    private static void preventDeselection(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == null) old.setSelected(true);
        });
    }

    private static HBox labeledRow(String label, Node control) {
        Label rowLabel = new Label(label);
        rowLabel.getStyleClass().add("field-label");
        return new HBox(8, rowLabel, control);
    }

    private static Integer parseInt(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
