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

package net.sf.linuxorg.pcal.common.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * This class is a factory for the vertically rendered text.
 * Copy-pasted from the Darryl Burke example. 
 * @author Mar'yan Rachynskyy
 */
public class VerticalCaption {
    
    static public Icon getVerticalCaption (JComponent component, String caption, boolean clockwise) {
        Font f = component.getFont ();
        // we need to render everything in plain font
        f = f.deriveFont(Font.PLAIN);
        FontMetrics fm = component.getFontMetrics (f);
        int captionHeight = fm.getHeight ();
        int captionWidth = fm.stringWidth (caption);
        BufferedImage bi = new BufferedImage (captionHeight + 4,
                captionWidth + 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics ();
        
        g.setColor (new Color (0, 0, 0, 0)); // transparent
        g.fillRect (0, 0, bi.getWidth (), bi.getHeight ());
        
        g.setColor (component.getForeground ());
        g.setFont (f);
        g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        if (clockwise) {
            g.rotate (Math.PI / 2);
        } else {
            g.rotate (- Math.PI / 2);
            g.translate (-bi.getHeight (), bi.getWidth ());
        }
        g.drawString (caption, 2, -6);
        
        Icon icon = new ImageIcon (bi);
        return icon;
    }
}
