/**
 *  Copyright (C) 2010 by Mar'yan Rachynskyy
 *  mrach@users.sourceforge.net
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.linuxorg.pcal.bbtgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import net.sf.linuxorg.pcal.MainWindow;
import net.sf.linuxorg.pcal.PCalendar;
import net.sf.linuxorg.pcal.common.gui.RangeSlider;
import net.sf.linuxorg.pcal.engine.BBTSympthomsSetDefinition;
import net.sf.linuxorg.pcal.messages.Messages;

/**
 * This class provides BBT Preferences widget.
 * Use the factory methods to get new instances of the widgets.
 * @author Mar'yan Rachynskyy
 *
 */
public class BBTPrefsWidget {
    private static BBTSympthomsSetDefinition sympthomsContainer = PCalendar.engine.getBBTSympthoms();

    // the created components references
    public JTable sympthomsTable;
    public JTable valuesTable;
    public JSplitPane prefsSplitPane;
    public JButton sympthomsUpButton;
    public JButton sympthomsDownButton;
    public JButton valuesAddButton;
    public JButton valuesRemoveButton;
    public JButton valuesUpButton;
    public JButton valuesDownButton;

    private JRadioButton rbCelsius;
    private JRadioButton rbFahrenheit;

    private RangeSlider graphTempRangeSlider;
    private JLabel graphTempRangeMinLabel;
    private JLabel graphTempRangeMinValue;
    private JLabel graphTempRangeMaxLabel;
    private JLabel graphTempRangeMaxValue;
    private boolean isRefreshingSlider = false; // true if slider is being refreshed and no need to run change script 

    // the cached number of the selected sympthom
    public int cachedSympthomID = -1;


    /**
     * The BBT preferences widget factory.
     * @return the new instance of the widget
     */
    public JComponent getBBTPreferences() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints c = new GridBagConstraints();

        JLabel l = new JLabel(Messages.getString("BBTPrefsWidget.0")); //$NON-NLS-1$
        c.insets.bottom = 10;
        c.anchor = GridBagConstraints.WEST;
        mainPanel.add(l, c);

        // the sympthoms pane
        JPanel sympthomsPanel = new JPanel(new BorderLayout());

        Border b = BorderFactory.createEtchedBorder();
        b = BorderFactory.createTitledBorder(b, Messages.getString("BBTPrefsWidget.1")); //$NON-NLS-1$
        sympthomsPanel.setBorder(b);		

        sympthomsTable = new JTable(new BBTSympthomsListModel(this, sympthomsContainer));
        sympthomsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sympthomsTable.setPreferredSize(new Dimension(100, 100));
        sympthomsPanel.add(sympthomsTable, BorderLayout.CENTER);

        ActionListener prefActionListener = getPreferencesActionListener();

        JToolBar sympthomsToolbar =  new JToolBar();
        sympthomsToolbar.setFloatable(false);
        sympthomsToolbar.setOrientation(JToolBar.VERTICAL);
        sympthomsUpButton = MainWindow.createToolbarButton("16x16/up.png", //$NON-NLS-1$ 
                "sympthomUp", Messages.getString("BBTPrefsWidget.2"), //$NON-NLS-1$ //$NON-NLS-2$
                prefActionListener);
        sympthomsToolbar.add(sympthomsUpButton);
        sympthomsDownButton = MainWindow.createToolbarButton("16x16/down.png", //$NON-NLS-1$ 
                "sympthomDown", Messages.getString("BBTPrefsWidget.3"), //$NON-NLS-1$ //$NON-NLS-2$
                prefActionListener);
        sympthomsToolbar.add(sympthomsDownButton);

        sympthomsPanel.add(sympthomsToolbar, BorderLayout.LINE_END);

        //values panel		
        JPanel valuesPanel = new JPanel(new BorderLayout());

        b = BorderFactory.createEtchedBorder();
        b = BorderFactory.createTitledBorder(b, Messages.getString("BBTPrefsWidget.4")); //$NON-NLS-1$
        valuesPanel.setBorder(b);		

        valuesTable = new JTable(new BBTSympthomValuesModel(this, sympthomsContainer));
        valuesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        valuesTable.setTableHeader(null);
        JScrollPane scrollPane = new JScrollPane(valuesTable, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        valuesPanel.add(scrollPane, BorderLayout.CENTER);

        JToolBar valuesToolbar =  new JToolBar();
        valuesToolbar.setFloatable(false);
        valuesToolbar.setOrientation(JToolBar.VERTICAL);
        valuesAddButton = MainWindow.createToolbarButton("16x16/add.png", //$NON-NLS-1$ 
                "valueAdd", Messages.getString("BBTPrefsWidget.5"), //$NON-NLS-1$ //$NON-NLS-2$
                prefActionListener);
        valuesToolbar.add(valuesAddButton);
        valuesRemoveButton = MainWindow.createToolbarButton("16x16/remove.png", //$NON-NLS-1$ 
                "valueRemove", Messages.getString("BBTPrefsWidget.6"), //$NON-NLS-1$ //$NON-NLS-2$
                prefActionListener); 
        valuesToolbar.add(valuesRemoveButton);
        valuesUpButton = MainWindow.createToolbarButton("16x16/up.png", //$NON-NLS-1$
                "valueUp", Messages.getString("BBTPrefsWidget.7"), //$NON-NLS-1$ //$NON-NLS-2$
                prefActionListener);
        valuesToolbar.add(valuesUpButton);
        valuesDownButton = MainWindow.createToolbarButton("16x16/down.png", //$NON-NLS-1$ 
                "valueDown", Messages.getString("BBTPrefsWidget.8"), //$NON-NLS-1$ //$NON-NLS-2$
                prefActionListener);
        valuesToolbar.add(valuesDownButton);

        valuesPanel.add(valuesToolbar, BorderLayout.LINE_END);

        //splitter
        prefsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sympthomsPanel, valuesPanel);
        prefsSplitPane.setBorder(null);
        prefsSplitPane.setPreferredSize(new Dimension(400,200));
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        mainPanel.add(prefsSplitPane, c);

        // set up the listeners
        ListSelectionModel listSelectionModel = sympthomsTable.getSelectionModel();
        listSelectionModel.addListSelectionListener(getSympthomSelectionListener());
        sympthomsTable.setSelectionModel(listSelectionModel);

        ListSelectionModel valuesSelectionModel = valuesTable.getSelectionModel();
        valuesSelectionModel.addListSelectionListener(getValuesSelectionListener());
        valuesTable.setSelectionModel(valuesSelectionModel);

        sympthomsTable.changeSelection(0, 0, false, false);
        valuesTable.changeSelection(0, 0, false, false);

        JPanel pT = new JPanel(new GridBagLayout());

        GridBagConstraints cTemp = new GridBagConstraints();
        cTemp.insets.right = 5;

        l = new JLabel(Messages.getString("BBTPrefsWidget.9")); //$NON-NLS-1$
        pT.add(l, cTemp);

        rbCelsius = new JRadioButton(Messages.getString("BBTPrefsWidget.10")); //$NON-NLS-1$
        rbCelsius.setMnemonic(KeyEvent.VK_E);
        rbCelsius.addActionListener(getScaleActionListener());
        cTemp.gridx = 1;
        pT.add(rbCelsius, cTemp);

        rbFahrenheit = new JRadioButton(Messages.getString("BBTPrefsWidget.11")); //$NON-NLS-1$
        rbFahrenheit.setMnemonic(KeyEvent.VK_F);
        rbFahrenheit.addActionListener(getScaleActionListener());
        cTemp.gridx = 2;
        pT.add(rbFahrenheit, cTemp);

        ButtonGroup group = new ButtonGroup();
        group.add(rbCelsius);
        group.add(rbFahrenheit);

        graphTempRangeMinLabel = new JLabel(Messages.getString("BBTPrefsWidget.19")); //$NON-NLS-1$
        graphTempRangeMinValue = new JLabel("0"); //$NON-NLS-1$
        graphTempRangeMaxLabel = new JLabel(Messages.getString("BBTPrefsWidget.20")); //$NON-NLS-1$
        graphTempRangeMaxValue = new JLabel("0"); //$NON-NLS-1$

        graphTempRangeSlider = new RangeSlider();

        graphTempRangeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(!isRefreshingSlider) {
                    refreshTempScaleLabels();
                    PCalendar.engine.setMinTempScale(graphTempRangeSlider.getValue());
                    PCalendar.engine.setMaxTempScale(graphTempRangeSlider.getUpperValue());
                }
            }
        });        

        cTemp.gridx = 0;
        cTemp.gridy = 1;
        cTemp.anchor = GridBagConstraints.EAST;
        pT.add(graphTempRangeMinLabel, cTemp);
        cTemp.gridy = 2;
        pT.add(graphTempRangeMaxLabel, cTemp);

        cTemp.anchor = GridBagConstraints.WEST;
        cTemp.gridx = 1;
        cTemp.gridy = 1;
        pT.add(graphTempRangeMinValue, cTemp);

        cTemp.gridy = 2;
        pT.add(graphTempRangeMaxValue, cTemp);

        cTemp.gridy = 3;
        cTemp.gridwidth = 2;
        pT.add(graphTempRangeSlider, cTemp);


        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        mainPanel.add(pT, c);

        return mainPanel;
    }

    /**
     * @return a selection listener for the preferences sympthoms list
     */
    private ListSelectionListener getSympthomSelectionListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting()) return;

                //enable/disable the toolbar buttons
                checkPreferencesToolbarActiveness();


                //kick the values table to be refreshed
                if(valuesTable.isEditing()) {
                    valuesTable.getCellEditor().stopCellEditing();
                }
                cachedSympthomID = sympthomsTable.getSelectedRow();
                ((AbstractTableModel) valuesTable.getModel()).fireTableDataChanged();
            }
        };
    }

    /**
     * @return a selection listener for the preferences values list
     */
    private ListSelectionListener getValuesSelectionListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting()) return;

                boolean upEnabled = false;
                boolean downEnabled = false;
                boolean removeEnabled = false;

                if(cachedSympthomID != -1) {

                    //enable/disable the toolbar buttons
                    int selectedRow = valuesTable.getSelectedRow();

                    String[] shownValues = sympthomsContainer.getSympthomValues(cachedSympthomID);

                    if(shownValues != null) { 
                        upEnabled = (selectedRow>0) &&
                                (shownValues[selectedRow]!=null); //$NON-NLS-1$

                        downEnabled = (selectedRow>=0)&&
                                (selectedRow<shownValues.length-1) &&
                                (shownValues[selectedRow+1]!=null); //$NON-NLS-1$

                        removeEnabled = (selectedRow>=0)&&
                                (selectedRow<shownValues.length);
                    }

                }
                valuesUpButton.setEnabled(upEnabled);
                valuesDownButton.setEnabled(downEnabled);
                valuesRemoveButton.setEnabled(removeEnabled);
            }
        };
    }

    private ActionListener getPreferencesActionListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int nextSwapRow = 0; //zero means no sympthoms swap

                int selectedSympthomsRow = sympthomsTable.getSelectedRow();
                int selectedValuesRow = valuesTable.getSelectedRow();

                stopEditing();

                String actionCommand = e.getActionCommand();

                if(actionCommand.equals("sympthomUp")) { //$NON-NLS-1$
                    nextSwapRow = -1;
                } else 
                    if(actionCommand.equals("sympthomDown")) { //$NON-NLS-1$
                        nextSwapRow = 1;
                    }

                if(nextSwapRow != 0) {
                    int newPosition = selectedSympthomsRow+nextSwapRow;
                    PCalendar.engine.swapSympthoms(newPosition, selectedSympthomsRow);

                    ((AbstractTableModel) sympthomsTable.getModel()).fireTableDataChanged();
                    sympthomsTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                    sympthomsTable.changeSelection(newPosition, 0, false, false);
                    return;
                }
                // end of sympthoms

                // values block
                if(actionCommand.equals("valueUp")) { //$NON-NLS-1$
                    nextSwapRow = -1;
                } else 
                    if(actionCommand.equals("valueDown")) { //$NON-NLS-1$
                        nextSwapRow = 1;
                    } else {
                        nextSwapRow = 0;
                    }
                if(nextSwapRow != 0) {
                    int newPosition = selectedValuesRow+nextSwapRow;
                    if((newPosition<0) || (selectedValuesRow<0)) return;

                    PCalendar.engine.swapSympthomValues(cachedSympthomID, newPosition, selectedValuesRow);

                    ((AbstractTableModel) valuesTable.getModel()).fireTableDataChanged();
                    valuesTable.changeSelection(newPosition, 0, false, false);
                    return;
                }

                if(actionCommand.equals("valueAdd")) { //$NON-NLS-1$
                    // add a new line after the currently selected one
                    int newLinePos = selectedValuesRow+1;
                    PCalendar.engine.insertSypmthomValue(cachedSympthomID, newLinePos);
                    // switch the new row to the edit mode
                    ((AbstractTableModel) valuesTable.getModel()).fireTableDataChanged();
                    valuesTable.requestFocus();
                    valuesTable.editCellAt(newLinePos, 0);
                    valuesTable.changeSelection(newLinePos, 0, false, false);
                    return;
                } 

                if(actionCommand.equals("valueRemove")) { //$NON-NLS-1$
                    if(selectedValuesRow<0) return;
                    // Just show a confirmation request here
                    int q_res = JOptionPane.showConfirmDialog(
                            prefsSplitPane.getTopLevelAncestor(),
                            Messages.getString("BBTPrefsWidget.12") + //$NON-NLS-1$
                            Messages.getString("BBTPrefsWidget.13"), //$NON-NLS-1$
                            Messages.getString("BBTPrefsWidget.14"), //$NON-NLS-1$
                            JOptionPane.YES_NO_OPTION);
                    if(q_res==JOptionPane.YES_OPTION) {
                        PCalendar.engine.deleteSypmthomValue(cachedSympthomID, selectedValuesRow);
                        ((AbstractTableModel) valuesTable.getModel()).fireTableDataChanged();
                        if(selectedValuesRow>=valuesTable.getRowCount()) {
                            selectedValuesRow = valuesTable.getRowCount() - 1; 
                        }
                        valuesTable.changeSelection(selectedValuesRow, 0, false, false);
                    }
                } 
            }
        };
    }

    /**
     * Enables/disables the toolbar buttons
     */
    protected void checkPreferencesToolbarActiveness() {
        int selectedRow = sympthomsTable.getSelectedRow();
        if(selectedRow == -1) return;
        String selectedSympthom = sympthomsContainer.getSympthoms()[selectedRow];
        boolean upEnabled = (selectedRow>0) &&
                (selectedSympthom!=null) &&
                (!selectedSympthom.equals("")); //$NON-NLS-1$
        sympthomsUpButton.setEnabled(upEnabled);

        boolean downEnabled = (selectedRow>=0)&&
                (selectedRow<3) &&
                (sympthomsContainer.getSympthoms()[selectedRow+1]!=null) &&
                (!sympthomsContainer.getSympthoms()[selectedRow+1].equals("")); //$NON-NLS-1$     
        sympthomsDownButton.setEnabled(downEnabled);

        boolean addEnabled = (selectedSympthom!=null) &&
                (!selectedSympthom.equals("")); 	//$NON-NLS-1$
        valuesAddButton.setEnabled(addEnabled);
    }

    private ActionListener getScaleActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(PCalendar.engine.isFileBBTCelsiusScale() == rbCelsius.isSelected()) return;
                int q_res = JOptionPane.showConfirmDialog(
                        prefsSplitPane.getTopLevelAncestor(),
                        Messages.getString("BBTPrefsWidget.15") + //$NON-NLS-1$
                        Messages.getString("BBTPrefsWidget.16") + //$NON-NLS-1$
                        Messages.getString("BBTPrefsWidget.17"), //$NON-NLS-1$
                        Messages.getString("BBTPrefsWidget.18"), //$NON-NLS-1$
                        JOptionPane.YES_NO_OPTION);
                if(q_res==JOptionPane.YES_OPTION) {
                    PCalendar.engine.changeBBTScale(rbCelsius.isSelected());
                    refreshBBTScaleWidgets();
                    refreshTempScaleLabels();
                } else {
                    refreshBBTRadioButtons();
                }
            }
        };
    }

    /**
     * This method is called before the dialog is going to be closed
     */
    public void prepareForClose() {
        stopEditing();
    }

    /**
     * Stops editing operations if any  
     */
    private void stopEditing() {
        if(sympthomsTable.isEditing()) {
            sympthomsTable.getCellEditor().stopCellEditing();
        }

        if(valuesTable.isEditing()) {
            valuesTable.getCellEditor().stopCellEditing();
        }
    }

    /**
     * Refresh the state of all widgets
     */
    public void refreshWidgets() {
        refreshBBTRadioButtons();
        refreshBBTScaleWidgets();
        refreshTempScaleLabels();
        sympthomsTable.changeSelection(0, 0, false, false);
    }

    /**
     * Refresh temperature slider and labels
     */
    private void refreshBBTScaleWidgets() {
        isRefreshingSlider = true;
        if(PCalendar.engine.isFileBBTCelsiusScale()) {
            rbCelsius.setSelected(true);
            graphTempRangeSlider.setMinimum(BBTWidget.BBT_TEMPERATURE_SCALE_MIN_C);
            graphTempRangeSlider.setMaximum(BBTWidget.BBT_TEMPERATURE_SCALE_MAX_C);
        } else {
            rbFahrenheit.setSelected(true);
            graphTempRangeSlider.setMinimum(BBTWidget.BBT_TEMPERATURE_SCALE_MIN_F);
            graphTempRangeSlider.setMaximum(BBTWidget.BBT_TEMPERATURE_SCALE_MAX_F);
        }
        graphTempRangeSlider.setUpperValue(PCalendar.engine.getMaxTempScale());
        graphTempRangeSlider.setValue(PCalendar.engine.getMinTempScale());
        isRefreshingSlider = false;
    }

    /**
     * Refresh temperature scale radio buttons
     */
    private void refreshBBTRadioButtons() {
        if(PCalendar.engine.isFileBBTCelsiusScale()) {
            rbCelsius.setSelected(true);
        } else {
            rbFahrenheit.setSelected(true);
        }
    }

    /**
     * Repaints the min and max values basing on the current range slider selection
     */
    private void refreshTempScaleLabels() {
        graphTempRangeMinValue.setText(String.valueOf(graphTempRangeSlider.getValue()/10.0));
        graphTempRangeMaxValue.setText(String.valueOf(graphTempRangeSlider.getUpperValue()/10.0));
    }

}
