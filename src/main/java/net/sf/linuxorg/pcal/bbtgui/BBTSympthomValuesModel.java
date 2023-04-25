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

import javax.swing.table.AbstractTableModel;

import net.sf.linuxorg.pcal.engine.BBTSympthomsSetDefinition;

public class BBTSympthomValuesModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private BBTSympthomsSetDefinition sympthomsContainer = null;
	private BBTPrefsWidget bbtWidgets = null;

	BBTSympthomValuesModel(BBTPrefsWidget masterBBTWidgets, BBTSympthomsSetDefinition masterSympthomsContainer) {
		sympthomsContainer = masterSympthomsContainer;
		bbtWidgets = masterBBTWidgets;
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public int getRowCount() {
		if(bbtWidgets.cachedSympthomID==-1) {
			return 0;
		} else {
			return sympthomsContainer.getSympthomValuesCount(bbtWidgets.cachedSympthomID);
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return sympthomsContainer.getSympthomValue(bbtWidgets.cachedSympthomID, rowIndex);
	}
	
	@Override
	public boolean isCellEditable(int row, int col)
	{ return true; }
	
	@Override
	public void setValueAt(Object value, int row, int col) {
		String stringValue = value.toString();
		//check if the value was really changed
		if(stringValue.equals(sympthomsContainer.getSympthomValue(bbtWidgets.cachedSympthomID, row))) return;
		
		// empty values are allowed here
		sympthomsContainer.setSypmthomValue(bbtWidgets.cachedSympthomID, 
				row, stringValue);
		fireTableDataChanged();
	}

}
