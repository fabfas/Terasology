/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.rendering.gui.dialogs;

import com.google.common.collect.Lists;
import org.newdawn.slick.Color;
import org.terasology.config.ModuleConfig;
import org.terasology.engine.CoreRegistry;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.Module;
import org.terasology.engine.module.ModuleManager;
import org.terasology.rendering.gui.framework.UIDisplayContainer;
import org.terasology.rendering.gui.framework.UIDisplayElement;
import org.terasology.rendering.gui.framework.events.ClickListener;
import org.terasology.rendering.gui.framework.events.SelectionListener;
import org.terasology.rendering.gui.widgets.UIButton;
import org.terasology.rendering.gui.widgets.UIComposite;
import org.terasology.rendering.gui.widgets.UIDialog;
import org.terasology.rendering.gui.widgets.UILabel;
import org.terasology.rendering.gui.widgets.UIList;
import org.terasology.rendering.gui.widgets.UIListItem;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector4f;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Immortius
 */
public class UIDialogModules extends UIDialog {
    private static final Color ACTIVE_TEXT_COLOR = new Color(255, 220, 0);
    private static final Color ACTIVE_SELECTED_TEXT_COLOR = new Color(255, 255, 0);
    private static final Color INACTIVE_TEXT_COLOR = new Color(180, 180, 180);
    private static final Color INACTIVE_SELECTED_TEXT_COLOR = new Color(255, 255, 255);
    private static final String ACTIVATE_TEXT = "Activate";
    private static final String DEACTIVATE_TEXT = "Deactivate";
    private static final Color BLACK = new Color(0, 0, 0);

    private ModuleConfig moduleConfig;
    private ModuleConfig originalModuleConfig;
    private UIList modList;
    private UIButton toggleButton;
    private UILabel nameLabel;
    private UILabel descriptionLabel;
    private UIComposite detailPanel;
    private ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);

    public UIDialogModules(ModuleConfig moduleConfig) {
        super(new Vector2f(640f, 480f));
        this.moduleConfig = new ModuleConfig();
        this.originalModuleConfig = moduleConfig;
        this.moduleConfig.copy(moduleConfig);
        this.setEnableScrolling(false);
        populateModList();
        setTitle("Select Modules...");

    }

    private void populateModList() {
        List<Module> modules = Lists.newArrayList(moduleManager.getModules());
        Collections.sort(modules, new Comparator<Module>() {
            @Override
            public int compare(Module o1, Module o2) {
                return o1.getModuleInfo().getDisplayName().compareTo(o2.getModuleInfo().getDisplayName());
            }
        });

        for (Module module : modules) {
            if (!module.getModuleInfo().getId().equals(TerasologyConstants.ENGINE_MODULE)) {
                UIListItem item = new UIListItem(module.getModuleInfo().getDisplayName(), module);
                item.setPadding(new Vector4f(2f, 5f, 2f, 5f));
                if (moduleConfig.hasMod(module.getModuleInfo().getId())) {
                    item.setTextColor(ACTIVE_TEXT_COLOR);
                    item.setTextSelectionColor(ACTIVE_SELECTED_TEXT_COLOR);
                } else {
                    item.setTextColor(INACTIVE_TEXT_COLOR);
                    item.setTextSelectionColor(INACTIVE_SELECTED_TEXT_COLOR);
                }
                modList.addItem(item);
            }
        }
        modList.addSelectionListener(new SelectionListener() {
            @Override
            public void changed(UIDisplayElement element) {
                Module module = (Module) modList.getSelection().getValue();
                detailPanel.setVisible(true);
                nameLabel.setText(module.getModuleInfo().getDisplayName());
                descriptionLabel.setText(module.getModuleInfo().getDescription());
                boolean active = moduleConfig.hasMod(module.getModuleInfo().getId());
                if (active) {
                    toggleButton.getLabel().setText(DEACTIVATE_TEXT);
                } else {
                    toggleButton.getLabel().setText(ACTIVATE_TEXT);
                }
                toggleButton.setVisible(!module.getModuleInfo().getId().equals("core"));
            }
        });
        modList.addDoubleClickListener(new ClickListener() {
            @Override
            public void click(UIDisplayElement element, int button) {
                toggleSelectedModuleActivation();
            }
        });

        toggleButton.addClickListener(new ClickListener() {
            @Override
            public void click(UIDisplayElement element, int button) {
                toggleSelectedModuleActivation();
            }
        });
    }

    private void toggleSelectedModuleActivation() {
        Module selectedModule = (Module) modList.getSelection().getValue();
        if (selectedModule.getModuleInfo().getId().equals("core")) {
            return;
        }
        if (moduleConfig.hasMod(selectedModule.getModuleInfo().getId())) {
            deactivateMod(selectedModule);
            toggleButton.getLabel().setText(ACTIVATE_TEXT);
        } else {
            activateMod(selectedModule);
            toggleButton.getLabel().setText(DEACTIVATE_TEXT);
        }
        refreshListItemActivation();
    }

    private void refreshListItemActivation() {
        for (UIListItem item : modList.getItems()) {
            Module module = (Module) item.getValue();
            if (moduleConfig.hasMod(module.getModuleInfo().getId())) {
                item.setTextColor(ACTIVE_TEXT_COLOR);
                item.setTextSelectionColor(ACTIVE_SELECTED_TEXT_COLOR);
            } else {
                item.setTextColor(INACTIVE_TEXT_COLOR);
                item.setTextSelectionColor(INACTIVE_SELECTED_TEXT_COLOR);
            }
        }
    }

    private void deactivateMod(Module module) {
        moduleConfig.removeMod(module.getModuleInfo().getId());
        for (String activeModName : Lists.newArrayList(moduleConfig.listMods())) {
            Module activeModule = moduleManager.getModule(activeModName);
            if (activeModule != null && activeModule.getModuleInfo().getDependencies().contains(module.getModuleInfo().getId())) {
                deactivateMod(activeModule);
            }
        }
    }

    private void activateMod(Module module) {
        moduleConfig.addMod(module.getModuleInfo().getId());
        for (String dependencyName : module.getModuleInfo().getDependencies()) {
            Module dependency = moduleManager.getModule(dependencyName);
            if (dependency != null) {
                activateMod(dependency);
            }
        }
    }

    @Override
    protected void createDialogArea(UIDisplayContainer parent) {

        UIComposite modPanel = new UIComposite();
        modPanel.setPosition(new Vector2f(15, 50f));
        modPanel.setSize(new Vector2f(320f, 400f));
        modPanel.setVisible(true);

        detailPanel = new UIComposite();
        detailPanel.setPosition(new Vector2f(340, 50));
        detailPanel.setSize(new Vector2f(320, 400));
        detailPanel.setVisible(true);

        modList = new UIList();
        modList.setVisible(true);
        modList.setSize(new Vector2f(300f, 350f));
        modList.setPadding(new Vector4f(10f, 5f, 10f, 5f));
        modList.setBackgroundImage("engine:gui_menu", new Vector2f(264f, 18f), new Vector2f(159f, 63f));
        modList.setBorderImage("engine:gui_menu", new Vector2f(256f, 0f), new Vector2f(175f, 88f), new Vector4f(16f, 7f, 7f, 7f));

        modPanel.addDisplayElement(modList);
        modPanel.layout();

        UILabel label = new UILabel("Name:");
        label.setVisible(true);
        label.setPosition(new Vector2f(0, 0));
        label.setColor(BLACK);
        detailPanel.addDisplayElement(label);
        nameLabel = new UILabel();
        nameLabel.setVisible(true);
        nameLabel.setColor(BLACK);
        nameLabel.setTextShadow(false);
        nameLabel.setPosition(new Vector2f(label.getPosition().x + label.getSize().x + 10f, label.getPosition().y));
        detailPanel.addDisplayElement(nameLabel);
        label = new UILabel("Description:");
        label.setVisible(true);
        label.setPosition(new Vector2f(0, nameLabel.getPosition().y + nameLabel.getSize().y + 8f));
        label.setColor(BLACK);
        detailPanel.addDisplayElement(label);
        descriptionLabel = new UILabel();
        descriptionLabel.setColor(BLACK);
        descriptionLabel.setVisible(true);
        descriptionLabel.setWrap(true);
        descriptionLabel.setTextShadow(false);
        descriptionLabel.setPosition(new Vector2f(0, label.getPosition().y + label.getSize().y + 8f));
        descriptionLabel.setSize(new Vector2f(300, descriptionLabel.getSize().y));
        detailPanel.addDisplayElement(descriptionLabel);

        toggleButton = new UIButton(new Vector2f(128f, 32), UIButton.ButtonType.NORMAL);
        toggleButton.setPosition(new Vector2f(0, 240f));
        toggleButton.setVisible(true);

        detailPanel.addDisplayElement(toggleButton);
        detailPanel.setVisible(false);

        addDisplayElement(modPanel);
        addDisplayElement(detailPanel);

    }

    @Override
    protected void createButtons(UIDisplayContainer parent) {
        UIButton okButton = new UIButton(new Vector2f(128f, 32f), UIButton.ButtonType.NORMAL);
        okButton.getLabel().setText("Ok");
        okButton.setPosition(new Vector2f(getSize().x / 2 - okButton.getSize().x - 16f, getSize().y - okButton.getSize().y - 10));
        okButton.setVisible(true);

        okButton.addClickListener(new ClickListener() {
            @Override
            public void click(UIDisplayElement element, int button) {
                originalModuleConfig.copy(moduleConfig);
                close();
            }
        });

        UIButton cancelButton = new UIButton(new Vector2f(128f, 32f), UIButton.ButtonType.NORMAL);
        cancelButton.setPosition(new Vector2f(okButton.getPosition().x + okButton.getSize().x + 16f, okButton.getPosition().y));
        cancelButton.getLabel().setText("Cancel");
        cancelButton.setVisible(true);

        cancelButton.addClickListener(new ClickListener() {
            @Override
            public void click(UIDisplayElement element, int button) {
                close();
            }
        });

        parent.addDisplayElement(okButton);
        parent.addDisplayElement(cancelButton);
    }
}