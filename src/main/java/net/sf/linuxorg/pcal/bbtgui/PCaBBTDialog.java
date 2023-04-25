/**
 *  Copyright (C) 2009-2010 by Mar'yan Rachynskyy
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.linuxorg.pcal.MainWindow;
import net.sf.linuxorg.pcal.PCalendar;
import net.sf.linuxorg.pcal.messages.Messages;

/**
 * @author Mar'yan Rachynskyy
 * 
 */
public class PCaBBTDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	private static final String BBTWINDOW = "BBTWindow"; //$NON-NLS-1$
	private static final String SPLITTER_POS = "splitterPos";   //$NON-NLS-1$

	private final JLabel legendLabels[] = {new JLabel(Messages.getString("MainWindow.0")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.1")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.2")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.3")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.4")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.5")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.6")), //$NON-NLS-1$
			new JLabel(Messages.getString("MainWindow.13"))}; //$NON-NLS-1$
	private JTabbedPane tabbedPane;

	private BBTWidget bbtWidget = null;
	private BBTPrefsWidget bbtPrefsWidget = null;

	/**
	 * Create the dialog with initial layout
	 */
	public PCaBBTDialog() {
		super(PCalendar.mainWindow.getFrame(), Messages.getString("PCaBBTDialog.0"), true);  //$NON-NLS-1$

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				actionPerformed(null);
			}
		});

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// OK Button
		JButton b = new JButton(
				Messages.getString("PCaBBTDialog.1"), MainWindow.createResoruceIcon("16x16/ok.png"));   //$NON-NLS-1$//$NON-NLS-2$
		b.addActionListener(this);
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;		
		c.insets.top = 5;
		c.insets.right = 5;
		c.insets.bottom = 5;
		add(b, c);
		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(b);

		tabbedPane = new JTabbedPane();
				
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.insets.right = 0;

		add(tabbedPane, c);
		
		// the colors legend
		JPanel p = new JPanel(new GridLayout(2, 4, 5, 1));
		for(int i=0; i<legendLabels.length; i++) {
			legendLabels[i].setOpaque(true);
			p.add(legendLabels[i]);
		}
		
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.insets.left = 5;
		c.insets.top = 2;
		c.insets.right = 10;
		c.insets.bottom = 2;
		add(p, c);

		bbtPrefsWidget = new BBTPrefsWidget();
		bbtWidget = new BBTWidget();
		
		tabbedPane.addTab(Messages.getString("PCaBBTDialog.2"), bbtWidget.getBBTWidget()); //$NON-NLS-1$
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_D);
		tabbedPane.addTab(Messages.getString("PCaBBTDialog.3"), bbtPrefsWidget.getBBTPreferences()); //$NON-NLS-1$
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_P);
		
		tabbedPane.addChangeListener(getTabChangeListener());

		pack();

		// Restore the frame position and size
		final Rectangle DEFAULT_BOUNDS = new Rectangle(200, 200, 380, 450);		
		Rectangle bounds = MainWindow.loadFrameBounds(BBTWINDOW, DEFAULT_BOUNDS); 
		setBounds(bounds);

		// load the preferences splitter position
		Preferences windowPrefsNode = PCalendar.settings.node(BBTWINDOW); //$NON-NLS-1$
		bbtPrefsWidget.prefsSplitPane.setDividerLocation(windowPrefsNode.getInt(SPLITTER_POS, 150)); //$NON-NLS-1$

	}

	private ChangeListener getTabChangeListener() {		
		return new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(tabbedPane.getSelectedIndex() == 0) {
					bbtWidget.refreshWidgets(bbtWidget.getBbtStartDate());
				}
			}			
		};
	}

	/**
	 * Show the dialog
	 */
	public void showDialog() {
		//reload the legend colors
		for(int i=0; i<legendLabels.length; i++) {
			legendLabels[i].setForeground(PCalendar.mainWindow.legendFGColors[i]);
			legendLabels[i].setBackground(PCalendar.mainWindow.legendBGColors[i]);
		}		
		
		bbtWidget.refreshWidgets(PCalendar.mainWindow.getCurrentCalendarStartDate());
		bbtPrefsWidget.refreshWidgets();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent arg0) {
		// save the dialog size and position
		final String prefKey = BBTWINDOW; //$NON-NLS-1$
		MainWindow.saveFrameBounds(prefKey, getBounds());

		//save the preferences splitter position here as well
		Preferences windowPrefsNode = PCalendar.settings.node(BBTWINDOW); //$NON-NLS-1$
		windowPrefsNode.putInt(SPLITTER_POS, bbtPrefsWidget.prefsSplitPane.getDividerLocation());
		
		//notify the tabs about the window going to be close
		bbtWidget.prepareForClose();
		bbtPrefsWidget.prepareForClose();
		
		PCalendar.mainWindow.refreshAll(false);
		
		setVisible(false);
	}
}
