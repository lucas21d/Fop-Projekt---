package projekt.view.gameControls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired;
import projekt.model.Player;
import projekt.model.ResourceType;
import projekt.view.IntegerField;
import projekt.view.ResourceCardPane;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A dialog to prompt the user to select a number of resources.
 * The dialog shows the resources the player can choose from and lets the user
 * select a number of each resource.
 * If dropCards is true, the user is prompted to drop cards instead of selecting
 * them.
 * The result of the dialog is a map of the selected resources and their
 * amounts.
 */
public class SelectResourcesDialog extends Dialog<Map<ResourceType, Integer>> {

    private final IntegerProperty selectedResources = new SimpleIntegerProperty();
    private final Map<ResourceType, IntegerField> integerFields = new HashMap<>();
    private final double SPACING = 30;
    private final double CARD_WIDTH = 75;
    private final int MAX_INPUT_VALUE = 100;

    /**
     * Creates a new SelectResourcesDialog for the given player and resources.
     *
     * @param amountToSelect        The amount of resources to select.
     * @param player                The player that is prompted to select resources.
     * @param resourcesToSelectFrom The resources the player can select from. If
     *                              null the player can select any resource.
     * @param dropCards             Whether the player should drop cards instead of
     *                              selecting them.
     */
    public SelectResourcesDialog(
        final int amountToSelect, final Player player,
        final Map<ResourceType, Integer> resourcesToSelectFrom, final boolean dropCards
    ) {
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);
        dialogPane.setContent(init(amountToSelect, player, resourcesToSelectFrom, dropCards));
    }

    @StudentImplementationRequired("H3.3")
    private Region init(
        final int amountToSelect,
        final Player player,
        Map<ResourceType, Integer> resourcesToSelectFrom,
        final boolean dropCards
    ) {
        VBox dialogContainer = new VBox();
        dialogContainer.setAlignment(Pos.CENTER);

        Label label = new Label("You (" + player.getName() + ") need to " + (dropCards ? "drop " : "select ") + amountToSelect + " more " + (amountToSelect == 1 ? "card" : "cards"));
        label.setFont(Font.font(30));

        label.textProperty().bind(Bindings.createStringBinding(
            () -> {
                int difference = amountToSelect - selectedResources.get();

                if (difference == 0) return "You (" + player.getName() + ") can continue";

                if (difference < 0) return "You (" + player.getName() + ") need to deselect " + -difference + (difference == -1 ? " card" : " cards");

                return "You (" + player.getName() + ") need to " + (dropCards ? "drop " : "select ") + difference + " more " + (difference == 1 ? "card" : "cards");
            },
            selectedResources
        ));


        HBox cards = new HBox(SPACING);
        cards.paddingProperty().set(new Insets(25, 50, 0, 50));

        if (resourcesToSelectFrom == null) {
            for (ResourceType resourceType : ResourceType.values()) {
                cards.getChildren()
                    .add(newResourceCard(
                        resourceType,
                        "",
                        MAX_INPUT_VALUE
                    ));
            }
        } else {
            for (ResourceType resourceType : ResourceType.values()) {
                if (resourcesToSelectFrom.get(resourceType) == null) continue;

                cards.getChildren()
                    .add(newResourceCard(
                        resourceType,
                        resourcesToSelectFrom.get(resourceType).toString(),
                        resourcesToSelectFrom.get(resourceType)
                    ));
            }
        }

        dialogContainer.getChildren().add(label);
        dialogContainer.getChildren().add(new Separator());
        dialogContainer.getChildren().add(cards);

        setResultConverter(button ->
            integerFields
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().valueProperty().get()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            ActionEvent.ACTION,
            event -> {
                if (selectedResources.get() != amountToSelect) event.consume();
            }
        );

        return dialogContainer;
    }

    /**
     * Initates a card of the specified resource type to be displayed alongside its input field.
     *
     * @param resourceType the resource type of the card
     * @param labelText the text on the cards label
     * @param maxValue the max value the input field can have
     * @return a VBox containing the card and the inputfield
     */
    private VBox newResourceCard(ResourceType resourceType, String labelText, int maxValue) {
        VBox resourceContainer = new VBox(SPACING);

        ResourceCardPane resourceCardPane = new ResourceCardPane(resourceType, labelText, CARD_WIDTH);

        IntegerField inputField = new IntegerField(0, maxValue);
        inputField.valueProperty().subscribe(() ->
            selectedResources.setValue(
                integerFields
                    .values()
                    .stream()
                    .mapToInt(field -> field.valueProperty().get())
                    .sum())
        );

        integerFields.put(resourceType, inputField);

        resourceContainer.getChildren().add(resourceCardPane);
        resourceContainer.getChildren().add(inputField);

        return resourceContainer;
    }
}
