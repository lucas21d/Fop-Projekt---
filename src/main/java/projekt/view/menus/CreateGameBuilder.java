package projekt.view.menus;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.tudalgo.algoutils.student.annotation.DoNotTouch;
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired;
import projekt.model.PlayerImpl;
import projekt.model.PlayerImpl.Builder;

import java.util.List;
import java.util.function.Supplier;

/**
 * A Builder to create the create game view.
 * The create game view lets users add and remove players and start the game.
 * It is possible to give each player a name, a color and to select whether the
 * player is a bot or not.
 */
public class CreateGameBuilder extends MenuBuilder {
    private final ObservableList<PlayerImpl.Builder> observablePlayers;
    private final Supplier<Boolean> startGameHandler;

    /**
     * Creates a new CreateGameBuilder with the given players and handlers.
     *
     * @param players          The list of players to display and modify.
     * @param returnHandler    The handler to call when the user wants to return to
     *                         the main menu
     * @param startGameHandler The handler to call when the user wants to start the
     *                         game
     */
    @DoNotTouch
    public CreateGameBuilder(
        final ObservableList<PlayerImpl.Builder> players,
        final Runnable returnHandler,
        final Supplier<Boolean> startGameHandler
    ) {
        super("Start new Game", returnHandler);
        this.startGameHandler = startGameHandler;
        this.observablePlayers = players;
    }

    @Override
    protected Node initCenter() {
        final VBox mainBox = new VBox();
        mainBox.setStyle("-fx-background-color: #2D2D30; background-color: #2D2D30; -fx-fill-height: true");
//        BackgroundFill backgroundFill = new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
//        Background background = new Background(backgroundFill);
//        mainBox.setBackground(background);

        final VBox playerListVBox = new VBox();
        this.observablePlayers.subscribe(() -> {
            playerListVBox.getChildren().clear();
            for (final PlayerImpl.Builder playerBuilder : this.observablePlayers) {
                final HBox playerListingHBox = new HBox();
                playerListingHBox.setAlignment(Pos.CENTER);
                final TextField playerNameTextField = new TextField(playerBuilder.nameOrDefault());
                playerNameTextField.setOnKeyPressed(e -> {
                    final String newName = playerNameTextField.getText();
                    if (newName.isBlank()) {
                        playerBuilder.name(null);
                        playerNameTextField.setText(playerBuilder.nameOrDefault());
                        playerNameTextField.selectAll();
                    } else {
                        playerBuilder.name(newName);
                    }
                });
                playerListingHBox.getChildren().addAll(
                    playerNameTextField
                );
                playerListVBox.getChildren().addAll(
                    playerListingHBox,
                    createRemovePlayerButton(playerBuilder.getId()),
                    createPlayerColorPicker(playerBuilder),
                    createBotOrPlayerSelector(playerBuilder)
                );
            }
        });

        final Button startGameButton = new Button("Start Game");
        final Label startGameErrorLabel = new Label();
        startGameButton.setOnAction(e -> {
            if (!this.startGameHandler.get()) {
                startGameErrorLabel.setText("Cannot start game");
            }
        });

        Region spacer = new Region();
        spacer.setPrefHeight(375);

        mainBox.getChildren().addAll(
            playerListVBox,
            startGameButton,
            startGameErrorLabel,
            createAddPlayerButton(),
            spacer
        );

        mainBox.alignmentProperty().set(Pos.TOP_CENTER);
        final ScrollPane scrollPane = new ScrollPane();
//        scrollPane.setFitToHeight(true);
        scrollPane.setContent(mainBox);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    /**
     * Creates a button to add a new player to the game.
     * The button adds a new player to the list of players when clicked.
     *
     * @return a button to add a new player to the game
     */
    @StudentImplementationRequired("H3.4")
    private Node createAddPlayerButton() {
        Button addPlayerButton = new Button("Add player");
        addPlayerButton.setOnAction(event -> observablePlayers.add(nextPlayerBuilder()));
        return addPlayerButton;
    }

    /**
     * Creates a color picker to select the color of the player.
     * Two players cannot have the same color.
     *
     * @param playerBuilder the builder for the player to create the color picker
     *                      for
     * @return a color picker to select the color of the player
     */
    @StudentImplementationRequired("H3.4")
    private Node createPlayerColorPicker(final Builder playerBuilder) {
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setValue(playerBuilder.getColor());
        final ObjectProperty<Color> oldColorProperty = new SimpleObjectProperty<>(playerBuilder.getColor());
        colorPicker.setOnAction(event -> {
            Color colorPicked = colorPicker.getValue();
            List<Color> alreadyUsedColors = observablePlayers.stream().map(Builder::getColor).toList();
            if (alreadyUsedColors.contains(colorPicked)) {
                alertColorAlreadyPicked().showAndWait();
                colorPicker.setValue(oldColorProperty.get());
            } else {
                oldColorProperty.set(colorPicked);
                playerBuilder.color(colorPicked);
            }
        });
        return colorPicker;
    }

    /**
     * Creates an alert for when the color selected is already in use.
     *
     * @return alert created.
     */
    @NotNull
    private static Alert alertColorAlreadyPicked() {
        Alert alertColorAlreadyPicked = new Alert(Alert.AlertType.ERROR);
        alertColorAlreadyPicked.setTitle("Error: Color already picked");
        alertColorAlreadyPicked.setHeaderText("Color already picked");
        alertColorAlreadyPicked.setContentText("The color you have selected has already been picked. Please choose another color");
        return alertColorAlreadyPicked;
    }


    /**
     * Creates a node to select whether the player is a bot or not.
     *
     * @param playerBuilder the builder for the player to create the selector for
     * @return a node to select whether the player is a bot or not
     */
    @StudentImplementationRequired("H3.4")
    private Node createBotOrPlayerSelector(final Builder playerBuilder) {
        CheckBox checkboxBotSelected = new CheckBox("is bot?");
        checkboxBotSelected.setSelected(playerBuilder.isAi());
        checkboxBotSelected.setOnAction(check -> playerBuilder.ai(checkboxBotSelected.isSelected()));
        return checkboxBotSelected;
    }

    /**
     * Creates a button to remove the player with the given id.
     *
     * @param id the id of the player to remove
     * @return a button to remove the player with the given id
     */
    @StudentImplementationRequired("H3.4")
    private Button createRemovePlayerButton(final int id) {
        Button removePlayerButton = new Button("Remove player");
        removePlayerButton.setOnAction(event -> removePlayer(id));
        return removePlayerButton;
    }

    /**
     * Removes the player with the given id and updates the ids of the remaining
     * players.
     *
     * @param id the id of the player to remove
     */
    @StudentImplementationRequired("H3.4")
    private void removePlayer(final int id) {
        observablePlayers.removeIf(player -> player.getId() == id);
        int playerId = 0;
        for (PlayerImpl.Builder player : observablePlayers) {
            player.id(playerId++);
        }
    }

    /**
     * Returns a new {@link PlayerImpl.Builder} for the player with the next id.
     *
     * @return a new {@link PlayerImpl.Builder} for the player with the next id
     */
    public PlayerImpl.Builder nextPlayerBuilder() {
        return new PlayerImpl.Builder(this.observablePlayers.size() + 1);
    }

}
