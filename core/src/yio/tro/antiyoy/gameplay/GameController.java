package yio.tro.antiyoy.gameplay;

import yio.tro.antiyoy.SettingsManager;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.ai.AiFactory;
import yio.tro.antiyoy.ai.ArtificialIntelligence;
import yio.tro.antiyoy.gameplay.campaign.CampaignProgressManager;
import yio.tro.antiyoy.gameplay.data_storage.DecodeManager;
import yio.tro.antiyoy.gameplay.data_storage.EncodeManager;
import yio.tro.antiyoy.gameplay.data_storage.GameSaver;
import yio.tro.antiyoy.gameplay.data_storage.ImportManager;
import yio.tro.antiyoy.gameplay.editor.EditorSaveSystem;
import yio.tro.antiyoy.gameplay.editor.LevelEditor;
import yio.tro.antiyoy.gameplay.highlight.HighlightManager;
import yio.tro.antiyoy.gameplay.loading.LoadingManager;
import yio.tro.antiyoy.gameplay.loading.LoadingParameters;
import yio.tro.antiyoy.gameplay.messages.MessagesManager;
import yio.tro.antiyoy.gameplay.replays.ReplayManager;
import yio.tro.antiyoy.gameplay.rules.GameRules;
import yio.tro.antiyoy.gameplay.rules.Ruleset;
import yio.tro.antiyoy.gameplay.rules.RulesetGeneric;
import yio.tro.antiyoy.gameplay.rules.RulesetSlay;
import yio.tro.antiyoy.gameplay.touch_mode.TouchMode;
import yio.tro.antiyoy.gameplay.user_levels.UserLevelProgressManager;
import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.menu.scenes.Scenes;
import yio.tro.antiyoy.stuff.Fonts;
import yio.tro.antiyoy.stuff.GraphicsYio;
import yio.tro.antiyoy.stuff.PointYio;
import yio.tro.antiyoy.stuff.Yio;

import java.util.ArrayList;
import java.util.Random;


public class GameController {

    private final DebugActionsManager debugActionsManager;
    public YioGdxGame yioGdxGame;

    public final SelectionManager selectionManager;
    public final FieldController fieldController;
    public CameraController cameraController;
    public final AiFactory aiFactory;

    public Random random, predictableRandom;
    public boolean letsUpdateCacheByAnim;
    public boolean updateWholeCache;
    public long currentTime;
    public boolean checkToMarch;
    public boolean ignoreMarch;
    public int turn;
    private long timeToUpdateCache;
    boolean readyToEndTurn;
    private boolean proposedSurrender;
    public boolean backgroundVisible;
    private ArrayList<ArtificialIntelligence> aiList;
    public ArrayList<Unit> unitList;
    public int marchDelay;
    public int playersNumber;
    public String balanceString;
    public String currentPriceString;
    public LoadingParameters initialParameters;
    public float priceStringWidth;
    public MapGenerator mapGeneratorSlay;
    public MapGeneratorGeneric mapGeneratorGeneric;
    public Unit jumperUnit;
    public MatchStatistics matchStatistics;
    public GameSaver gameSaver;
    public Forefinger forefinger;
    public TutorialScript tutorialScript;
    public LevelEditor levelEditor;
    public int currentTouchCount;
    public Ruleset ruleset;
    ClickDetector clickDetector;
    public PointYio touchPoint;
    public PointYio convertedTouchPoint;
    public SnapshotManager snapshotManager;
    public SpeedManager speedManager;
    public ReplayManager replayManager;
    public NamingManager namingManager;
    public SkipLevelManager skipLevelManager;
    public LevelSizeManager levelSizeManager;
    public ColorsManager colorsManager;
    public EncodeManager encodeManager;
    public DecodeManager decodeManager;
    public ImportManager importManager;
    public EditorSaveSystem editorSaveSystem;
    public TouchMode touchMode;
    ArrayList<TouchMode> dyingTms;
    public MessagesManager messagesManager;
    public HighlightManager highlightManager;


    public GameController(YioGdxGame yioGdxGame) {
        this.yioGdxGame = yioGdxGame;
        random = new Random();
        predictableRandom = new Random(0);
        CampaignProgressManager.getInstance();
        selectionManager = new SelectionManager(this);
        marchDelay = 500;
        cameraController = new CameraController(this);
        unitList = new ArrayList<>();
        dyingTms = new ArrayList<>();

        editorSaveSystem = new EditorSaveSystem(this);
        decodeManager = new DecodeManager(this);
        importManager = new ImportManager(this);
        encodeManager = new EncodeManager(this);
        colorsManager = new ColorsManager(this);
        mapGeneratorSlay = new MapGenerator(this);
        convertedTouchPoint = new PointYio();
        mapGeneratorGeneric = new MapGeneratorGeneric(this);
        aiList = new ArrayList<>();
        initialParameters = new LoadingParameters();
        touchPoint = new PointYio();
        snapshotManager = new SnapshotManager(this);
        fieldController = new FieldController(this);
        jumperUnit = new Unit(this, fieldController.nullHex, 0);
        speedManager = new SpeedManager(this);
        replayManager = new ReplayManager(this);
        namingManager = new NamingManager(this);
        levelSizeManager = new LevelSizeManager(this);

        matchStatistics = new MatchStatistics();
        gameSaver = new GameSaver(this);
        forefinger = new Forefinger(this);
        levelEditor = new LevelEditor(this);
        aiFactory = new AiFactory(this);
        debugActionsManager = new DebugActionsManager(this);
        clickDetector = new ClickDetector();
        skipLevelManager = new SkipLevelManager(this);
        messagesManager = new MessagesManager(this);
        highlightManager = new HighlightManager(this);

        LoadingManager.getInstance().setGameController(this);
        TouchMode.createModes(this);
        touchMode = null;
    }


    void takeAwaySomeMoneyToAchieveBalance() {
        // so the problem is that all players except first
        // get income in the first turn
        updateRuleset();
        for (Province province : fieldController.provinces) {
            if (province.getFraction() == 0) continue; // first player is not getting income at first turn
            province.money -= province.getIncome() - province.getTaxes();
        }
    }


    private void checkForAloneUnits() {
        for (int i = 0; i < unitList.size(); i++) {
            Unit unit = unitList.get(i);
            if (isCurrentTurn(unit.getFraction()) && unit.currentHex.numberOfFriendlyHexesNearby() == 0) {
                fieldController.killUnitByStarvation(unit.currentHex);
                i--;
            }
        }
    }


    private void checkForBankrupts() {
        for (Province province : fieldController.provinces) {
            if (isCurrentTurn(province.getFraction())) {
                if (province.money < 0) {
                    province.money = 0;
                    fieldController.killEveryoneByStarvation(province);
                }
            }
        }
    }


    public void move() {
        updateCurrentTime();
        matchStatistics.increaseTimeCount();
        cameraController.move();

        checkForAiToMove();
        checkToEndTurn();
        checkToUpdateCacheByAnim();

        doCheckAnimHexes();

        moveCheckToMarch();
        moveUnits();
        fieldController.moveAnimHexes();
        selectionManager.moveSelections();

        jumperUnit.moveJumpAnim();
        fieldController.moveZoneManager.move();
        selectionManager.getBlackoutFactor().move();
        selectionManager.moveDefenseTips();
        levelEditor.move();

        fieldController.moveZoneManager.checkToClear();
        selectionManager.tipFactor.move();
        moveTouchMode();

        fieldController.moveResponseAnimHex();
        moveTutorialStuff();
        namingManager.move();
        skipLevelManager.move();
        messagesManager.move();
        highlightManager.move();
    }


    private void moveTouchMode() {
        if (touchMode != null) {
            touchMode.move();
        }

        for (int i = dyingTms.size() - 1; i >= 0; i--) {
            TouchMode dtm = dyingTms.get(i);
            dtm.move();
            if (dtm.isReadyToBeRemoved()) {
                dyingTms.remove(dtm);
            }
        }
    }


    private void updateCurrentTime() {
        currentTime = System.currentTimeMillis();
    }


    private void doCheckAnimHexes() {
        if (!fieldController.letsCheckAnimHexes) return;
        if (currentTime <= fieldController.timeToCheckAnimHexes) return;
        if (!doesCurrentTurnEndDependOnAnimHexes()) return;

        fieldController.checkAnimHexes();
    }


    private void moveTutorialStuff() {
        if (GameRules.tutorialMode) {
            forefinger.move();
            tutorialScript.move();
        }
    }


    private void checkForAiToMove() {
        if (readyToEndTurn) return;

        if (GameRules.replayMode) {
            replayManager.performStep();
            return; // AI can't do anything by itself in replays
        }

        if (isPlayerTurn()) return;
        performAiMove();
    }


    private void performAiMove() {
        aiList.get(turn).perform();
        updateCacheOnceAfterSomeTime();
        applyReadyToEndTurn();
    }


    public void applyReadyToEndTurn() {
        readyToEndTurn = true;
    }


    public void onInitialSnapshotRecreated() {
        turn = 0;
        readyToEndTurn = false;
        prepareCertainUnitsToMove();
    }


    private void moveUnits() {
        for (Unit unit : unitList) {
            unit.moveJumpAnim();
            unit.move();
        }
    }


    public void setIgnoreMarch(boolean ignoreMarch) {
        this.ignoreMarch = ignoreMarch;
    }


    private void moveCheckToMarch() {
        if (!SettingsManager.longTapToMove) return;
        if (ignoreMarch) return;
        if (!checkToMarch) return;
        if (!checkConditionsToMarch()) return;

        checkToMarch = false;
        fieldController.updateFocusedHex();
        selectionManager.setSelectedUnit(null);
        if (fieldController.focusedHex != null && fieldController.focusedHex.active) {
            fieldController.marchUnitsToHex(fieldController.focusedHex);
        }
    }


    boolean checkConditionsToMarch() {
        if (currentTouchCount != 1) return false;
        if (currentTime - cameraController.touchDownTime <= marchDelay) return false;
        if (!cameraController.touchedAsClick()) return false;

        return true;
    }


    private void checkToUpdateCacheByAnim() {
        if (!isReadyToUpdateCache()) return;

        letsUpdateCacheByAnim = false;

        updateCache();
        updateFogOfWar();

        updateWholeCache = false;
    }


    private boolean isReadyToUpdateCache() {
        if (!letsUpdateCacheByAnim) return false;
        if (currentTime <= timeToUpdateCache) return false;
        if (!doesCurrentTurnEndDependOnAnimHexes()) return false;
        if (isSomethingMoving()) return false;

        return true;
    }


    private void updateFogOfWar() {
        if (!GameRules.fogOfWarEnabled) return;

        fieldController.fogOfWarManager.updateFog();
    }


    private void updateCache() {
        if (updateWholeCache) {
            yioGdxGame.gameView.updateCacheLevelTextures();
        } else {
            yioGdxGame.gameView.updateCacheNearAnimHexes();
        }
    }


    private boolean canEndTurn() {
        if (isInEditorMode()) return false;
        if (!readyToEndTurn) return false;
        if (!cameraController.checkConditionsToEndTurn()) return false;
        if (speedManager.getSpeed() == SpeedManager.SPEED_PAUSED) return false;

        if (doesCurrentTurnEndDependOnAnimHexes()) {
            return fieldController.animHexes.size() == 0;
        } else {
            return true;
        }
    }


    private boolean doesCurrentTurnEndDependOnAnimHexes() {
        if (DebugFlags.testMode) return false;
        if (isPlayerTurn()) return true;

        if (speedManager.getSpeed() == SpeedManager.SPEED_FAST_FORWARD) {
            return isCurrentTurn(0);
        }

        return true;
    }


    public boolean haveToAskToEndTurn() {
        if (GameRules.tutorialMode) return false;

        return SettingsManager.askToEndTurn && fieldController.atLeastOneUnitIsReadyToMove();
    }


    private void checkToEndTurn() {
        if (!canEndTurn()) return;

        readyToEndTurn = false;
        endTurnActions();
        turn = getNextTurnIndex();
        turnStartActions();
    }


    public void prepareCertainUnitsToMove() {
        for (Unit unit : unitList) {
            if (isCurrentTurn(unit.getFraction())) {
                unit.setReadyToMove(true);
                unit.startJumping();
            }
        }
    }


    public void stopAllUnitsFromJumping() {
        for (Unit unit : unitList) {
//            unit.setReadyToMove(false);
            unit.stopJumping();
        }
    }


    private int checkIfWeHaveWinner() {
        if (fieldController.activeHexes.size() == 0) return -1;
        if (GameRules.diplomacyEnabled) {
            return fieldController.diplomacyManager.getDiplomaticWinner();
        }
        if (!fieldController.isThereOnlyOneKingdomOnMap()) return -1;

        for (Province province : fieldController.provinces) {
            if (province.hexList.get(0).isNeutral()) continue;
            return province.getFraction();
        }

        System.out.println("Problem in GameController.checkIfWeHaveWinner()");
        return -1;
    }


    private int zeroesInArray(int array[]) {
        int zeroes = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == 0) zeroes++;
        }
        return zeroes;
    }


    private void checkToEndGame() {
        if (GameRules.replayMode) return;

        int winnerFraction = checkIfWeHaveWinner();
        if (winnerFraction >= 0) {
            endGame(winnerFraction);
            return;
        }

        checkToProposeSurrender();
    }


    private void checkToProposeSurrender() {
        if (GameRules.diplomacyEnabled) return;
        if (playersNumber != 1) return;

        if (!proposedSurrender) {
            int possibleWinner = fieldController.possibleWinner();
            if (possibleWinner >= 0 && isPlayerTurn(possibleWinner)) {
                doProposeSurrender();
                proposedSurrender = true;
            }
        }
    }


    private void doProposeSurrender() {
        onGameFinished(turn);
        Scenes.sceneSurrenderDialog.create();
    }


    private int indexOfNumberInArray(int array[], int number) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == number) return i;
        }
        return -1;
    }


    public static int maxNumberFromArray(int array[]) {
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }
        return max;
    }


    public void forceGameEnd() {
        int playerHexCount[] = fieldController.getPlayerHexCount();
        int maxNumber = maxNumberFromArray(playerHexCount);
        int winnerFraction = 0;
        for (int i = 0; i < playerHexCount.length; i++) {
            if (maxNumber == playerHexCount[i]) {
                winnerFraction = i;
                break;
            }
        }

        fieldController.clearProvincesList();
        ArrayList<Hex> hexList = new ArrayList<>();
        for (Hex activeHex : fieldController.activeHexes) {
            if (activeHex.fraction == winnerFraction) {
                hexList.add(activeHex);
                break;
            }
        }
        fieldController.provinces.add(new Province(this, hexList));

        checkToEndGame();
    }


    private void endGame(int winFraction) {
        if (DebugFlags.testMode) {
            DebugFlags.testWinner = winFraction;
            return;
        }

        onGameFinished(winFraction);

        if (winFraction == 0) {
            GlobalStatistics.getInstance().onGameWon();
        }

        System.out.println("GameController.endGame");
        System.out.println("yioGdxGame.gamePaused = " + yioGdxGame.gamePaused);

        Scenes.sceneIncomeGraph.hide();
        Scenes.sceneAfterGameMenu.create(winFraction, isPlayerTurn(winFraction));
    }


    private void onGameFinished(int winFraction) {
        checkToTagCampaignLevel(winFraction);
        checkToTagUserLevel(winFraction);
    }


    private void checkToTagUserLevel(int winFraction) {
        if (!isPlayerTurn(winFraction)) return;

        String key = GameRules.ulKey;
        if (key == null) return;

        UserLevelProgressManager instance = UserLevelProgressManager.getInstance();

        instance.onLevelCompleted(key);
    }


    private void checkToTagCampaignLevel(int winFraction) {
        CampaignProgressManager instance = CampaignProgressManager.getInstance();

        if (instance.areCampaignLevelCompletionConditionsSatisfied(winFraction)) {
            instance.markLevelAsCompleted(instance.currentLevelIndex);
            Scenes.sceneCampaignMenu.updateLevelSelector();
        }
    }


    public void resetCurrentTouchCount() {
        currentTouchCount = 0;
    }


    public void resetProgress() {
        CampaignProgressManager.getInstance().resetProgress();

        yioGdxGame.selectedLevelIndex = 1;
    }


    private void endTurnActions() {
        checkToEndGame();
        ruleset.onTurnEnd();
        replayManager.onTurnEnded();
        fieldController.diplomacyManager.onTurnEnded();

        for (Unit unit : unitList) {
            unit.setReadyToMove(false);
            unit.stopJumping();
        }
    }


    private void turnStartActions() {
        selectionManager.deselectAll();

        if (isCurrentTurn(0)) {
            matchStatistics.onTurnMade();
            if (playersNumber == 1) {
                GlobalStatistics.getInstance().updateByMatchStatistics(matchStatistics);
            }
            fieldController.expandTrees();
        }

        prepareCertainUnitsToMove();
        fieldController.transformGraves(); // this must be called before 'check for bankrupts' and after 'expand trees'
        collectTributesAndPayTaxes();
        checkForStarvation();

        checkToUpdateCacheTextures();

        if (isPlayerTurn()) {
            resetCurrentTouchCount();
            snapshotManager.onTurnStart();
            jumperUnit.startJumping();
            checkToSkipTurn();
            fieldController.fogOfWarManager.updateFog();
        } else {
            for (Hex animHex : fieldController.animHexes) {
                animHex.animFactor.setValues(1, 0);
            }
        }

        fieldController.diplomacyManager.onTurnStarted();
        fieldController.checkToFocusCameraOnCurrentPlayer();

        checkToAutoSave();
    }


    private void checkToUpdateCacheTextures() {
        if (!isCurrentTurn(0)) return;

        yioGdxGame.gameView.updateCacheLevelTextures();
    }


    private void checkToSkipTurn() {
        if (fieldController.numberOfProvincesWithFraction(turn) != 0) return;
        onEndTurnButtonPressed();
    }


    private void checkForStarvation() {
        if (GameRules.replayMode) return;

        checkForBankrupts();
        checkForAloneUnits();
    }


    public void checkToAutoSave() {
        if (!SettingsManager.autosave) return;
        if (turn != 0) return;
        if (playersNumber <= 0) return;

        performAutosave();
    }


    private void collectTributesAndPayTaxes() {
        for (Province province : fieldController.provinces) {
            if (isCurrentTurn(province.getFraction())) {
                province.money += province.getBalance();
            }
        }
    }


    void updateCacheOnceAfterSomeTime() {
        letsUpdateCacheByAnim = true;
        timeToUpdateCache = System.currentTimeMillis() + 30;
    }


    public void onEndTurnButtonPressed() {
        cameraController.onEndTurnButtonPressed();

        if (!isPlayerTurn()) return;

        if (isInMultiplayerMode()) {
            endTurnInMultiplayerMode();
        } else {
            applyReadyToEndTurn();
        }
    }


    private void endTurnInMultiplayerMode() {
        if (!SettingsManager.askToEndTurn) {
            applyReadyToEndTurn();
            return;
        }

        Scenes.sceneTurnStartDialog.create();

        int nextTurnIndex = turn;
        while (fieldController.hasAtLeastOneProvince()) {
            nextTurnIndex = getNextTurnIndex(nextTurnIndex);
            if (isPlayerTurn(nextTurnIndex) && fieldController.numberOfProvincesWithFraction(nextTurnIndex) > 0) {
                break;
            }
        }

        Scenes.sceneTurnStartDialog.dialog.setColor(getColorByFraction(nextTurnIndex));
    }


    public boolean isInMultiplayerMode() {
        return playersNumber > 1;
    }


    public void defaultValues() {
        cameraController.defaultValues();
        ignoreMarch = false;
        readyToEndTurn = false;
        fieldController.defaultValues();
        selectionManager.setSelectedUnit(null);
        turn = 0;
        jumperUnit.startJumping();

        matchStatistics.defaultValues();
        speedManager.defaultValues();
        replayManager.defaultValues();
        GameRules.defaultValues();
        namingManager.defaultValues();
        skipLevelManager.defaultValues();
        colorsManager.defaultValues();
        levelEditor.defaultValues();
        messagesManager.defaultValues();

        proposedSurrender = false;
        backgroundVisible = true;
        resetTouchMode();
    }


    public void setPlayersNumber(int playersNumber) {
        this.playersNumber = playersNumber;
    }


    public void initTutorial() {
        SettingsManager.fastConstructionEnabled = false;

        if (GameRules.slayRules) {
            tutorialScript = new TutorialScriptSlayRules(this);
        } else {
            tutorialScript = new TutorialScriptGenericRules(this);
        }
        tutorialScript.createTutorialGame();
        GameRules.tutorialMode = true;
    }


    public int convertSliderIndexToColorOffset(int sliderIndex, int fractionsQuantity) {
        if (sliderIndex == 0) { // random
            return predictableRandom.nextInt(Math.min(fractionsQuantity, GameRules.NEUTRAL_FRACTION));
        }
        return sliderIndex - 1;
    }


    public void onEndCreation() {
        snapshotManager.clear();
        fieldController.createPlayerHexCount();
        updateRuleset();
        createCamera();
        fieldController.onEndCreation();
        aiFactory.createAiList(GameRules.difficulty);
        selectionManager.deselectAll();
        replayManager.onEndCreation();
        namingManager.onEndCreation();
        skipLevelManager.onEndCreation();
        messagesManager.onEndCreation();
    }


    public void checkToEnableAiOnlyMode() {
        if (playersNumber != 0) return;
        GameRules.aiOnlyMode = true;
    }


    public void updateInitialParameters(LoadingParameters parameters) {
        initialParameters.copyFrom(parameters);
    }


    private void sayArray(int array[]) {
        System.out.print("[ ");
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println("]");
    }


    public void createCamera() {
        cameraController.createCamera();
    }


    public void debugActions() {
        debugActionsManager.debugActions();
    }


    public boolean areCityNamesEnabled() {
        return SettingsManager.cityNamesEnabled;
    }


    void selectAdjacentHexes(Hex startHex) {
        selectionManager.selectAdjacentHexes(startHex);
    }


    public void addSolidObject(Hex hex, int type) {
        fieldController.addSolidObject(hex, type);
    }


    public void cleanOutHex(Hex hex) {
        fieldController.cleanOutHex(hex);
    }


    public int getColorByFraction(int fraction) {
        return colorsManager.getColorByFraction(fraction);
    }


    private void performAutosave() {
        yioGdxGame.saveSystem.performAutosave();
    }


    public void takeSnapshot() {
        snapshotManager.takeSnapshot();
    }


    public int mergedUnitStrength(Unit unit1, Unit unit2) {
        return unit1.strength + unit2.strength;
    }


    public boolean canUnitAttackHex(int unitStrength, int unitFraction, Hex hex) {
        if (!GameRules.diplomacyEnabled) {
            return ruleset.canUnitAttackHex(unitStrength, hex);
        }

        return fieldController.diplomacyManager.canUnitAttackHex(unitStrength, unitFraction, hex);
    }


    boolean canMergeUnits(int strength1, int strength2) {
        return strength1 + strength2 <= 4;
    }


    public boolean mergeUnits(Hex hex, Unit unit1, Unit unit2) {
        if (!ruleset.canMergeUnits(unit1, unit2)) return false;

        fieldController.cleanOutHex(unit1.currentHex);
        fieldController.cleanOutHex(unit2.currentHex);
        Unit mergedUnit = fieldController.addUnit(hex, mergedUnitStrength(unit1, unit2));
        matchStatistics.onUnitsMerged();
        mergedUnit.setReadyToMove(true);
        if (!unit1.isReadyToMove() || !unit2.isReadyToMove()) {
            mergedUnit.setReadyToMove(false);
            mergedUnit.stopJumping();
        }

        return true;
    }


    void tickleMoneySign() {
        ButtonYio coinButton = yioGdxGame.menuControllerYio.getCoinButton();
        coinButton.appearFactor.setValues(1, 0.13);
        coinButton.appearFactor.appear(4, 1);
    }


    public void restartGame() {
        if (fieldController.initialLevelString != null) {
            gameSaver.legacyImportManager.applyFullLevel(initialParameters, fieldController.initialLevelString);
        }

        LoadingManager.getInstance().startGame(initialParameters);
    }


    public void undoAction() {
        boolean success = snapshotManager.undoAction();

        if (success) {
            resetCurrentTouchCount();
        }
    }


    public void turnOffEditorMode() {
        GameRules.inEditorMode = false;
    }


    void updateCurrentPriceString() {
        currentPriceString = "$" + selectionManager.getCurrentTipPrice();
        priceStringWidth = GraphicsYio.getTextWidth(Fonts.gameFont, currentPriceString);
    }


    void updateBalanceString() {
        if (fieldController.selectedProvince != null) {
            balanceString = fieldController.selectedProvince.getBalanceString();
        }
    }


    public Unit addUnit(Hex hex, int strength) {
        return fieldController.addUnit(hex, strength);
    }


    boolean isSomethingMoving() {
        for (Hex hex : fieldController.animHexes) {
            if (hex.containsUnit() && hex.unit.moveFactor.get() < 1) return true;
        }
        if (GameRules.inEditorMode && levelEditor.isSomethingMoving()) return true;
        return false;
    }


    public LevelEditor getLevelEditor() {
        return levelEditor;
    }


    public void touchDown(int screenX, int screenY, int pointer, int button) {
        touchPoint.set(screenX, screenY);
        currentTouchCount++;

        touchMode.touchDownReaction();
        clickDetector.onTouchDown(touchPoint);
    }


    public void detectAndShowMoveZoneForBuildingUnit(int strength) {
        fieldController.moveZoneManager.detectAndShowMoveZoneForBuildingUnit(strength);
    }


    public void detectAndShowMoveZoneForFarm() {
        fieldController.moveZoneManager.detectAndShowMoveZoneForFarm();
    }


    public ArrayList<Hex> detectMoveZone(Hex startHex, int strength) {
        return fieldController.moveZoneManager.detectMoveZone(startHex, strength);
    }


    public ArrayList<Hex> detectMoveZone(Hex startHex, int strength, int moveLimit) {
        return fieldController.moveZoneManager.detectMoveZone(startHex, strength, moveLimit);
    }


    public void addAnimHex(Hex hex) {
        fieldController.addAnimHex(hex);
    }


    Province findProvinceCopy(Province src) {
        return fieldController.findProvinceCopy(src);
    }


    public Province getProvinceByHex(Hex hex) {
        return fieldController.getProvinceByHex(hex);
    }


    private int getNextTurnIndex(int currentTurn) {
        int next = currentTurn + 1;

        if (next == GameRules.NEUTRAL_FRACTION) {
            next++;
        }

        if (next >= GameRules.fractionsQuantity) {
            next = 0;
        }

        return next;
    }


    private int getNextTurnIndex() {
        return getNextTurnIndex(turn);
    }


    public boolean isPlayerTurn(int turn) {
        return turn < playersNumber;
    }


    public boolean isPlayerTurn() {
        return isPlayerTurn(turn);
    }


    public boolean isCurrentTurn(int fraction) {
        return turn == fraction;
    }


    public void moveUnit(Unit unit, Hex target, Province unitProvince) {
        if (!unit.isReadyToMove()) {
            System.out.println("Someone tried to move unit that is not ready to move: " + unit);
            if (!GameRules.replayMode) return;
        }

        replayManager.onUnitMoved(unit.currentHex, target);
        if (isMovementPeaceful(unit, target)) {
            moveUnitPeacefully(unit, target);
        } else {
            moveUnitWithAttack(unit, target, unitProvince);
        }

        if (isPlayerTurn()) {
            fieldController.moveZoneManager.hide();
            updateBalanceString();
        }
    }


    private boolean isMovementPeaceful(Unit unit, Hex target) {
        return unit.currentHex.sameFraction(target);
    }


    private void moveUnitWithAttack(Unit unit, Hex destination, Province unitProvince) {
        if (!destination.canBeAttackedBy(unit) || unitProvince == null) {
            System.out.println("Problem in GameController.moveUnitWithAttack");
            Yio.printStackTrace();
            return;
        }

        fieldController.setHexFraction(destination, turn); // must be called before object in hex destroyed
        fieldController.cleanOutHex(destination);
        unit.moveToHex(destination);
        unitProvince.addHex(destination);
        if (isPlayerTurn()) {
            fieldController.selectedHexes.add(destination);
            updateCacheOnceAfterSomeTime();
        }
    }


    private void moveUnitPeacefully(Unit unit, Hex target) {
        if (!target.containsUnit()) {
            unit.moveToHex(target);
        } else {
            mergeUnits(target, unit, target.unit);
        }

        if (isPlayerTurn()) {
            fieldController.setResponseAnimHex(target);
        }
    }


    public void onClick() {
        touchMode.onClick();
    }


    public void showFocusedHexInConsole() {
        if (!DebugFlags.showFocusedHexInConsole) return;

        Hex focusedHex = fieldController.focusedHex;
        YioGdxGame.say("Hex: " + focusedHex.fraction + " " + focusedHex.index1 + " " + focusedHex.index2);
    }


    public void setCheckToMarch(boolean checkToMarch) {
        this.checkToMarch = checkToMarch;
    }


    public void setTouchMode(TouchMode touchMode) {
        if (this.touchMode == touchMode) return;

        if (this.touchMode != null) {
            onTmEnd();
        }

        this.touchMode = touchMode;
        touchMode.onModeBegin();

        if (dyingTms.contains(touchMode)) {
            dyingTms.remove(touchMode);
        }
    }


    public void resetTouchMode() {
        if (isInEditorMode()) {
            setTouchMode(TouchMode.tmEditor);
            return;
        }

        setTouchMode(TouchMode.tmDefault);
    }


    private void onTmEnd() {
        touchMode.kill();
        touchMode.onModeEnd();

        if (!dyingTms.contains(touchMode)) {
            dyingTms.add(touchMode);
        }
    }


    public void touchUp(int screenX, int screenY, int pointer, int button) {
        touchPoint.set(screenX, screenY);
        currentTouchCount--;

        if (currentTouchCount < 0) {
            currentTouchCount = 0;
        }

        touchMode.touchUpReaction();
        clickDetector.onTouchUp(touchPoint);

        if (clickDetector.isClicked()) {
            onClick();
        }
    }


    public void touchDragged(int screenX, int screenY, int pointer) {
        touchPoint.set(screenX, screenY);

        touchMode.touchDragReaction();
        clickDetector.onTouchDrag(touchPoint);
    }


    public void updateRuleset() {
        if (GameRules.slayRules) {
            ruleset = new RulesetSlay(this);
        } else {
            ruleset = new RulesetGeneric(this);
        }
    }


    public void scrolled(int amount) {
        if (touchMode.onMouseWheelScrolled(amount)) return;

        if (amount == 1) {
            cameraController.changeZoomLevel(0.5);
        } else if (amount == -1) {
            cameraController.changeZoomLevel(-0.6);
        }
    }


    public void close() {
        for (int i = 0; i < fieldController.fWidth; i++) {
            for (int j = 0; j < fieldController.fHeight; j++) {
                if (fieldController.field[i][j] != null) fieldController.field[i][j].close();
            }
        }
        if (fieldController.provinces != null) {
            for (Province province : fieldController.provinces) {
                province.close();
            }
        }

        fieldController.provinces = null;
        fieldController.field = null;
        yioGdxGame = null;
    }


    public ArrayList<Unit> getUnitList() {
        return unitList;
    }


    public MapGenerator getMapGeneratorSlay() {
        return mapGeneratorSlay;
    }


    public Random getPredictableRandom() {
        return predictableRandom;
    }


    public MapGeneratorGeneric getMapGeneratorGeneric() {
        return mapGeneratorGeneric;
    }


    public YioGdxGame getYioGdxGame() {
        return yioGdxGame;
    }


    public boolean isInEditorMode() {
        return GameRules.inEditorMode;
    }


    public Random getRandom() {
        return random;
    }


    public long getCurrentTime() {
        return currentTime;
    }


    public MatchStatistics getMatchStatistics() {
        return matchStatistics;
    }


    public int getTurn() {
        return turn;
    }


    public ArrayList<ArtificialIntelligence> getAiList() {
        return aiList;
    }


    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
    }
}
