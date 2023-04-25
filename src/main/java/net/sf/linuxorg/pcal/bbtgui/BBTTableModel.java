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

import javax.swing.table.AbstractTableModel;

import net.sf.linuxorg.pcal.PCalendar;
import net.sf.linuxorg.pcal.engine.BBTSympthomsSetDefinition;
import net.sf.linuxorg.pcal.engine.DateIntsContainer;

/**
 * This class provides the table model for the BBT widget
 * @author Mar'yan Rachynskyy
 *
 */
public class BBTTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private BBTWidget bbtWidget;
	private BBTSympthomsSetDefinition sympthomsContainer = PCalendar.engine.getBBTSympthoms();

	public BBTTableModel(BBTWidget masterBBTWidget) {
		bbtWidget = masterBBTWidget;
	}

	@Override
	public int getColumnCount() {
		return bbtWidget.dateLabels.length;
	}

	@Override
	public int getRowCount() {
		return bbtWidget.verticalLabelsActiveCount;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex>bbtWidget.bbtTemperatureLevelsCount)) {
			int sympthomIndex = rowIndex - bbtWidget.bbtTemperatureLevelsCount - 1;
			DateIntsContainer dateValues = PCalendar.engine.getDateIntegers(bbtWidget.datesList[columnIndex]);
			if(dateValues!=null) {
				Integer valueIndex = dateValues.getIntValue(sympthomIndex);
				if(valueIndex != null) {
					return sympthomsContainer.getSympthomValue(sympthomIndex, valueIndex);
				} else {
					return ""; //$NON-NLS-1$
				}
			} else {
				return ""; //$NON-NLS-1$
			}
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public String getColumnName(int col) {
		if(col<bbtWidget.dateLabels.length) {
			return bbtWidget.dateLabels[col];
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false; 
	}

	@Override
	public void setValueAt(Object value, int row, int col) {

	}

}