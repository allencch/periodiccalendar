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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import net.sf.linuxorg.pcal.common.gui.VerticalCaption;

public class RowHeaderRenderer extends JLabel implements TableCellRenderer {
	private static final long serialVersionUID = 1L;
	
	BBTWidget bbtWidget;

	RowHeaderRenderer(JTable table, BBTWidget masterWidget) {
		JTableHeader header = table.getTableHeader();
		setOpaque(true);
		setBorder(UIManager.getBorder("TableHeader.cellBorder")); //$NON-NLS-1$
		setHorizontalAlignment(CENTER);
		setForeground(header.getForeground());
		setBackground(header.getBackground());
		setFont(header.getFont());
		bbtWidget = masterWidget;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		String stringValue = (value == null) ? "" : value.toString();  //$NON-NLS-1$
		if(row <=bbtWidget.bbtTemperatureLevelsCount) {
			setText(stringValue);
			setIcon(null);
		} else {
			Icon icon = VerticalCaption.getVerticalCaption (this, stringValue, false);
			setText(null);
			setIcon (icon);
			setPreferredSize(new Dimension(icon.getIconWidth(),icon.getIconHeight()));
		}

		return this;
	}
}
