/**
 *  Copyright (C) 2009-2010 by Mar'yan Rachynsky
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

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import net.sf.linuxorg.pcal.PCalendar;
import net.sf.linuxorg.pcal.engine.BBTSympthomsSetDefinition;
import net.sf.linuxorg.pcal.engine.DateIntsContainer;
import net.sf.linuxorg.pcal.messages.Messages;

public class BBTSympthomsListModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private BBTSympthomsSetDefinition sympthomsDefinition = null;
	private BBTPrefsWidget bbtWidgets = null;

	BBTSympthomsListModel(BBTPrefsWidget masterBBTWidgets, BBTSympthomsSetDefinition masterSympthomsContainer) {
		sympthomsDefinition = masterSympthomsContainer;
		bbtWidgets = masterBBTWidgets;
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public int getRowCount() {
		return 4;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		String[] sympthoms = sympthomsDefinition.getSympthoms();

		if(sympthoms[rowIndex] == null) {
			return ""; //$NON-NLS-1$
		} else {
			return sympthoms[rowIndex];
		}
	}

	@Override
	public boolean isCellEditable(int row, int col)	{ 
		// editable rows are 0 - always, other - if there is no row with empty value above
		if(row==0) {
			return true;
		} else {			
			String prevSympthomName = sympthomsDefinition.getSympthomName(row -1 );
			return (prevSympthomName == null)?false:
				!sympthomsDefinition.getSympthomName(row -1 ).equals(""); //$NON-NLS-1$
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		String stringValue = value.toString();

		//check if the value was really changed
		if(stringValue.equals(sympthomsDefinition.getSympthomName(row))) return;		

		// empty string means we need to delete the sympthom
		if(stringValue.trim().equals("")) { //$NON-NLS-1$
			// check if there are any observations recorded for this sympthom
			boolean anyValuesRecorded = false;
			for(DateIntsContainer dateInts: PCalendar.engine.getAllDateIntegers().values()) {
				if(dateInts.getIntValue(row)!=null) {
					anyValuesRecorded = true;
					break;
				}
			}
			if(anyValuesRecorded) {
				int q_res = JOptionPane.showConfirmDialog(
						bbtWidgets.prefsSplitPane.getTopLevelAncestor(),
						Messages.getString("BBTSympthomsListModel.2"), //$NON-NLS-1$
						Messages.getString("BBTSympthomsListModel.3"), //$NON-NLS-1$
						JOptionPane.YES_NO_OPTION);
				if(q_res==JOptionPane.YES_OPTION) {
					// go through all date-ints and drop the deleted sympthom. All next sympthoms are moved upward
					for(DateIntsContainer dateInts: PCalendar.engine.getAllDateIntegers().values()) {
						for(int i=row;i<DateIntsContainer.getIntsCount()-1; i++) {
							dateInts.setIntValue(row, dateInts.getIntValue(row+1));
						}
						// anyway the last one will be null in any case
						dateInts.setIntValue(DateIntsContainer.getIntsCount()-1, null);
					}
					sympthomsDefinition.deleteSympthom(row);
				}
			} else {
				sympthomsDefinition.deleteSympthom(row);
			}
			fireTableDataChanged();
			bbtWidgets.sympthomsTable.getSelectionModel().setSelectionInterval(row, row);
		} else {
			sympthomsDefinition.changeSympthomName(row, value.toString());
			fireTableCellUpdated(row, col);
		}		

		bbtWidgets.checkPreferencesToolbarActiveness();
	}

}
