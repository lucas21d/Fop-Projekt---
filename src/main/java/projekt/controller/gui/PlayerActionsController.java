package projekt.controller.gui;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Builder;
import javafx.util.Subscription;
import org.tudalgo.algoutils.student.annotation.DoNotTouch;
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired;
import projekt.controller.PlayerController;
import projekt.controller.PlayerObjective;
import projekt.controller.actions.*;
import projekt.model.*;
import projekt.model.buildings.Edge;
import projekt.model.tiles.Tile;
import projekt.view.gameControls.AcceptTradeDialog;
import projekt.view.gameControls.PlayerActionsBuilder;
import projekt.view.gameControls.SelectCardToStealDialog;
import projekt.view.gameControls.SelectResourcesDialog;
import projekt.view.gameControls.TradeDialog;
import projekt.view.gameControls.UseDevelopmentCardDialog;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static projekt.model.buildings.Settlement.Type.*;

/**
 * This class is responsible for handling all player actions performed through
 * the UI. It ensures that the correct buttons are enabled and disabled based on
 * the current player objective and state.
 * It also ensures that the correct actions are triggered when a button is
 * clicked and that the user is prompted when a action requires user input.
 * Additionally it triggers the respective actions based on the user input.
 *
 * <b>Do not touch any of the given attributes these are constructed in a way to
 * ensure thread safety.</b>
 */
public class PlayerActionsController implements Controller {
    private final PlayerActionsBuilder builder;
    private final GameBoardController gameBoardController;
    private final Property<PlayerController> playerControllerProperty = new SimpleObjectProperty<>();
    private final Property<PlayerObjective> playerObjectiveProperty = new SimpleObjectProperty<>(PlayerObjective.IDLE);
    private final Property<PlayerState> playerStateProperty = new SimpleObjectProperty<>();
    private Subscription playerObjectiveSubscription = Subscription.EMPTY;
    private Subscription playerStateSubscription = Subscription.EMPTY;

    /**
     * Creates a new PlayerActionsController.
     * It attaches listeners to populate the playerController, playerState and
     * playerObjective properties. This is necessary to ensure these properties are
     * always on the correct thread.
     * Additionally the PlayerActionsBuilder is created with all necessary event
     * handlers.
     *
     * <b>Do not touch this constructor.</b>
     *
     * @param gameBoardController      the game board controller
     * @param playerControllerProperty the property that contains the player
     *                                 controller that is currently active
     */
    @DoNotTouch
    public PlayerActionsController(
        final GameBoardController gameBoardController,
        final Property<PlayerController> playerControllerProperty
    ) {
        this.playerControllerProperty.subscribe((oldValue, newValue) -> {
            Platform.runLater(() -> {
                playerObjectiveSubscription.unsubscribe();
                playerObjectiveSubscription = newValue.getPlayerObjectiveProperty().subscribe((
                                                                                                  oldObjective,
                                                                                                  newObjective
                                                                                              ) -> Platform.runLater(() -> this.playerObjectiveProperty.setValue(newObjective)));

                playerStateSubscription.unsubscribe();
                playerStateSubscription = newValue.getPlayerStateProperty().subscribe(
                    (oldState, newState) -> Platform.runLater(() -> this.playerStateProperty.setValue(newState)));
                this.playerStateProperty.setValue(newValue.getPlayerStateProperty().getValue());
                this.playerObjectiveProperty.setValue(newValue.getPlayerObjectiveProperty().getValue());
            });
        });
        this.gameBoardController = gameBoardController;
        playerControllerProperty.subscribe((oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue == null) {
                    return;
                }
                this.playerControllerProperty.setValue(newValue);
            });
        });
        Platform.runLater(() -> {
            this.playerControllerProperty.setValue(playerControllerProperty.getValue());
        });

        this.builder = new PlayerActionsBuilder(
            actionWrapper(this::buildVillageButtonAction, true),
            actionWrapper(this::upgradeVillageButtonAction, true),
            actionWrapper(this::buildRoadButtonAction, true),
            actionWrapper(this::buyDevelopmentCardButtonAction, false),
            actionWrapper(this::useDevelopmentCardButtonAction, false),
            actionWrapper(this::endTurnButtonAction, false),
            actionWrapper(this::rollDiceButtonAction, false),
            actionWrapper(this::tradeButtonAction, false),
            this::abortButtonAction
        );
    }

    /**
     * Updates the UI based on the given objective. This includes enabling and
     * disabling buttons and prompting the user if necessary.
     * Also redraws the game board and updates the player information.
     *
     * @param objective the objective to check
     */
    @StudentImplementationRequired("H3.2")
    private void updateUIBasedOnObjective(final PlayerObjective objective) {
        removeAllHighlights();
        drawIntersections();
        drawEdges();
        getHexGridController().drawTiles();
        builder.disableAllButtons();
        updatePlayerInformation();

        if (getPlayerController().getPlayer().isAi()) return;

        switch(objective) {
            case PLACE_ROAD:
                updateBuildRoadButtonState();
                break;
            case PLACE_VILLAGE:
                updateBuildVillageButtonState();
                break;
            case DROP_CARDS:
                selectResources(getPlayerState().cardsToSelect()); // to be Reviewed!
                break;
            case SELECT_CARDS:
                selectResources(getPlayerState().cardsToSelect()); // to be Reviewed!
                break;
            case SELECT_CARD_TO_STEAL:
                selectCardToStealAction();
                break;
            case SELECT_ROBBER_TILE:
                getHexGridController().highlightTiles(this::selectRobberTileAction);
                break;
            case REGULAR_TURN:
                updateBuildRoadButtonState();
                updateBuildVillageButtonState();
                updateUpgradeVillageButtonState();
                updateBuyDevelopmentCardButtonState();
                updateUseDevelopmentCardButtonState();
                builder.enableEndTurnButton();
                builder.enableTradeButton();
                break;
            case ACCEPT_TRADE:
                acceptTradeOffer();
                break;
            case DICE_ROLL:
                updateUpgradeVillageButtonState();
                builder.enableRollDiceButton();
                break;
                //TODO 3.2 default case erweitern oder etwas anderes fixen, weil wenn der code von REGULAR_TURN nicht dort ist wird das spiel blockiert.
                // TODO 3.2 vielleicht liegt es daran ,dass es keinen case für IDLE gibt?
            default:
                updateBuildRoadButtonState();
                updateBuildVillageButtonState();
                updateUpgradeVillageButtonState();
                updateBuyDevelopmentCardButtonState();
                updateUseDevelopmentCardButtonState();
                builder.enableEndTurnButton();
                builder.enableRollDiceButton();
                builder.enableTradeButton();
                break;

        }
    }

    /**
     * Returns the player controller that is currently active.
     * Please do not use this method to get the playerState or playerObjective.
     * Use the {@link #getPlayerState()} and {@link #getPlayerObjective()} instead.
     *
     * @return the player controller that is currently active
     */
    @DoNotTouch
    private PlayerController getPlayerController() {
        return playerControllerProperty.getValue();
    }

    /**
     * Returns the player state of the player that is currently active.
     *
     * @return the player state of the player that is currently active
     */
    @DoNotTouch
    private PlayerState getPlayerState() {
        return playerStateProperty.getValue();
    }

    /**
     * Returns the player objective of the player that is currently active.
     *
     * @return the player objective of the player that is currently active
     */
    @DoNotTouch
    private PlayerObjective getPlayerObjective() {
        return playerObjectiveProperty.getValue();
    }

    /**
     * Returns the HexGridController of the game board.
     *
     * @return the HexGridController of the game board
     */
    @DoNotTouch
    private HexGridController getHexGridController() {
        return gameBoardController.getHexGridController();
    }

    /**
     * Returns the player that is currently active.
     *
     * @return the player that is currently active
     */
    @DoNotTouch
    private Player getPlayer() {
        return getPlayerController().getPlayer();
    }

    /**
     * ReDraws the intersections.
     */
    @DoNotTouch
    private void drawIntersections() {
        getHexGridController().drawIntersections();
    }

    /**
     * ReDraws the edges.
     */
    @DoNotTouch
    private void drawEdges() {
        getHexGridController().drawEdges();
    }

    /**
     * Removes all highlights from the game board.
     */
    @DoNotTouch
    private void removeAllHighlights() {
        getHexGridController().getEdgeControllers().forEach(EdgeController::unhighlight);
        getHexGridController().getIntersectionControllers().forEach(IntersectionController::unhighlight);
        getHexGridController().unhighlightTiles();
    }

    /**
     * Updates the player information in the game board.
     */
    @DoNotTouch
    private void updatePlayerInformation() {
        gameBoardController.updatePlayerInformation(getPlayer(), getPlayerState().changedResources());
    }

    /**
     * Wraps a event handler (primarily button Actions) to ensure that all
     * highlights are removed, intersections are redrawn and all buttons except the
     * abort button (if abortable) are disabled.
     * This method is intended to be used when a button is clicked, to ensure a
     * common state before a action is performed.
     *
     * @param handler   the handler to wrap
     * @param abortable whether the action is abortable
     * @return the wrapped handler
     */
    @DoNotTouch
    private Consumer<ActionEvent> actionWrapper(final Consumer<ActionEvent> handler, final boolean abortable) {
        return e -> {
            removeAllHighlights();
            drawIntersections();
            if (abortable) {
                builder.disableAllButtons();
                builder.enableAbortButton();
            }

            handler.accept(e);
            // gameBoardController.updatePlayerInformation(getPlayer());
        };
    }

    // Build actions

    /**
     * Wraps a event handler to ensure that all highlights are removed, the correct
     * buttons are reenabled and the player information is up to date.
     * <p>
     * This method is intended to be used when a action is triggered on the
     * player controller to ensure a common state after the action is performed.
     *
     * @param handler the handler to wrap
     * @return the wrapped handler
     */
    @DoNotTouch
    private Consumer<MouseEvent> buildActionWrapper(final Consumer<MouseEvent> handler) {
        return e -> {
            handler.accept(e);

            removeAllHighlights();
            if (getPlayerController() != null) {
                updateUIBasedOnObjective(getPlayerObjective());
            }
        };
    }

    /**
     * Enables or disable the build village button based on the currently allowed
     * actions and if there are any buildable intersections.
     */
    @StudentImplementationRequired("H3.1")
    private void updateBuildVillageButtonState() {
        // TODO: H3.1 check done
        if(getPlayerObjective().getAllowedActions().contains(BuildVillageAction.class)  //checks if building a Village is allowed in current state.
            && getPlayerController().canBuildVillage()                                  //checks if player has enough resources.
            && (  getPlayerController().getPlayer().getSettlements().size() < 2         //first round placing the first 2 villages anywhere.
            || !getPlayerState().buildableVillageIntersections().isEmpty()  ) ){ //checks if there a any empty Intersections adjacent to owned roads.

            builder.enableBuildVillageButton();
        }else{
            builder.disableBuildVillageButton();
        }
    }

    /**
     * Attaches the logic to build a village to all buildable intersections and
     * highlights them.
     * When an intersection is selected, it triggers the BuildVillageAction.
     * The logic is wrapped in a buildActionWrapper to ensure a common state after a
     * village is built.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @StudentImplementationRequired("H3.1")
    private void buildVillageButtonAction(final ActionEvent event) {
        // TODO: H3.1 check done
        Set<Intersection> buildableVillages = getPlayerState().buildableVillageIntersections(); // gets all buildable spaces

        getHexGridController()
            .getIntersectionControllers()
            .stream()
            .filter(x -> buildableVillages
                .stream()
                .anyMatch(y -> y.equals(x.getIntersection())))
            .forEach(x -> x.highlight(
                buildActionWrapper(MouseEvent -> getPlayerController()
                    .triggerAction(new BuildVillageAction(x.getIntersection())))
            ));
    }

    /**
     * Enables or disable the upgrade village button based on the currently allowed
     * actions and if there are any upgradeable villages.
     */
    @StudentImplementationRequired("H3.1")
    private void updateUpgradeVillageButtonState() {
        // TODO: H3.1 check done
        if(getPlayerObjective().getAllowedActions().contains(UpgradeVillageAction.class)    //checks if upgrading a village is allowed in current state.
            && getPlayerController().canUpgradeVillage()                                    //checks if player has enough resources.
            && !getPlayerController().getPlayer().getSettlements().isEmpty()                //checks if the player has any settlement
            && getPlayerController().getPlayer().getSettlements().stream().anyMatch(x -> x.type().equals(VILLAGE))){ //checks if there are any villages on Intersections.

            builder.enableUpgradeVillageButton();
            return;
        }

        builder.disableUpgradeVillageButton();
    }

    /**
     * Attaches the logic to upgrade a village to all upgradeable intersections and
     * highlights them.
     * When an intersection is selected, it triggers the UpgradeVillageAction.
     * The logic is wrapped in a buildActionWrapper to ensure a common state after a
     * village is upgraded.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @StudentImplementationRequired("H3.1")
    private void upgradeVillageButtonAction(final ActionEvent event) {
        // TODO: H3.1 check done
        Set<Intersection> upgradableVillages = getPlayerState().upgradableVillageIntersections(); // gets all upgradable Villages
        getHexGridController().getIntersectionControllers().stream().filter(x->upgradableVillages.contains(x.getIntersection())).forEach(x->{x.highlight(buildActionWrapper(MouseEvent->getPlayerController().triggerAction(new UpgradeVillageAction(x.getIntersection()))));});
    }

    /**
     * Enables or disable the build road button based on the currently allowed
     * actions and if there are any edges to build on.
     */
    @StudentImplementationRequired("H3.1")
    private void updateBuildRoadButtonState() {
        // TODO: H3.1 check done
        if(getPlayerObjective().getAllowedActions().contains(BuildRoadAction.class)  //checks if building a Road is allowed in current state.
            && getPlayerController().canBuildRoad()                                  //checks if player has enough resources.
            &&( getPlayerController().getPlayer().getSettlements().stream().anyMatch(x->x.intersection().getConnectedEdges().stream().anyMatch(y -> !y.hasRoad()))   // checks if any emty Edges beside a settlement
            || getPlayerController().getPlayer().getRoads().values().stream().anyMatch(z->z.getIntersections().stream().anyMatch(x -> (x.hasSettlement()) ? x.getSettlement().owner().equals(getPlayer()) : (x.getConnectedEdges()
                .stream()
                .anyMatch(y -> y.getRoadOwner()
                        .equals(getPlayer()))))))){ //checks if there's any empty Edges adjacent to owned roads and not blocked by an enemy settlement.

            builder.enableBuildRoadButton();
            return;
        }

        builder.disableBuildRoadButton();
    }

    /**
     * Attaches the logic to build a road to all buildable edges and highlights
     * them.
     * When an edge is selected, it triggers the BuildRoadAction.
     * The logic is wrapped in a buildActionWrapper to ensure a common state after a
     * road is built.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @StudentImplementationRequired("H3.1")
    private void buildRoadButtonAction(final ActionEvent event) {
        // TODO: H3.1 check done
        Set<Edge> buildableRoads = getPlayerState().buildableRoadEdges(); // gets all buildable spaces
        getHexGridController()
            .getEdgeControllers()
            .stream()
            .filter(x -> buildableRoads.contains(x.getEdge()))
            .forEach(x -> x.highlight(
                buildActionWrapper(MouseEvent -> getPlayerController()
                    .triggerAction(
                    new BuildRoadAction(x.getEdge())))
            ));
    }

    /**
     * The action that is triggered when the end turn button is clicked.
     *
     * @param event the event that triggered the action
     */
    @DoNotTouch
    private void endTurnButtonAction(final ActionEvent event) {
        getPlayerController().triggerAction(new EndTurnAction());
    }

    /**
     * The action that is triggered when the roll dice button is clicked.
     *
     * @param event the event that triggered the action
     */
    @DoNotTouch
    private void rollDiceButtonAction(final ActionEvent event) {
        getPlayerController().triggerAction(new RollDiceAction());
    }

    // Robber actions

    /**
     * Triggers the SelectRobberTileAction with the selected tile and unhighlights
     * all tiles. After the action is triggered, the tiles are redrawn.
     *
     * @param tile the tile that was clicked
     */
    @DoNotTouch
    private void selectRobberTileAction(final Tile tile) {
        getHexGridController().unhighlightTiles();
        getPlayerController().triggerAction(new SelectRobberTileAction(tile.getPosition()));
        getHexGridController().drawTiles();
    }

    /**
     * Performs the action of selecting a card to steal from another player.
     * If there are no players to steal from, triggers the EndTurnAction.
     * Prompts the user to select a card to steal from a player and triggers the
     * StealCardAction with the selected card.
     * If no card is selected, triggers the EndTurnAction.
     */
    @DoNotTouch
    private void selectCardToStealAction() {
        if (getPlayerState().playersToStealFrom().isEmpty()) {
            getPlayerController().triggerAction(new EndTurnAction());
            return;
        }
        final SelectCardToStealDialog dialog = new SelectCardToStealDialog(getPlayerState().playersToStealFrom());
        dialog.showAndWait().ifPresentOrElse(
            result -> getPlayerController().triggerAction(new StealCardAction(result.getValue(), result.getKey())),
            () -> getPlayerController().triggerAction(new EndTurnAction())
        );
    }

    /**
     * Prompts the user to select resource cards.
     * If the current player objective is DROP_HALF_CARDS, the user can only select
     * cards from the players resources.
     * If the user cancels or an invalid amount of cards is selected, the user is
     * prompted again.
     * <p>
     * Triggers the SelectCardsAction with the selected cards.
     *
     * @param amountToSelect the amount of cards to select
     */
    @DoNotTouch
    private void selectResources(final int amountToSelect) {
        final SelectResourcesDialog dialog = new SelectResourcesDialog(amountToSelect, getPlayer(),
                                                                       PlayerObjective.DROP_CARDS.equals(getPlayerObjective()) ? getPlayer().getResources() : null,
                                                                       PlayerObjective.DROP_CARDS.equals(getPlayerObjective())
        );
        Optional<Map<ResourceType, Integer>> result = dialog.showAndWait();
        while (result.isEmpty() || result.get() == null) {
            result = dialog.showAndWait();
        }
        getPlayerController().triggerAction(new SelectCardsAction(result.get()));
    }

    // Development card actions

    /**
     * Enables or disable the buy development card button based on the currently
     * allowed actions and whether the player can buy a development card.
     */
    @DoNotTouch
    private void updateBuyDevelopmentCardButtonState() {
        if (getPlayerObjective().getAllowedActions().contains(BuyDevelopmentCardAction.class)
            && getPlayerController().canBuyDevelopmentCard()) {
            builder.enableBuyDevelopmentCardButton();
            return;
        }
        builder.disableBuyDevelopmentCardButton();
    }

    /**
     * Performs the action of buying a development card.
     * Triggers the BuyDevelopmentCardAction.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @DoNotTouch
    private void buyDevelopmentCardButtonAction(final ActionEvent event) {
        getPlayerController().triggerAction(new BuyDevelopmentCardAction());
        updateUIBasedOnObjective(getPlayerObjective());
    }

    /**
     * Enables or disable the use development card button based on the currently
     * allowed actions and whether the player has any development cards to play.
     */
    @DoNotTouch
    private void updateUseDevelopmentCardButtonState() {
        if (getPlayerObjective().getAllowedActions().contains(PlayDevelopmentCardAction.class)
            && getPlayer().getDevelopmentCards().entrySet().stream().anyMatch(
            entry -> entry.getKey() != DevelopmentCardType.VICTORY_POINTS && entry.getValue() > 0)) {
            builder.enablePlayDevelopmentCardButton();
            return;
        }
        builder.disablePlayDevelopmentCardButton();
    }

    /**
     * Performs the action of playing a development card.
     * Prompts the user to select a development card to play.
     * If the user cancels, the action is cancelled.
     * Triggers the PlayDevelopmentCardAction with the selected card.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @DoNotTouch
    public void useDevelopmentCardButtonAction(final ActionEvent event) {
        final UseDevelopmentCardDialog dialog = new UseDevelopmentCardDialog(getPlayer());
        dialog.showAndWait()
            .ifPresent(result -> getPlayerController().triggerAction(new PlayDevelopmentCardAction(result)));
        updateUIBasedOnObjective(getPlayerObjective());
    }

    // Trade actions

    /**
     * Performs the trading action.
     * Prompts the user to select the cards to offer and the cards to request.
     * If the user cancels, the trade is cancelled.
     * Triggers the TradeAction with the selected cards.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @DoNotTouch
    private void tradeButtonAction(final ActionEvent event) {
        System.out.println("Trading");
        final TradeDialog dialog = new TradeDialog(new TradePayload(null, null, false, getPlayer()));
        dialog.showAndWait().ifPresentOrElse(payload -> {
            getPlayerController().triggerAction(new TradeAction(payload));
        }, () -> System.out.println("Trade cancelled"));
        updateUIBasedOnObjective(getPlayerObjective());
    }

    /**
     * Performs the action of accepting a trade offer.
     * Prompts the user to accept or decline the trade offer.
     * If the user cancels, the trade is declined.
     * Triggers the AcceptTradeAction with a boolean representing the players
     * decision.
     */
    @DoNotTouch
    private void acceptTradeOffer() {
        final Optional<Boolean> optionalResult = new AcceptTradeDialog(getPlayerState().offeredTrade(), getPlayer())
            .showAndWait();
        optionalResult.ifPresent(result -> getPlayerController().triggerAction(new AcceptTradeAction(result)));
    }

    /**
     * Aborts the current action by remove all highlights and reenabling the correct
     * buttons. Disables the abort button.
     * <p>
     * This method is prepared to be used with a button.
     *
     * @param event the event that triggered the action
     */
    @DoNotTouch
    private void abortButtonAction(final ActionEvent event) {
        removeAllHighlights();
        updateUIBasedOnObjective(getPlayerObjective());
        builder.disableAbortButton();
    }

    @Override
    @DoNotTouch
    public Builder<Region> getBuilder() {
        return builder;
    }

    @Override
    @DoNotTouch
    public Region buildView() {
        final Region view = builder.build();

        playerObjectiveProperty.subscribe((oldValue, newValue) -> updateUIBasedOnObjective(newValue));
        playerStateProperty.subscribe((oldValue, newValue) -> updateUIBasedOnObjective(getPlayerObjective()));
        builder.disableAllButtons();
        if (getPlayerController() != null) {
            updateUIBasedOnObjective(getPlayerObjective());
        }
        return view;
    }
}
