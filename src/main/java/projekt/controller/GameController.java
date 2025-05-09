package projekt.controller;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.tudalgo.algoutils.student.annotation.DoNotTouch;
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired;
import projekt.Config;
import projekt.controller.actions.EndTurnAction;
import projekt.controller.actions.AcceptTradeAction;
import projekt.controller.actions.IllegalActionException;
import projekt.controller.actions.PlayerAction;
import projekt.model.DevelopmentCardType;
import projekt.model.GameState;
import projekt.model.HexGridImpl;
import projekt.model.Player;
import projekt.model.ResourceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static projekt.controller.PlayerObjective.*;

/**
 * The GameController class represents the controller for the game logic.
 * It manages the game state, player controllers, dice rolling and the overall
 * progression of the game.
 * It tells the players controllers what to do and when to do it.
 */
public class GameController {

    private final GameState state;
    private final Map<Player, PlayerController> playerControllers;
    private final Supplier<Integer> dice;
    private final IntegerProperty currentDiceRoll = new SimpleIntegerProperty(0);
    private final List<AiController> aiControllers = new ArrayList<>();
    private final Supplier<DevelopmentCardType> availableDevelopmentCards = Config.developmentCardGenerator();
    private final IntegerProperty roundCounter = new SimpleIntegerProperty(0);

    private final Property<PlayerController> activePlayerControllerProperty = new SimpleObjectProperty<>();

    /**
     * Initializes the {@link GameController} with the given {@link GameState},
     * {@link PlayerController}s and dice.
     *
     * @param state             The {@link GameState}.
     * @param playerControllers The {@link PlayerController}s.
     * @param dice              The dice.
     */
    public GameController(
        final GameState state,
        final Map<Player, PlayerController> playerControllers,
        final Supplier<Integer> dice
    ) {
        this.state = state;
        this.playerControllers = playerControllers;
        this.dice = dice;
    }

    /**
     * Initializes the {@link GameController} with the given {@link GameState} and
     * dice.
     * The {@link PlayerController}s are initialized with an empty {@link HashMap}.
     *
     * @param state The {@link GameState}.
     * @param dice  The dice.
     */
    public GameController(final GameState state, final Supplier<Integer> dice) {
        this.state = state;
        this.dice = dice;
        this.playerControllers = new HashMap<>();
    }

    /**
     * Initializes the {@link GameController} with the given {@link GameState}.
     * The dice is initialized with the Random from {@link Config#RANDOM} and
     * respects the configured dice sides and number of dice.
     *
     * @param state The {@link GameState}.
     * @see #GameController(GameState, Supplier)
     */
    public GameController(final GameState state) {
        this(state, () -> IntStream.rangeClosed(1, Config.NUMBER_OF_DICE)
            .map(i -> Config.RANDOM.nextInt(1, Config.DICE_SIDES + 1))
            .sum());
    }

    /**
     * Initializes the {@link GameController} with a new {@link GameState} that has
     * a new {@link HexGridImpl} that uses the radius from
     * {@link Config#GRID_RADIUS} and an empty list of {@link Player}s.
     *
     * @see #GameController(GameState)
     */
    public GameController() {
        this(new GameState(new HexGridImpl(Config.GRID_RADIUS), new ArrayList<>()));
    }

    /**
     * Initializes the {@link PlayerController}s for all players in the game.
     */
    public void initPlayerControllers() {
        for (final Player player : state.getPlayers()) {
            playerControllers.put(player, new PlayerController(this, player));
            if (player.isAi()) {
                aiControllers.add(new BasicAiController(playerControllers.get(player), state.getGrid(), state,
                                                        activePlayerControllerProperty
                ));
            }
        }
    }

    /**
     * Returns the {@link GameState}.
     *
     * @return The {@link GameState}.
     */
    public GameState getState() {
        return state;
    }

    /**
     * Returns the {@link PlayerController}s
     *
     * @return The {@link PlayerController}s
     */
    public Map<Player, PlayerController> getPlayerControllers() {
        return playerControllers;
    }

    /**
     * Returns the active {@link PlayerController} {@link Property}.
     *
     * @return The active {@link PlayerController} {@link Property}.
     */
    public Property<PlayerController> getActivePlayerControllerProperty() {
        return activePlayerControllerProperty;
    }

    /**
     * Returns the active {@link PlayerController}.
     *
     * @return The active {@link PlayerController}.
     */
    public PlayerController getActivePlayerController() {
        return activePlayerControllerProperty.getValue();
    }

    /**
     * Returns the {@link IntegerProperty} of the current dice roll.
     *
     * @return The {@link IntegerProperty} of the current dice roll.
     */
    public IntegerProperty getCurrentDiceRollProperty() {
        return currentDiceRoll;
    }

    /**
     * Returns the {@link IntegerProperty} of the round counter.
     *
     * @return The {@link IntegerProperty} of the round counter.
     */
    public IntegerProperty getRoundCounterProperty() {
        return roundCounter;
    }

    /**
     * Sets the active {@link PlayerController} {@link Property} to the
     * {@link PlayerController} of the given {@link Player}.
     */
    private void setActivePlayerControllerProperty(final Player activePlayer) {
        this.activePlayerControllerProperty.setValue(playerControllers.get(activePlayer));
    }

    /**
     * Casts the dice and returns the result.
     *
     * @return The result of the dice roll.
     */
    public int castDice() {
        currentDiceRoll.set(dice.get());
        return currentDiceRoll.get();
    }

    /**
     * Draws a development card from the stack of available development cards.
     *
     * @return The drawn development card.
     */
    public DevelopmentCardType drawDevelopmentCard() {
        return availableDevelopmentCards.get();
    }

    /**
     * Returns the {@link Player}s that have reached the victory condition.
     *
     * @return The {@link Player}s that have reached the victory condition.
     */
    public Set<Player> getWinners() {
        final Player playerWithMostKnightsPlayed = getState().getPlayers()
            .stream()
            .filter(player -> player.getKnightsPlayed() >= 3)
            .max(Comparator.comparingInt(Player::getKnightsPlayed))
            .orElse(null);
        final Player playerWithLongestRoad = null; // TODO: uncomment code if getLongestRoad(Player) is implemented in
        // HexGrid
        // getState().getPlayers()
        // .stream()
        // .max(Comparator.comparingInt(player ->
        // player.getHexGrid().getLongestRoad(player).size()))
        // .orElse(null);

        return getState().getPlayers()
            .stream()
            .filter(player -> (
                player.getVictoryPoints()
                    + (player == playerWithMostKnightsPlayed ? 2 : 0)
                    + (player == playerWithLongestRoad ? 2 : 0)
            ) >= Config.REQUIRED_VICTORY_POINTS)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Starts the game.
     *
     * @throws IllegalStateException If there are less {@link Player}s than
     *                               configured.
     */
    @DoNotTouch
    public void startGame() {
        if (this.state.getPlayers().size() < Config.MIN_PLAYERS) {
            throw new IllegalStateException("Not enough players");
        }
        if (playerControllers.isEmpty()) {
            initPlayerControllers();
        }

        firstRound();

        roundCounter.set(1);
        while (getWinners().isEmpty()) {
            for (final PlayerController playerController : playerControllers.values()) {
                withActivePlayer(playerController, () -> {
                    // Dice roll
                    playerController.waitForNextAction(PlayerObjective.DICE_ROLL);
                    final var diceRoll = currentDiceRoll.get();

                    if (diceRoll == 7) {
                        diceRollSeven();
                    } else {
                        distributeResources(diceRoll);
                    }
                    // Regular turn
                    regularTurn();
                });
            }
            roundCounter.set(roundCounter.get() + 1);
        }

        // Game End
        getState().setWinner(getWinners().iterator().next());
    }

    /**
     * Executes the given {@link Runnable} and set the active player to the given
     * {@link PlayerController}.
     * After the {@link Runnable} is executed, the active player is set to
     * {@code null} and the objective is set to {@link PlayerObjective#IDLE}.
     *
     * @param pc The {@link PlayerController} to set as active player.
     * @param r  The {@link Runnable} to execute.
     */
    @DoNotTouch
    public void withActivePlayer(final PlayerController pc, final Runnable r) {
        activePlayerControllerProperty.setValue(pc);
        r.run();
        pc.setPlayerObjective(IDLE);
        activePlayerControllerProperty.setValue(null);
    }

    /**
     * Starts the regular turn of the active player and waits for the player to end
     * his turn.
     */
    @StudentImplementationRequired("H2.1")
    private void regularTurn() {
        PlayerAction action;
        PlayerController activePlayerController = getActivePlayerController();
        if (activePlayerController != null) {
            do {
                action = activePlayerController.waitForNextAction(REGULAR_TURN);
            } while (!(action instanceof EndTurnAction));
        }
    }

    /**
     * Executes the first round of the game.
     * <p>
     * Each player places two villages and two roads.
     */
    @StudentImplementationRequired("H2.1")
    private void firstRound() {
        playerControllers
            .values()
            .forEach(value -> withActivePlayer(value, this::firstActions));
    }

    /**
     * Initiates the actions for the activePlayerController at the beginning of the game
     */
    private void firstActions() {
        if (getActivePlayerController() == null) return;

        for (int i = 0; i < 2; i++) {
            getActivePlayerController().waitForNextAction(PLACE_VILLAGE);
            getActivePlayerController().waitForNextAction(PLACE_ROAD);
        }
    }

    /**
     * Offer the trade to all players that can accept the trade. As soon as one
     * player accepts the trade, the offering player can continue with his round.
     *
     * @param offeringPlayer The player offering the trade.
     * @param offer          The resources the offering player offers.
     * @param request        The resources the offering player requests.
     */
    @StudentImplementationRequired("H2.3")
    public void offerTrade(
        final Player offeringPlayer, final Map<ResourceType, Integer> offer,
        final Map<ResourceType, Integer> request
    ) {
        // offer trade to all players
        for (PlayerController playerController: playerControllers.values()) {
            if (playerController.getPlayer().equals(offeringPlayer)) {
                continue;
            }

            playerController.setPlayerTradeOffer(offeringPlayer, offer, request);
            // read action from the player
            PlayerAction playerAction = playerController.waitForNextAction(ACCEPT_TRADE);
            if (playerAction instanceof AcceptTradeAction tradeAction) {
                try {
                    playerController.acceptTradeOffer(tradeAction.accepted());
                    playerController.resetPlayerTradeOffer();
                    break;
                    //  so that the other players don't receive the offer is it has been already been accepted?
                } catch (IllegalActionException ignored) {
                }
            }
        }
        for (PlayerController playerController: playerControllers.values()) {
            playerController.setPlayerObjective(IDLE);
        }
        setActivePlayerControllerProperty(offeringPlayer);
    }

    /**
     * Triggers the actions that happen when a 7 is rolled.
     * <p>
     * Every player with too many cards must drop half of his cards.
     * Then the active player must select a tile to place the robber on and can then
     * steal a card from a player next to the robber.
     */
    @StudentImplementationRequired("H2.1")
    private void diceRollSeven() {
        PlayerController roller = getActivePlayerController();

        playerControllers
            .values()
            .forEach(this::diceRollSevenPlayerAction);

        withActivePlayer(roller,this::moveRobber);

        setActivePlayerControllerProperty(roller.getPlayer());
    }

    /**
     * Triggered for each player when a seven is rolled.
     * Checks if the player has more than seven resources and executes DROP_CARDS action if needed.
     * @param playerController The playercontroller for which the check is run.
     */
    private void diceRollSevenPlayerAction(PlayerController playerController) {
        int numberOfResources = getTotalNumberOfResources(playerController.getPlayer());

        if (numberOfResources <= 7) return;

        withActivePlayer(
            playerController,
            () -> {
                playerController.setCardsToSelect((int) Math.floor(numberOfResources / 2.0));
                playerController.waitForNextAction(DROP_CARDS);
            });
    }

    /**
     * Calculates the total number of resources a given player has.
     * @return the total number of resources a given player has
     */
    private int getTotalNumberOfResources(Player player) {
        return player
            .getResources()
            .values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    /**
     * Allows the active playerController to move the robber and steal a card another player
     */
    private void moveRobber() {
        if (getActivePlayerController() == null) return;

        getActivePlayerController().waitForNextAction(SELECT_ROBBER_TILE);
        getActivePlayerController().waitForNextAction(SELECT_CARD_TO_STEAL);
    }

    /**
     * Distributes the resources of the given dice roll to the players.
     *
     * @param diceRoll The dice roll to distribute the resources for.
     */
    @StudentImplementationRequired("H2.2")
    public void distributeResources(final int diceRoll) {
        // TODO: H2.2 check, done
        activePlayerControllerProperty
            .getValue()
            .getPlayer()
            .getHexGrid()
            .getTiles(diceRoll)
            .stream()
            .filter(x -> !(x.hasRobber()))
            .forEach(tile -> tile.getSettlements()
                        .forEach(settlement -> settlement
                            .owner()
                            .addResource(tile.getType().resourceType, settlement.type().resourceAmount)));
    }
}
