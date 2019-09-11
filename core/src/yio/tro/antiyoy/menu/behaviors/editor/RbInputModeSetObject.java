package yio.tro.antiyoy.menu.behaviors.editor;

import yio.tro.antiyoy.gameplay.editor.LeInputMode;
import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.gameplay.editor.LevelEditor;
import yio.tro.antiyoy.menu.behaviors.Reaction;

/**
 * Created by yiotro on 27.11.2015.
 */
public class RbInputModeSetObject extends Reaction {

    @Override
    public void perform(ButtonYio buttonYio) {
        getGameController(buttonYio).getLevelEditor().setInputMode(LeInputMode.set_object);
        getGameController(buttonYio).getLevelEditor().setInputObject(buttonYio.id - 162);
    }
}
