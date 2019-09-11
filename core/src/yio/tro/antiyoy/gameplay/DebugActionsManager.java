package yio.tro.antiyoy.gameplay;

import yio.tro.antiyoy.KeyboardManager;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.gameplay.campaign.CampaignProgressManager;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyManager;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticCooldown;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticEntity;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticRelation;
import yio.tro.antiyoy.gameplay.name_generator.CityNameGenerator;
import yio.tro.antiyoy.gameplay.replays.ReplaySaveSystem;
import yio.tro.antiyoy.gameplay.rules.GameRules;
import yio.tro.antiyoy.menu.keyboard.AbstractKbReaction;
import yio.tro.antiyoy.menu.scenes.Scenes;
import yio.tro.antiyoy.stuff.Yio;

import java.util.ArrayList;

public class DebugActionsManager {

    private final GameController gameController;


    public DebugActionsManager(GameController gameController) {
        this.gameController = gameController;
    }


    public void debugActions() {
        doShowColorStuff();
    }


    private void doShowDipMessage() {
        Scenes.sceneDipMessage.showMessage("Debug", "Some random message");
    }


    private void doShowEditorProvinces() {
        gameController.levelEditor.editorProvinceManager.showProvincesInConsole();
    }


    private void doForceException() {
        Yio.forceException();
    }


    private void doKillRandomFriend() {
        DiplomacyManager diplomacyManager = gameController.fieldController.diplomacyManager;
        DiplomaticEntity mainEntity = diplomacyManager.getMainEntity();

        DiplomaticEntity friend = null;
        for (DiplomaticEntity entity : diplomacyManager.entities) {
            if (entity == mainEntity) continue;
            if (mainEntity.getRelation(entity) != DiplomaticRelation.FRIEND) continue;

            friend = entity;
            break;
        }

        if (friend == null) return;

        for (Hex activeHex : gameController.fieldController.activeHexes) {
            if (activeHex.fraction != friend.fraction) continue;

            gameController.fieldController.setHexFraction(activeHex, GameRules.NEUTRAL_FRACTION);
        }
    }


    private void doTestAreaSelectionMode() {
        gameController.fieldController.diplomacyManager.enableAreaSelectionMode(-1);
    }


    private void doForceDiplomaticLoss() {
        if (!GameRules.diplomacyEnabled) return;

        DiplomaticEntity randomAiDiplomaticEntity = getRandomAiDiplomaticEntity();
        if (randomAiDiplomaticEntity == null) return;

        doForceWinForDiplomaticEntity(randomAiDiplomaticEntity);
        Scenes.sceneNotification.show("Forced diplomatic loss");
    }


    private void doForceWinForDiplomaticEntity(DiplomaticEntity winner) {
        for (DiplomaticEntity entity : getDiplomacyManager().entities) {
            if (!entity.alive) continue;
            if (entity == winner) continue;
            if (entity.getRelation(winner) == DiplomaticRelation.FRIEND) continue;

            getDiplomacyManager().setRelation(entity, winner, DiplomaticRelation.FRIEND);
        }
    }


    private DiplomaticEntity getRandomAiDiplomaticEntity() {
        DiplomacyManager diplomacyManager = getDiplomacyManager();
        for (DiplomaticEntity entity : diplomacyManager.entities) {
            if (entity.isHuman()) continue;
            if (!entity.alive) continue;
            return entity;
        }
        return null;
    }


    private void doForceLevelSkipAvailability() {
        gameController.skipLevelManager.forceSkipAvailability();
        Scenes.sceneNotification.show("Level skip forced");
    }


    public void doShiftFractionsInEditorMode() {
        gameController.colorsManager.doShiftFractionsInEditorMode();
    }


    private void doShowKeyboard() {
        KeyboardManager.getInstance().apply("Debug", new AbstractKbReaction() {
            @Override
            public void onInputFromKeyboardReceived(String input) {
                System.out.println("input = " + input);
            }
        });
    }


    private void doShowSuspiciousStuff() {
        System.out.println();
        System.out.println("DebugActionsManager.doShowSuspiciousStuff");
        DiplomacyManager diplomacyManager = gameController.fieldController.diplomacyManager;

        ArrayList<DiplomaticCooldown> cooldowns = diplomacyManager.cooldowns;
        System.out.println("cooldowns.size() = " + cooldowns.size());

        gameController.snapshotManager.showInConsole();
    }


    private void doTestStringBuilder() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            builder.append(i).append(" ");
        }

        String one = builder.toString();

        builder.setLength(0);;
        for (int i = 0; i < 5; i++) {
            builder.append(3 * i).append(" ");
        }

        String two = builder.toString();

        System.out.println();
        System.out.println("DebugActionsManager.doTestStringBuilder");
        System.out.println("one = " + one);
        System.out.println("two = " + two);
    }


    private void doTakeSnapshot() {
        gameController.takeSnapshot();
    }


    private DiplomacyManager getDiplomacyManager() {
        return gameController.fieldController.diplomacyManager;
    }


    private void doShowDiplomaticCooldownsInConsole() {
        getDiplomacyManager().showCooldownsInConsole(gameController.turn);
    }


    private void doShowGameRules() {
        System.out.println();
        System.out.println("DebugActionsManager.doShowGameRules");
        System.out.println("GameRules.campaignMode = " + GameRules.campaignMode);
        System.out.println("CampaignProgressManager.getInstance().currentLevelIndex = " + CampaignProgressManager.getInstance().currentLevelIndex);
        System.out.println("GameRules.slayRules = " + GameRules.slayRules);
        System.out.println("GameRules.userLevelMode = " + GameRules.userLevelMode);
        System.out.println("GameRules.editorFog = " + GameRules.editorFog);
        System.out.println("GameRules.editorDiplomacy = " + GameRules.editorDiplomacy);
    }


    private void doShowDiplomaticMessage() {
        Scenes.sceneDipMessage.create();
        Scenes.sceneDipMessage.dialog.setMessage("Message", "HJdas hjashdk ahsdkj aha hsdja hkjas hkash jkdah kjash dkjsahd kah kjah dkjah dkjhaskjd hsk hhsdk asda");
    }


    private void doShowDiplomaticContracts() {
        getDiplomacyManager().showContractsInConsole(0);
    }


    private void doGenerateMultipleCityNames() {
        System.out.println();
        System.out.println("DebugActionsManager.generateMultipleCityNames");

        CityNameGenerator instance = CityNameGenerator.getInstance();
        FieldController fieldController = gameController.fieldController;
        ArrayList<Hex> activeHexes = fieldController.activeHexes;
        for (int i = 0; i < 10; i++) {
            Hex randomHex = activeHexes.get(YioGdxGame.random.nextInt(activeHexes.size()));
            String name = instance.generateName(randomHex);
            System.out.println("- " + name);
        }

        ArrayList<String> allNames = new ArrayList<>();
        for (Hex activeHex : activeHexes) {
            allNames.add(instance.generateName(activeHex));
        }

        int duplicates = 0;
        boolean hasDuplicates = false;
        for (int i = 0; i < allNames.size(); i++) {
            for (int j = i + 1; j < allNames.size(); j++) {
                if (allNames.get(i).equals(allNames.get(j))) {
                    duplicates++;
                }
            }
        }
        hasDuplicates = (duplicates > 0);

        System.out.println("hasDuplicates = " + hasDuplicates);
        if (hasDuplicates) {
            System.out.println("duplicates = " + duplicates);
        }
    }


    private void doShowNotification() {
        Scenes.sceneNotification.show("debug notification");
    }


    private void doShowSnapshotsInConsole() {
        gameController.snapshotManager.showInConsole();
    }


    private void doShowRuleset() {
        System.out.println();
        System.out.println("DebugActionsManager.doShowRuleset");
        System.out.println("GameRules.slayRules = " + GameRules.slayRules);
        String simpleName = gameController.ruleset.getClass().getSimpleName();
        System.out.println("simpleName = " + simpleName);
    }


    private void checkIfSomeProvincesAreDoubledInList() {
        ArrayList<Province> provinces = gameController.fieldController.provinces;
        for (int i = 0; i < provinces.size(); i++) {
            for (int j = 0; j < provinces.size(); j++) {
                Province A = provinces.get(i);
                Province B = provinces.get(j);
                if (i != j && A.equals(B)) {
                    System.out.println("found shit!");
                }
            }
        }
    }


    private void doSaveReplay() {
        ReplaySaveSystem instance = ReplaySaveSystem.getInstance();
        instance.saveReplay(gameController.replayManager.getReplay());
        Scenes.sceneNotification.show("Debug replay saved");
    }


    private void doShowReplayManager() {
        gameController.replayManager.showInConsole();
    }


    private void doShowSnapshots() {
        SnapshotManager snapshotManager = gameController.snapshotManager;
        snapshotManager.showInConsole();
    }


    private void doShowStatistics() {
        gameController.matchStatistics.showInConsole();
    }


    private void doGiveEverybodyLotOfMoney() {
        for (Province province : gameController.fieldController.provinces) {
            province.money += 1000;
        }
    }


    private void doShowColorStuff() {
        gameController.colorsManager.doShowInConsole();
    }


    private void doForceWin() {
        doCaptureRandomHexes();
    }


    private void doCaptureRandomHexes() {
        Scenes.sceneCheatsMenu.rbCaptureHexes.perform(null);
    }


    private boolean hasAtLeastOnePlayerHexNearby(Hex hex) {
        for (int dir = 0; dir < 6; dir++) {
            Hex adjacentHex = hex.getAdjacentHex(dir);
            if (adjacentHex == null) continue;
            if (!adjacentHex.active) continue;
            if (!adjacentHex.sameFraction(0)) continue;

            return true;
        }

        return false;
    }

}