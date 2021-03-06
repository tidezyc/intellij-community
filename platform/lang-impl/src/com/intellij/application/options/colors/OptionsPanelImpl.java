/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.application.options.colors;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ListScrollingUtil;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBList;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class OptionsPanelImpl extends JPanel implements OptionsPanel {
  private static final Comparator<EditorSchemeAttributeDescriptor> ATTR_COMPARATOR = new Comparator<EditorSchemeAttributeDescriptor>() {
    @Override
    public int compare(EditorSchemeAttributeDescriptor o1, EditorSchemeAttributeDescriptor o2) {
      return StringUtil.naturalCompare(o1.toString(), o2.toString());
    }
  };
  private final JBList myOptionsList;
  private final ColorAndFontDescriptionPanel myOptionsPanel;

  private final ColorAndFontOptions myOptions;
  private final SchemesPanel mySchemesProvider;
  private final String myCategoryName;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);
  private final CollectionListModel<EditorSchemeAttributeDescriptor> myListModel;

  public OptionsPanelImpl(ColorAndFontDescriptionPanel optionsPanel,
                          ColorAndFontOptions options,
                          SchemesPanel schemesProvider,
                          String categoryName) {
    super(new BorderLayout());
    myOptions = options;
    mySchemesProvider = schemesProvider;
    myCategoryName = categoryName;

    optionsPanel.setActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        myDispatcher.getMulticaster().settingsChanged();
      }
    });

    myListModel = new CollectionListModel<EditorSchemeAttributeDescriptor>();
    myOptionsList = new JBList(myListModel);
    new ListSpeedSearch(myOptionsList);

    myOptionsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!mySchemesProvider.areSchemesLoaded()) return;
        processListValueChanged();
      }
    });
    myOptionsList.setCellRenderer(new DefaultListCellRenderer(){
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof ColorAndFontDescription) {
          setIcon(((ColorAndFontDescription)value).getIcon());
          setToolTipText(((ColorAndFontDescription)value).getToolTip());
        }
        return component;
      }
    });

    myOptionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myOptionsList);
    scrollPane.setPreferredSize(new Dimension(230, 60));
    JPanel north = new JPanel(new BorderLayout());
    north.add(scrollPane, BorderLayout.CENTER);
    north.add(optionsPanel, BorderLayout.EAST);
    myOptionsPanel = optionsPanel;
    add(north, BorderLayout.NORTH);
  }

  @Override
  public void addListener(ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  private void processListValueChanged() {
    Object selectedValue = myOptionsList.getSelectedValue();
    ColorAndFontDescription description = (ColorAndFontDescription)selectedValue;
    ColorAndFontDescriptionPanel optionsPanel = myOptionsPanel;
    if (description == null) {
      optionsPanel.resetDefault();
      return;
    }
    optionsPanel.reset(description);

    myDispatcher.getMulticaster().selectedOptionChanged(description);

  }

  private void fillOptionsList() {
    int selIndex = myOptionsList.getSelectedIndex();

    myListModel.removeAll();

    ArrayList<EditorSchemeAttributeDescriptor> list = ContainerUtil.newArrayList();
    for (EditorSchemeAttributeDescriptor description : myOptions.getCurrentDescriptions()) {
      if (!description.getGroup().equals(myCategoryName)) continue;
      list.add(description);
    }
    Collections.sort(list, ATTR_COMPARATOR);
    myListModel.add(list);
    if (selIndex >= 0) {
      myOptionsList.setSelectedIndex(selIndex);
    }
    ListScrollingUtil.ensureSelectionExists(myOptionsList);

    Object selected = myOptionsList.getSelectedValue();
    if (selected instanceof EditorSchemeAttributeDescriptor) {
      myDispatcher.getMulticaster().selectedOptionChanged(selected);
    }
  }

  @Override
  public JPanel getPanel() {
    return this;
  }

  @Override
  public void updateOptionsList() {
    fillOptionsList();
    processListValueChanged();
  }

  @Override
  public Runnable showOption(String attribute) {
    final int index = getAttributeIndex(attribute);
    return index < 0 ? null : new Runnable() {
      @Override
      public void run() {
        ListScrollingUtil.selectItem(myOptionsList, index);
      }
    };
  }

  private int getAttributeIndex(final String option) {
    return ContainerUtil.indexOf(myListModel.getItems(), new Condition<EditorSchemeAttributeDescriptor>() {
      @Override
      public boolean value(EditorSchemeAttributeDescriptor o) {
        return StringUtil.naturalCompare(o.getType(), option) == 0;
      }
    });
  }

  @Override
  public void applyChangesToScheme() {
    Object selectedValue = myOptionsList.getSelectedValue();
    if (selectedValue instanceof ColorAndFontDescription) {
      myOptionsPanel.apply((ColorAndFontDescription)selectedValue, myOptions.getSelectedScheme());
    }
  }

  @Override
  public void selectOption(String attribute) {
    int index = getAttributeIndex(attribute);
    if (index < 0) return;
    ListScrollingUtil.selectItem(myOptionsList, index);
  }

  @Override
  public Set<String> processListOptions() {
    HashSet<String> result = new HashSet<String>();
    EditorSchemeAttributeDescriptor[] descriptions = myOptions.getCurrentDescriptions();

    for (EditorSchemeAttributeDescriptor description : descriptions) {
      if (description.getGroup().equals(myCategoryName)) {
        result.add(description.toString());
      }
    }


    return result;
  }
}
