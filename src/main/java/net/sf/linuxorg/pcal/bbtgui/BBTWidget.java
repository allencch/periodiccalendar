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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.sf.linuxorg.pcal.MainWindow;
import net.sf.linuxorg.pcal.PCalDayInfo;
import net.sf.linuxorg.pcal.PCalendar;
import net.sf.linuxorg.pcal.common.gui.VerticalCaption;
import net.sf.linuxorg.pcal.engine.BBTSympthomsSetDefinition;
import net.sf.linuxorg.pcal.messages.Messages;

/**
 * This class provides BBT Table widget with contains the BBT table and the supplementing components.
 * Use the factory methods to get new instances of the widgets.
 * @author Mar'yan Rachynskyy
 */

public class BBTWidget {
    private static final String CLEAR = "<CLEAR>"; //$NON-NLS-1$
    public static final int BBT_TEMPERATURE_SCALE_MIN_C = 340; // minimum possible scale temp in C
    public static final int BBT_TEMPERATURE_SCALE_MIN_F = 932; // minimum possible scale temp in F
    public static final int BBT_TEMPERATURE_SCALE_MAX_C = 390; // minimum possible scale temp in C
    public static final int BBT_TEMPERATURE_SCALE_MAX_F = 1022; // minimum possible scale temp in F

    private Date bbtStartDate = new Date();

    private JTable bbtTable;
    private BBTTableModel bbtTableModel;
    private JTable bbtRowHeader;
    private JScrollPane bbtScrollPane;
    private JLabel currentMonthLabel;
    private JPopupMenu bbtPopupMenu;
    private JPopupMenu sympthomsPopupMenu;
    private Action actionAddOvulation, actionDeleteRecord;

    private ActionListener sympthomsMenuActionListener;
    private Point popupActivatedCell = null;

    protected int bbtTemperatureLevelsCount = 0;
    private String [] verticalLabels;
    protected int verticalLabelsActiveCount = 0;
    private int genericRowHeight = 0;
    private int[] sympthomsRowHeights = new int[BBTSympthomsSetDefinition.BBT_SYMPTHOMS_COUNT];

    protected Date[] datesList = new Date[] {};
    protected String[] dateLabels = new String[] {};
    protected int[] bbtPointRow = new int[] {};
    private Color[] daysBGColors = new Color[] {};
    private Color[] daysFGColors = new Color[] {};
    private boolean[] isOvulationFlag = new boolean[] {};

    /**
     * The main BBT Grid widget factory
     * @param startDate the start date for the calendar. If null - the today date will be 
     * centered.
     * @return the new instance of the widget
     */
    public JComponent getBBTWidget() {
        // the observations table

        bbtTableModel = new BBTTableModel(this);
        bbtTable = new JTable(bbtTableModel) {
            private static final long serialVersionUID = 1L;

            // delegate the painting stuff to the external function
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintBBTGraph(g);
            }
        };		
        bbtTable.setPreferredScrollableViewportSize(new Dimension(400, 70));
        bbtTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        bbtTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bbtTable.getTableHeader().setResizingAllowed(false);
        bbtTable.getTableHeader().setReorderingAllowed(false);
        bbtTable.setRowSelectionAllowed(false);
        bbtTable.setColumnSelectionAllowed(false);
        bbtTable.setColumnSelectionAllowed(true);

        bbtTable.addKeyListener(getBBTKeyListener());
        bbtTable.addMouseListener(getBBTMouseListener());
        bbtTable.addMouseMotionListener(getBBTMouseMoutionListener());

        bbtTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting()) return;
                bbtTable.repaint();
            }
        });

        genericRowHeight = bbtTable.getRowHeight();

        resetTableColumns();
        bbtTable.setDefaultRenderer(Object.class, getBBTTableCellRenderer());

        bbtRowHeader = new JTable(getBBTRowHeadersModel());	

        //Calculate row header width basing on the current font
        FontMetrics fm = PCalendar.mainWindow.getGraphics().getFontMetrics();
        int rowHeaderWidth = fm.stringWidth(Messages.getString("BBTWidget.0")) + 8; //$NON-NLS-1$
        Dimension d = bbtRowHeader.getPreferredScrollableViewportSize();
        d.width = rowHeaderWidth;
        bbtRowHeader.setPreferredScrollableViewportSize(d);
        bbtRowHeader.setIntercellSpacing(new Dimension(0, 0));

        //bbtRowHeader.getColumnModel().getColumn(0).setPreferredWidth(); // the widest string
        bbtRowHeader.setDefaultRenderer(Object.class, new RowHeaderRenderer(bbtTable, this));

        bbtScrollPane = new JScrollPane(bbtTable);
        bbtScrollPane.setRowHeaderView(bbtRowHeader);
        bbtScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //listen to the table resize
        bbtScrollPane.addComponentListener(
                new ComponentAdapter() {
                    public void componentResized(ComponentEvent e) {
                        int bbtDaysCount = bbtScrollPane.getViewportBorderBounds().width / genericRowHeight;
                        if(dateLabels.length != bbtDaysCount) {
                            generateHorizontalLabels(bbtDaysCount);
                            resizeRowHeights();
                        }
                    }
                });

        actionAddOvulation = new ActionAddOvulation();
        actionDeleteRecord = new ActionDeleteRecord();

        bbtPopupMenu = new JPopupMenu();
        bbtPopupMenu.add(actionAddOvulation);
        bbtPopupMenu.add(actionDeleteRecord);
        sympthomsPopupMenu = new JPopupMenu();

        sympthomsMenuActionListener = getSympthomsMenuActionListener();

        ActionListener bbtActionListener = getBBTActionListener();

        JToolBar bbtToolBar = new JToolBar();
        bbtToolBar.setFloatable(false);
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/first2.png", //$NON-NLS-1$ 
                "prevYear", Messages.getString("BBTWidget.1"),	//$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/first.png", //$NON-NLS-1$ 
                "prevMonth", Messages.getString("BBTWidget.2"), //$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/backward.png", //$NON-NLS-1$ 
                "prevDay", Messages.getString("BBTWidget.3"), //$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.addSeparator();
        bbtToolBar.addSeparator();
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/forward.png",	//$NON-NLS-1$ 
                "nextDay", Messages.getString("BBTWidget.4"),	//$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/last.png", 	//$NON-NLS-1$ 
                "nextMonth", Messages.getString("BBTWidget.5"),	//$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/last2.png",	//$NON-NLS-1$ 
                "nextYear", Messages.getString("BBTWidget.6"),	//$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.addSeparator();
        bbtToolBar.add(MainWindow.createToolbarButton("16x16/jump-to.png",	//$NON-NLS-1$ 
                "centerToday", Messages.getString("BBTWidget.7"),	//$NON-NLS-1$ //$NON-NLS-2$
                bbtActionListener));
        bbtToolBar.addSeparator();

        currentMonthLabel = new JLabel();
        bbtToolBar.add(currentMonthLabel);

        JPanel bbtPanel = new JPanel(new BorderLayout());
        bbtPanel.add(bbtScrollPane, BorderLayout.CENTER);
        bbtPanel.add(bbtToolBar, BorderLayout.PAGE_START);

        return bbtPanel;
    }

    /**
     * Rebuilds the table columns data
     */
    private void resetTableColumns() {
        TableColumnModel columnModel = bbtTable.getColumnModel();
        for(int i =0;i<bbtTable.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i); 
            column.setPreferredWidth(genericRowHeight);
            column.setHeaderRenderer(getVerticalHeaderCellRenderer());
        }
    }

    /**
     * @return the bbt row header model
     */
    private TableModel getBBTRowHeadersModel() {
        TableModel tm = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public int getRowCount() {
                return verticalLabelsActiveCount;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return verticalLabels[rowIndex];
            }
        };
        return tm;
    }

    /**
     * Fills the temperature scale and the sypmthoms list data
     */
    private void generateVerticalLabels() {

        verticalLabelsActiveCount = verticalLabels.length;

        Double d = PCalendar.engine.getMinTempScale() / 10.0;

        if(PCalendar.engine.isFileBBTCelsiusScale()) {
            // generate the scale in Celsius
            for(int i=bbtTemperatureLevelsCount-1; i>=0; i--) {
                verticalLabels[i] = String.format("%1$4.1f", d);	//$NON-NLS-1$
                d += 0.1;
            }
        } else {
            for(int i=bbtTemperatureLevelsCount-1; i>=0; i--) {
                verticalLabels[i] = String.format("%1$4.1f", d);	//$NON-NLS-1$
                d += 0.1;
            }
        }
        verticalLabels[bbtTemperatureLevelsCount] = ""; //$NON-NLS-1$

        FontMetrics fm = PCalendar.mainWindow.getGraphics().getFontMetrics();

        for(int i=bbtTemperatureLevelsCount + 1; i<verticalLabels.length; i++) {
            int sympthomIndex = i - bbtTemperatureLevelsCount -1;
            verticalLabels[i] = PCalendar.engine.getBBTSympthoms().getSympthomName(sympthomIndex);

            if((verticalLabels[i]!=null) && (!verticalLabels[i].trim().equals(""))) {  //$NON-NLS-1$
                //calculate the max of the sympthom label and values widths
                int maxWidth = fm.stringWidth(verticalLabels[i]);
                for(String sympthomValue : PCalendar.engine.getBBTSympthoms().getSympthomValues(sympthomIndex)) {
                    int iwidth = fm.stringWidth(sympthomValue);
                    if(maxWidth<iwidth) {
                        maxWidth = iwidth;
                    }
                }
                //add a margin for the height
                maxWidth += 4;
                sympthomsRowHeights[sympthomIndex] = maxWidth;
            } else {
                verticalLabelsActiveCount = i;
                break;
            }
        }
        // request the table row header to rebuild itself
        bbtRowHeader.tableChanged(null);
    }

    private void resizeRowHeights() {
        for(int i=0; i<verticalLabelsActiveCount; i++) {
            if(i<(bbtTemperatureLevelsCount + 1)) {
                bbtTable.setRowHeight(i, genericRowHeight);
                bbtRowHeader.setRowHeight(i, genericRowHeight);
            } else {
                int sympthomIndex = i - bbtTemperatureLevelsCount -1;
                bbtTable.setRowHeight(i, sympthomsRowHeights[sympthomIndex]);
                bbtRowHeader.setRowHeight(i, sympthomsRowHeights[sympthomIndex]);
            }
        }
    }

    /**
     * @param bbtDaysCount - if 0 - the days count is recalculated, otherwise, the table is set to fit specified days count
     */
    private void generateHorizontalLabels(int bbtDaysCount) {
        // determine how many days can be displayed in the current widget
        if(bbtDaysCount == 0) {
            bbtDaysCount = bbtScrollPane.getViewportBorderBounds().width / genericRowHeight;
        }

        datesList = new Date[bbtDaysCount];
        dateLabels = new String[bbtDaysCount];
        bbtPointRow = new int[bbtDaysCount];
        daysBGColors = new Color[bbtDaysCount];
        daysFGColors = new Color[bbtDaysCount];
        isOvulationFlag = new boolean[bbtDaysCount];

        if(bbtDaysCount>0) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(bbtStartDate);

            int lastRecordedMonthIndex = calendar.get(Calendar.MONTH); 
            String monthsList = MainWindow.monthNames[lastRecordedMonthIndex];

            String yearString = String.valueOf(calendar.get(Calendar.YEAR));

            for(int i=0; i<bbtDaysCount; i++) {
                datesList[i] = calendar.getTime();
                dateLabels[i] = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

                PCalDayInfo dayInfo = PCalendar.engine.getDayInfo(calendar.getTime());
                Color[] dayColors = PCalendar.mainWindow.getDayColors(dayInfo);
                daysBGColors[i] = dayColors[0];
                daysFGColors[i] = dayColors[1];
                isOvulationFlag[i] = (dayInfo == null)?false:dayInfo.ovulation;

                // get the BBT records as well
                int temperature = PCalendar.engine.getBBT(calendar.getTime());
                if(temperature>0) {
                    bbtPointRow[i] = PCalendar.engine.getMinTempScale() + bbtTemperatureLevelsCount - temperature - 1;
                    if(bbtPointRow[i] > bbtTemperatureLevelsCount) bbtPointRow[i] = -1; 
                } else {
                    bbtPointRow[i] = -1;
                }

                int currentDateMonthIndex = calendar.get(Calendar.MONTH);
                if(lastRecordedMonthIndex != currentDateMonthIndex) {
                    monthsList+= Messages.getString("BBTWidget.8") + MainWindow.monthNames[currentDateMonthIndex]; //$NON-NLS-1$
                    lastRecordedMonthIndex = currentDateMonthIndex;
                }
                calendar.add(Calendar.DATE, 1);
            }
            currentMonthLabel.setText(yearString+Messages.getString("BBTWidget.9")+monthsList); //$NON-NLS-1$
            bbtTableModel.fireTableStructureChanged();
            resetTableColumns();
        }
    }


    private DefaultTableCellRenderer getVerticalHeaderCellRenderer() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getTableCellRendererComponent (JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                JLabel label = new JLabel ();
                Icon icon = VerticalCaption.getVerticalCaption (label, value.toString (), false);
                label.setIcon (icon);
                label.setHorizontalAlignment (JLabel.CENTER);
                label.setBorder (new BevelBorder (BevelBorder.RAISED));
                return label;
            }

        };
    }

    private DefaultTableCellRenderer getBBTTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getTableCellRendererComponent (JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                if(row < bbtTemperatureLevelsCount) {
                    Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if((bbtTable.getSelectedRow() == row) || (bbtTable.getSelectedColumn() == column)) {
                        renderer.setBackground(UIManager.getColor("Table.selectionBackground")); //$NON-NLS-1$
                    } else {
                        renderer.setBackground(daysBGColors[column]);
                        renderer.setForeground(daysFGColors[column]);
                    }
                    return renderer;
                } else				
                    if(row == bbtTemperatureLevelsCount) {
                        Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if((bbtTable.getSelectedRow() == row) || (bbtTable.getSelectedColumn() == column)) {
                            renderer.setBackground(UIManager.getColor("Table.selectionBackground")); //$NON-NLS-1$
                        } else {
                            renderer.setBackground(UIManager.getColor("TableHeader.background")); //$NON-NLS-1$
                        }
                        return renderer;
                    } else {
                        // vertical renderers for sympthoms
                        JLabel label = new JLabel ();
                        label.setOpaque(true);
                        Icon icon = VerticalCaption.getVerticalCaption (label, value.toString (), false);
                        label.setIcon (icon);
                        label.setText(null);
                        label.setHorizontalAlignment (JLabel.CENTER);

                        if(hasFocus) {
                            Border border = null;
                            if (isSelected) {
                                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder"); //$NON-NLS-1$
                            }
                            if (border == null) {
                                border = UIManager.getBorder("Table.focusCellHighlightBorder"); //$NON-NLS-1$
                            }
                            label.setBorder(border);
                        }
                        if(icon.getIconHeight() > table.getRowHeight(row)) {
                            table.setRowHeight(row, icon.getIconHeight());
                        }
                        if((bbtTable.getSelectedRow() == row) || (bbtTable.getSelectedColumn() == column)) {
                            label.setBackground(UIManager.getColor("Table.selectionBackground")); //$NON-NLS-1$
                        }
                        return label;
                    }
            }
        };
    }

    private ActionListener getBBTActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String actionCommand = e.getActionCommand();
                GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTime(bbtStartDate);
                if(actionCommand.equals("prevYear")) { //$NON-NLS-1$
                    calendar.add(Calendar.YEAR, -1);
                } else 
                    if(actionCommand.equals("prevMonth")) { //$NON-NLS-1$
                        calendar.add(Calendar.MONTH, -1);
                    } else 
                        if(actionCommand.equals("prevDay")) { //$NON-NLS-1$
                            calendar.add(Calendar.DATE, -1);
                        } else 
                            if(actionCommand.equals("nextDay")) { //$NON-NLS-1$
                                calendar.add(Calendar.DATE, 1);
                            }	else 
                                if(actionCommand.equals("nextMonth")) { //$NON-NLS-1$
                                    calendar.add(Calendar.MONTH, 1);
                                } else 
                                    if(actionCommand.equals("nextYear")) { //$NON-NLS-1$
                                        calendar.add(Calendar.YEAR, 1);
                                    } else 
                                        if(actionCommand.equals("centerToday")) { //$NON-NLS-1$
                                            Calendar temp_calendar1 = new GregorianCalendar();
                                            temp_calendar1.setTime(new Date());
                                            calendar.set(temp_calendar1.get(Calendar.YEAR), temp_calendar1.get(Calendar.MONTH), temp_calendar1.get(Calendar.DAY_OF_MONTH));
                                            calendar.add(Calendar.DATE, - dateLabels.length/2);
                                        }
                bbtStartDate = calendar.getTime();
                generateHorizontalLabels(dateLabels.length);
                resizeRowHeights();
            }
        };
    }

    /**
     * Make sure the dialog is up to date according to the current preferences and engine state
     */
    public void refreshWidgets(Date startDate) {
        if(startDate == null) {
            // get rid of hours:minutes:seconds
            Calendar temp_calendar1 = new GregorianCalendar();
            temp_calendar1.setTime(new Date());
            Calendar temp_calendar2 = new GregorianCalendar(temp_calendar1.get(Calendar.YEAR), temp_calendar1.get(Calendar.MONTH), temp_calendar1.get(Calendar.DAY_OF_MONTH));
            startDate = temp_calendar2.getTime();
        }
        bbtStartDate = startDate;

        bbtTemperatureLevelsCount = PCalendar.engine.getBBTTemperatureLevelsCount();
        verticalLabels = new String[bbtTemperatureLevelsCount + BBTSympthomsSetDefinition.BBT_SYMPTHOMS_COUNT + 1]; 

        generateVerticalLabels();
        generateHorizontalLabels(0);
        resizeRowHeights();

    }

    /**
     * This method is called before the dialog is going to be closed
     */
    public void prepareForClose() {
        if(bbtTable.isEditing()) {
            bbtTable.getCellEditor().stopCellEditing();
        }
    }

    private MouseListener getBBTMouseListener() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() > 1) {
                    int selectedRow = bbtTable.getSelectedRow(); 
                    if(selectedRow < 0) return; 
                    if(selectedRow < bbtTemperatureLevelsCount) {
                        int selectedColumn = bbtTable.getSelectedColumn();
                        toggleBBT(selectedRow, selectedColumn);
                    } else
                        if(selectedRow > bbtTemperatureLevelsCount) {
                            showSympthomsPopup(e.getPoint());
                        }
                }				
            }
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {

                    int popupRow = bbtTable.rowAtPoint(e.getPoint());

                    if(popupRow < 0) return;

                    if(popupRow <= bbtTemperatureLevelsCount) {
                        // show the BBT Graph menu
                        int selectedColumn = bbtTable.getSelectedColumn();
                        boolean enabledOvulation = true;
                        if(selectedColumn > 0) {
                            if(isOvulationFlag[selectedColumn]) {
                                enabledOvulation = false;
                            }
                        }
                        actionAddOvulation.setEnabled(enabledOvulation);
                        actionDeleteRecord.setEnabled(!enabledOvulation);

                        bbtPopupMenu.show(e.getComponent(),
                                e.getX(), e.getY());
                    } else {
                        // show the Sympthoms menu
                        showSympthomsPopup(e.getPoint());
                    }
                }
            }
        };
    }

    /**
     * @param point the point where popup should be activated
     */
    private void showSympthomsPopup(Point point) {
        sympthomsPopupMenu.removeAll();

        popupActivatedCell = new Point(bbtTable.columnAtPoint(point), bbtTable.rowAtPoint(point));

        JMenuItem clearMenuItem = sympthomsPopupMenu.add(Messages.getString("BBTWidget.10"));				 //$NON-NLS-1$
        clearMenuItem.addActionListener(sympthomsMenuActionListener);
        clearMenuItem.setActionCommand(CLEAR);
        sympthomsPopupMenu.addSeparator();

        int sympthomIndex = popupActivatedCell.y - bbtTemperatureLevelsCount - 1;				 
        String[] values = PCalendar.engine.getBBTSympthoms().getSympthomValues(sympthomIndex);
        for(int i = 0; i<values.length; i++) {
            JMenuItem menuItem = sympthomsPopupMenu.add(values[i]);
            menuItem.setActionCommand(""+i); //$NON-NLS-1$
            menuItem.addActionListener(sympthomsMenuActionListener);
        }
        sympthomsPopupMenu.show(bbtTable, point.x, point.y);
    }

    private KeyListener getBBTKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int selectedRow = bbtTable.getSelectedRow(); 
                    if(selectedRow >= 0) {
                        if(selectedRow < bbtTemperatureLevelsCount) {
                            int selectedColumn = bbtTable.getSelectedColumn();
                            toggleBBT(selectedRow, selectedColumn);
                        } else
                            if(selectedRow > bbtTemperatureLevelsCount) {
                                int selectedColumn = bbtTable.getSelectedColumn();
                                if(selectedColumn > 0) {
                                    showSympthomsPopup(bbtTable.getCellRect(selectedRow, selectedColumn, true).getLocation());
                                }
                            }
                    }
                }				
            }
        };
    }

    private MouseMotionListener getBBTMouseMoutionListener() {
        return new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if(bbtPopupMenu.isShowing()) return;
                Point mouseCoordinates = e.getPoint();
                int selectedRow = bbtTable.rowAtPoint(mouseCoordinates);
                int selectedColumn = bbtTable.columnAtPoint(mouseCoordinates);
                if((selectedRow>=0) && (selectedColumn>=0)) {
                    bbtTable.changeSelection(selectedRow, selectedColumn, false, false);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        };
    }

    private void toggleBBT(int selectedRow, int selectedColumn) {
        if(selectedColumn<0) return;

        Date selectedDate = datesList[selectedColumn];

        int temperature = PCalendar.engine.getMinTempScale() + bbtTemperatureLevelsCount - selectedRow - 1;

        int oldTemperature = PCalendar.engine.getBBT(selectedDate);

        if(oldTemperature == temperature) {
            PCalendar.engine.removeBBT(selectedDate);
            bbtPointRow[selectedColumn] = -1;
        } else {
            PCalendar.engine.addBBT(selectedDate, temperature);
            bbtPointRow[selectedColumn] = selectedRow;
        }
        bbtTable.repaint();
    }

    private ActionListener getSympthomsMenuActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(popupActivatedCell == null) return;
                if(popupActivatedCell.y <= bbtTemperatureLevelsCount) return;

                int sympthomIndex = popupActivatedCell.y - bbtTemperatureLevelsCount - 1;

                int newValue = -1;
                if(!e.getActionCommand().equals(CLEAR)) {
                    try {
                        newValue = Integer.parseInt(e.getActionCommand());
                    } catch (NumberFormatException ex) {
                        return;
                    }
                }
                PCalendar.engine.setDateInteger(datesList[popupActivatedCell.x], sympthomIndex, newValue);
                bbtTable.repaint();
            }
        };
    }

    private void paintBBTGraph(Graphics g) {
        final int POINT_RADIUS = 6; 
        final int POINT_D = POINT_RADIUS*2;

        // paintint he BBT Graph over the table
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));

        int prevX = -1;
        int prevY = -1;

        for(int i=0; i<bbtPointRow.length; i++) {
            if(bbtPointRow[i]>=0) {
                //paint the point
                Rectangle currentCell = bbtTable.getCellRect(bbtPointRow[i], i, true);
                int currentX = currentCell.x + currentCell.width/2;
                int currentY = currentCell.y + currentCell.height/2;
                g2d.fillOval(currentX - POINT_RADIUS, currentY - POINT_RADIUS, POINT_D,  POINT_D);				

                if(prevX>0) {
                    // connect the point to the previous one
                    g2d.drawLine(prevX, prevY, currentX, currentY);
                }
                prevX = currentX;
                prevY = currentY;
            } else {
                prevX = -1;
                prevY = -1;
            }
        }

    }

    public class ActionAddOvulation extends AbstractAction {
        private static final long serialVersionUID = 1L;
        public ActionAddOvulation() {
            super(Messages.getString("MainWindow.131"), MainWindow.createResoruceIcon("16x16/ovulation.png"));   //$NON-NLS-1$ //$NON-NLS-2$
            putValue(SHORT_DESCRIPTION, Messages.getString("MainWindow.132"));  //$NON-NLS-1$
            putValue(MNEMONIC_KEY, Messages.getMnemonic("MainWindow.131.Mnemonic")); //$NON-NLS-1$
        }
        public void actionPerformed(ActionEvent e) {
            int selectedColumn = bbtTable.getSelectedColumn();

            if(selectedColumn < 0) return;

            PCalendar.engine.addOvulationDate(datesList[selectedColumn]);

            //changing the records might impact the colors
            refreshColors();
            bbtTable.repaint();
        }
    }

    public class ActionDeleteRecord extends AbstractAction {
        private static final long serialVersionUID = 6375480347509749765L;
        public ActionDeleteRecord() {
            super(Messages.getString("MainWindow.147"), MainWindow.createResoruceIcon("16x16/remove.png"));   //$NON-NLS-1$ //$NON-NLS-2$
            putValue(SHORT_DESCRIPTION, Messages.getString("MainWindow.149"));  //$NON-NLS-1$
            putValue(MNEMONIC_KEY, Messages.getMnemonic("MainWindow.147.Mnemonic")); //$NON-NLS-1$
        }
        public void actionPerformed(ActionEvent e) {
            int selectedColumn = bbtTable.getSelectedColumn();

            if(selectedColumn < 0) return;

            PCalendar.engine.removeDateRecord(datesList[selectedColumn]);

            //changing the records might impact the colors
            refreshColors();		
            bbtTable.repaint();
        }
    }	

    private void refreshColors() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(bbtStartDate);

        for(int i=0; i<dateLabels.length; i++) {
            PCalDayInfo dayInfo = PCalendar.engine.getDayInfo(calendar.getTime());
            Color[] dayColors = PCalendar.mainWindow.getDayColors(dayInfo);
            daysBGColors[i] = dayColors[0];
            daysFGColors[i] = dayColors[1];
            isOvulationFlag[i] = (dayInfo == null)?false:dayInfo.ovulation;
            calendar.add(Calendar.DATE, 1);
        }
    }

    /**
     * @return the bbtStartDate
     */
    public Date getBbtStartDate() {
        return bbtStartDate;
    }
}